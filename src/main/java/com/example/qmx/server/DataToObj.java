package com.example.qmx.server;

import com.example.qmx.domain.*;
import com.example.qmx.mapper.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 解析从外部modbus tcp获取的帧数据，并将数据映射到不同的实体对象（如喷涂系统、喷涂房、炭块锁定等）
 然后通过MyBatis的Mapper接口将这些数据存储到数据库
 */

@Service
public class DataToObj {

    @Autowired
    private DeviceStatusMapper deviceStatusMapper;

    @Autowired
    private SensorMapper sensorMapper;

    @Autowired
    private SprayRecordMapper sprayRecordMapper;

    @Autowired
    private ProductHourlyMapper productHourlyMapper;

    @Autowired
    private ProductWeekMapper productWeekMapper;

    @Autowired
    private DataResponse dataResponse; // 新增：注入响应帧构造/发送服务
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private org.springframework.web.client.RestTemplate restTemplate;

    @org.springframework.beans.factory.annotation.Value("${zljc.api:}")
    private String zljcApi;

    @org.springframework.beans.factory.annotation.Value("${alarm.notify.url:}")
    private String alarmNotifyUrl;


    // 报警设备名列表
    private static final List<String> ALARM_DEVICE_NAMES = Arrays.asList(
            "停止器1", "停止器2",
            "锁定结构1", "锁定机构2",
            "机器人1地轨", "机器人2地轨",
            "机器人1", "机器人2",
            "喷涂机1", "喷涂机2",
            "压力传感器1", "压力传感器2", "压力传感器3", "压力传感器4", "压力传感器5",
            "搅拌器1", "搅拌器2"
    );

    /**
     * 按协议解码 Modbus TCP 报文（MBAP + PDU），并进行入库路由
     *
     * 协议要点：
     * 1) MBAP（7B）：[事务ID 2B][协议ID 2B=0x0000][长度 2B=N+4][单元标识符(网关号) 1B]
     * 2) PDU：      [功能码 1B=0x03][数据位长度 2B=N][数据内容 N 字节]
     * 3) 数据内容：由多个数据段拼接，每段格式为：
     *               [数据类型标识 1B][数据数量 1B][具体数据 ...]
     *    单个“具体数据”的长度由类型标识决定（大端）：
     *      0x00 报警信息     单长=1bit（按位解析，按需取出 count 个 bit）
     *      0x01 设备状态信息 单长=8bit（1字节）
     *      0x02 传感器参数   单长=32bit（4字节）
     *      0x03 喷涂记录     单长=16bit（2字节）
     *      0x04 喷涂产量     单长=16bit（2字节）
     *      0x05 控制参数     单长=不定（按实际协议补充）
     *      0x06 运动参数     单长=32bit（4字节）
     */
    public String handleModbusFrame(byte[] mbap, byte[] pdu) {
        // 基本校验
        if (mbap == null || mbap.length != 7 || pdu == null || pdu.length < 3) {
            throw new IllegalArgumentException("非法帧：MBAP 或 PDU 长度不正确");
        }

        int transactionId = ((mbap[0] & 0xFF) << 8) | (mbap[1] & 0xFF);
        int protocolId    = ((mbap[2] & 0xFF) << 8) | (mbap[3] & 0xFF);
        int mbapLength    = ((mbap[4] & 0xFF) << 8) | (mbap[5] & 0xFF); // N + 4
        int unitId        = (mbap[6] & 0xFF);

        if (protocolId != 0x0000) {
            throw new IllegalArgumentException("协议标识不是0x0000，非Modbus TCP帧");
        }

        int functionCode = pdu[0] & 0xFF;
        int dataLen      = ((pdu[1] & 0xFF) << 8) | (pdu[2] & 0xFF);
        if (pdu.length < 3 + dataLen) {
            throw new IllegalArgumentException("PDU长度与数据长度不匹配");
        }

        // 解析数据内容 Map<typeId, List<Long>>
        Map<Integer, List<Long>> decoded = parseDataSegments(pdu, 3, dataLen);

        // 报警处理
        try {
            handleAlarmAndNotify(decoded, unitId);
        } catch (Exception e) {
            System.err.println("报警处理/上报异常: " + e.getMessage());
        }

        try {
            routeToDb(decoded); 
        } catch (Exception e) {
            System.err.println("解析/入库过程发生异常: " + e.getMessage());
        }

        try {
            processZljcData(decoded);
        } catch (Exception e) {
            System.err.println("质量检测处理异常: " + e.getMessage());
        }

        // 返回解析摘要 JSON（便于接口验证）
        Map<String, Object> summary = new HashMap<>();

        summary.put("transactionId", transactionId);
        summary.put("protocolId", protocolId);
        summary.put("mbapLength", mbapLength);
        summary.put("unitId", unitId);
        summary.put("functionCode", functionCode);
        summary.put("dataLen", dataLen);
        summary.put("decodedTypes", decoded.keySet());
        summary.put("decodedValues", decoded);

        try {
            return objectMapper.writeValueAsString(summary);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return "{\"transactionId\":" + transactionId + ",\"unitId\":" + unitId + ",\"functionCode\":" + functionCode + "}";
        }
    }


    // 数据解析
    private Map<Integer, List<Long>> parseDataSegments(byte[] pdu, int offset, int dataLen) {
        final int end = offset + dataLen;
        Map<Integer, List<Long>> out = new HashMap<>();

        while (offset < end) {
            if (offset + 2 > end) break; // [typeId][count]
            int typeId = pdu[offset++] & 0xFF;
            int count  = pdu[offset++] & 0xFF;

            List<Long> values = out.computeIfAbsent(typeId, k -> new ArrayList<>());

            switch (typeId) {
                case 0x00: { // 报警信息
                    int bitBytes = (count + 7) / 8;
                    if (offset + bitBytes > end) { offset = end; break; }
                    for (int i = 0; i < count; i++) {
                        int bIndex = i / 8;
                        int bitIndex = i % 8; // 低位开始
                        int bit = (pdu[offset + bIndex] >> bitIndex) & 0x01;
                        values.add((long) bit);
                    }
                    offset += bitBytes;
                    break;
                }
                case 0x01: { // 设备状态
                    if (offset + count > end) { offset = end; break; }
                    for (int i = 0; i < count; i++) {
                        values.add((long) (pdu[offset + i] & 0xFF));
                    }
                    offset += count;
                    break;
                }
                case 0x02: { // 传感器
                    int need = count * 4;
                    if (offset + need > end) { offset = end; break; }
                    for (int i = 0; i < count; i++) {
                        int bits = ((pdu[offset] & 0xFF) << 24)
                                 | ((pdu[offset + 1] & 0xFF) << 16)
                                 | ((pdu[offset + 2] & 0xFF) << 8)
                                 | (pdu[offset + 3] & 0xFF);
                        values.add((long) bits); // 保存浮点的原始位模式
                        offset += 4;
                    }
                    break;
                }
                case 0x03: { // 喷涂记录
                    int need = count * 2;
                    if (offset + need > end) { offset = end; break; }
                    for (int i = 0; i < count; i++) {
                        int v = ((pdu[offset] & 0xFF) << 8) | (pdu[offset + 1] & 0xFF);
                        values.add((long) v);
                        offset += 2;
                    }
                    break;
                }
                case 0x04: { // 喷涂产量
                    int need = count * 2;
                    if (offset + need > end) { offset = end; break; }
                    for (int i = 0; i < count; i++) {
                        int v = ((pdu[offset] & 0xFF) << 8) | (pdu[offset + 1] & 0xFF);
                        values.add((long) v);
                        offset += 2;
                    }
                    break;
                }
                // case 0x05: { // 控制参数
                //     // 如果控制参数采用 [count][itemSize] 或固定结构，请把格式告知我，我再完善解析逻辑。
                //     offset = end;
                //     break;
                // }
                // case 0x06: { // 运动参数
                //     int need = count * 4;
                //     if (offset + need > end) { offset = end; break; }
                //     for (int i = 0; i < count; i++) {
                //         long v = ((long)(pdu[offset] & 0xFF) << 24)
                //                | ((long)(pdu[offset + 1] & 0xFF) << 16)
                //                | ((long)(pdu[offset + 2] & 0xFF) << 8)
                //                | ((long)(pdu[offset + 3] & 0xFF));
                //         values.add(v);
                //         offset += 4;
                //     }
                //     break;
                // }
                default: {

                    offset = end;
                    break;
                }
            }
        }
        // 在返回前打印解析后的数据（JSON 格式；失败则使用 toString）
        try {
            System.out.println("parseDataSegments 解析结果: " + objectMapper.writeValueAsString(out));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            System.out.println("parseDataSegments 解析结果: " + out);
        }
        return out;
    }


    private void routeToDb(Map<Integer, List<Long>> decoded) {
        List<Long> deviceStatus = decoded.getOrDefault(0x01, Collections.emptyList());
        List<Long> sensors      = decoded.getOrDefault(0x02, Collections.emptyList());
        List<Long> paintRecord  = decoded.getOrDefault(0x03, Collections.emptyList());
        List<Long> production   = decoded.getOrDefault(0x04, Collections.emptyList());
        
        String[] statusDevNames = new String[]{
                "停止器1", "停止器2", "锁定机构1", "锁定机构2", "喷枪1", "喷枪2", "喷涂机1压力", "喷涂机2压力", "喷涂管路1压力", "喷涂管路2压力",
                "搅拌器1", "搅拌器2", "进料门", "出料门"
        };

        for (int idx = 0; idx < deviceStatus.size(); idx++) {
            int status = deviceStatus.get(idx) > 0 ? 1 : 0;
            String devName = idx < statusDevNames.length ? statusDevNames[idx] : ("设备" + (idx + 1));

            DeviceStatus ds = new DeviceStatus();
            ds.setDevName(devName);
            ds.setStatus(status);
            ds.setTime(new Date());
            int n = deviceStatusMapper.insert(ds);
            if (n > 0) {
                System.out.println("device_status 插入成功: " + devName + " status=" + status);
            } else {
                System.out.println("device_status 插入失败: " + devName);
            }
        }

        // 0x02 传感器
        String[] sensorDevNames = new String[]{
                "涂料桶1液位", "涂料桶2液位", "喷涂管路1压力", "喷涂管路2压力", "喷涂机1压力", "喷涂机2压力", "上料管路实时压力"
        };
        for (int idx = 0; idx < sensors.size(); idx++) {
            long rawBits = sensors.get(idx);
            float f = Float.intBitsToFloat((int) rawBits); // 按 REAL32 还原
            float value = f; // 与实体的 Float 类型兼容

            String devName = idx < sensorDevNames.length ? sensorDevNames[idx] : ("传感器" + (idx + 1));
        
            Sensor s = new Sensor();
            s.setDevName(devName);
            s.setValue(value);
            s.setTime(new Date());
            int n = sensorMapper.insert(s);
            if (n > 0) {
                System.out.println("sensor 插入成功: " + devName + " value=" + value);
            } else {
                System.out.println("sensor 插入失败: " + devName);
            }
        }

        // 0x03 喷涂情况
        String[] sprayDevNames = new String[]{
                "机器人1进度", "机器人2进度", "相机"
        };
        for (int idx = 0; idx < paintRecord.size(); idx++) {
            double rate = paintRecord.get(idx);
            SprayRecord sr = new SprayRecord();
            sr.setDevName(sprayDevNames[idx]);
            sr.setRate(rate);
            sr.setTime(new Date());
            int n = sprayRecordMapper.insert(sr);
            if (n > 0) {
                System.out.println("spray_record 插入成功: rate=" + rate);
            } else {
                System.out.println("spray_record 插入失败: rate=" + rate);
            }
        }

        // 0x04 产量
        if (production.size() >= 1) {
            int numHourly = production.get(0).intValue();
            ProductHourly ph = new ProductHourly();
            ph.setNumHourly(numHourly);
            ph.setTime(new Date());
            int n = productHourlyMapper.insert(ph);
            if (n > 0) {
                System.out.println("product_hourly 插入成功: numHourly=" + numHourly);
            } else {
                System.out.println("product_hourly 插入失败: numHourly=" + numHourly);
            }
        }
        if (production.size() >= 2) {
            int numWeekly = production.get(1).intValue();
            ProductWeek pw = new ProductWeek();
            pw.setNumWeekly(numWeekly);
            pw.setTime(new Date());
            int n = productWeekMapper.insert(pw);
            if (n > 0) {
                System.out.println("product_week 插入成功: numWeekly=" + numWeekly);
            } else {
                System.out.println("product_week 插入失败: numWeekly=" + numWeekly);
            }
        }
        
    }

    private void fetchZLJCFromApi() {
        String ZLJC_URL = "http://";
        HttpURLConnection conn = null;
        try {
            if (ZLJC_URL == null || ZLJC_URL.isEmpty()) {
                System.err.println("质量检测API地址未配置：zljcApi 为空");
                return;
            }

            java.net.URL url = new java.net.URL(ZLJC_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Accept", "application/json");

            int statusCode = conn.getResponseCode();
            InputStream in = (statusCode >= 200 && statusCode < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }

            if (statusCode != 200) {
                throw new RuntimeException("质量检测API请求失败，HTTP状态码：" + statusCode + "，响应：" + sb.toString());
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(sb.toString());

            // 必须存在 rate，否则抛异常
            JsonNode rateNode = json.get("rate");
            if (rateNode == null || rateNode.isNull() || !rateNode.isNumber()) {
                throw new IllegalStateException("质量检测API未返回有效的 rate 字段");
            }
            double rate = rateNode.asDouble();

            // 可选：设备/阶段信息（若接口未提供则置空）
            String devName = json.path("devName").isMissingNode() ? null : json.get("devName").asText();
            Integer stage  = json.path("stage").isMissingNode() ? null : json.get("stage").asInt();

            // 写入 spray_record 表
            SprayRecord sr = new SprayRecord();
            sr.setDevName(devName);         // creatSQL.sql 中 devName 可为 NULL
            if (stage != null) {
                sr.setStage(stage);         // 如果表无该列，请将 domain 的 stage 标记 exist=false 或移除
            }
            sr.setRate(rate);
            sr.setTime(new Date());

            int n = sprayRecordMapper.insert(sr);
            if (n > 0) {
                System.out.println("spray_record 插入成功: devName=" + devName + ", rate=" + rate + (stage != null ? (", stage=" + stage) : ""));
            } else {
                System.out.println("spray_record 插入失败: devName=" + devName + ", rate=" + rate + (stage != null ? (", stage=" + stage) : ""));
            }
        } catch (Exception e) {
            System.err.println("质量检测API读取异常：" + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void processZljcData(Map<Integer, List<Long>> decoded) {
        fetchZLJCFromApi();
    }

    // 新增：对外提供“构造确认响应帧”的方法（与接收格式相同，数据内容为 0xAA）
    public byte[] buildAckFrame(byte[] mbap, byte[] pdu) {
        return dataResponse.buildResponseFrame(mbap, pdu);
    }

    // 可选：直接发送确认响应帧到网关（若你持有 socket）
    public void sendAckFrame(java.net.Socket socket, byte[] mbap, byte[] pdu) throws java.io.IOException {
        dataResponse.sendResponse(socket, mbap, pdu);
    }

    private void handleAlarmAndNotify(Map<Integer, List<Long>> decoded, int unitId) {
        List<Long> alarms = decoded.get(0x00);
        if (alarms == null || alarms.isEmpty()) {
            return; // 无报警位
        }
    
        // 收集触发的报警项（按位）
        java.util.List<java.util.Map<String, Object>> alarmItems = new java.util.ArrayList<>();
        for (int i = 0; i < alarms.size(); i++) {
            Long bit = alarms.get(i);
            if (bit != null && bit > 0) {
                String device = i < ALARM_DEVICE_NAMES.size()
                        ? ALARM_DEVICE_NAMES.get(i)
                        : ("报警设备" + (i + 1));
                // 控制台打印
                System.out.println("ALARM 触发: " + device + "（index=" + i + "，unitId=" + unitId + "）");
    
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("device", device);
                item.put("index", i);
                item.put("unitId", unitId);
                item.put("timestamp", System.currentTimeMillis());
                alarmItems.add(item);
            }
        }
    
        // 没有任何位被置位，则不做上报
        if (alarmItems.isEmpty()) {
            return;
        }
    
        // 如已配置前端接收地址，则立即上报（POST JSON）
        if (alarmNotifyUrl != null && !alarmNotifyUrl.trim().isEmpty() && restTemplate != null) {
            try {
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                org.springframework.http.HttpEntity<java.util.List<java.util.Map<String, Object>>> entity =
                        new org.springframework.http.HttpEntity<>(alarmItems, headers);
                org.springframework.http.ResponseEntity<String> resp =
                        restTemplate.postForEntity(alarmNotifyUrl, entity, String.class);
                System.out.println("报警上报成功: status=" + (resp != null ? resp.getStatusCodeValue() : "null"));
            } catch (Exception ex) {
                System.err.println("报警上报失败: " + ex.getMessage());
            }
        } else {
            // 未配置上报地址或无 RestTemplate，仅打印
            System.out.println("报警未上报（未配置 alarm.notify.url 或 RestTemplate 不可用），已在控制台打印触发项。");
        }
    }
}
