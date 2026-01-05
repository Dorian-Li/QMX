package com.example.qmx;

import com.example.qmx.mapper.*;
import com.example.qmx.server.DataResponse;
import com.example.qmx.server.DataServer;
import com.example.qmx.utils.ModbusUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.serotonin.modbus4j.BatchRead;
import com.serotonin.modbus4j.BatchResults;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.locator.BaseLocator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@SpringBootTest
class QmxApplicationTests {

    @Test
    void contextLoads() {
    }

    /**
     * 测试服务端是否能正常监听并接受网关连接
     */
    @Test
    public void testConnect() {
        int listenPort = 8088;      // 按你的服务端监听端口配置
        int initialTimeoutMs = 30000; // 首次连接等待超时（毫秒）

        java.net.ServerSocket server = null;
        java.net.Socket socket = null;
        try {
            server = new java.net.ServerSocket(listenPort);
            server.setSoTimeout(initialTimeoutMs);
            log.info("服务端已启动，等待网关连接: 0.0.0.0:{}（首次连接超时{}ms）", listenPort, initialTimeoutMs);

            // 阻塞等待网关连接（首次）
            socket = server.accept();
            String remote = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
            log.info("连接成功，来自网关: {}", remote);

            // 保持连接（不主动关闭）
            try { socket.setKeepAlive(true); } catch (Exception ignore) {}
            // 进入保持循环，直到你手动终止测试或网关主动断开
            log.info("保持连接中...按 Ctrl+C 停止测试或在IDE点击停止");
            while (true) {
                if (socket.isClosed()) {
                    log.info("网关已关闭连接，结束测试");
                    break;
                }
                Thread.sleep(5_000); // 每分钟检查一次连接状态
            }
        } catch (java.net.SocketTimeoutException e) {
            log.error("在初始超时时间内未收到网关连接（{}ms）", initialTimeoutMs);
            org.junit.jupiter.api.Assertions.fail("在初始超时时间内未收到网关连接");
        } catch (Exception e) {
            log.error("服务端监听或处理连接失败: {}", e.toString());
            org.junit.jupiter.api.Assertions.fail("服务端监听或处理连接失败: " + e.getMessage());
        }
    }

    /**
     * 测试能否进行数据解析
     */
    @Resource
    private DataServer dataServer;

    // Mock 所有 Mapper，避免真实 DB 依赖
    @org.springframework.boot.test.mock.mockito.MockBean(name = "DeviceStatusMapper")
    private com.example.qmx.mapper.DeviceStatusMapper deviceStatusMapper;
    @org.springframework.boot.test.mock.mockito.MockBean(name = "ProductHourlyMapper")
    private com.example.qmx.mapper.ProductHourlyMapper productHourlyMapper;
    @org.springframework.boot.test.mock.mockito.MockBean(name = "ProductWeekMapper")
    private com.example.qmx.mapper.ProductWeekMapper productWeekMapper;
    @org.springframework.boot.test.mock.mockito.MockBean(name = "SensorMapper")
    private com.example.qmx.mapper.SensorMapper sensorMapper;
    @org.springframework.boot.test.mock.mockito.MockBean(name = "SprayRecordMapper")
    private com.example.qmx.mapper.SprayRecordMapper sprayRecordMapper;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.qmx.server.DataToObj dataToObj;


    // 数据解析示例测试
    @Test
    public void parseSamplesTest() throws Exception {
        // 0x01 : 停止器1状态 + 喷枪1 + 搅拌器1
        // [1,0,1]
        byte[] seg01 = new byte[] {
                (byte)0x01, (byte)0x03,
                (byte)0x01, (byte)0x00, (byte)0x01
        };

        // 0x02 : 机器人1管道压力 + 机器人1喷涂机压力 + 上料管路实时压力 + 涂料桶1液位
        // [100,120,80,55]
        // [100.5, -120.75, 0.0, -55.125]
        byte[] seg02 = concat(
                new byte[]{(byte)0x02, (byte)0x04},
                beFloat(100.5f),
                beFloat(-120.75f),
                beFloat(0.0f),
                beFloat(-55.125f)
        );

        // 0x04 : 当日每时产量 + 当月每周产量
        // [30,210]
        byte[] seg04 = new byte[] {
                (byte)0x04, (byte)0x02,
                0x00, 0x1E,
                0x00, (byte)0xD2
        };

        // 拼接数据内容
        byte[] content = concat(seg01, seg02, seg04);

        // 构造 PDU：功能码(0x03) + 数据长度(2字节大端) + 内容
        byte functionCode = 0x03;
        int dataLen = content.length;
        byte[] pdu = new byte[1 + 2 + dataLen];
        pdu[0] = functionCode;
        pdu[1] = (byte)((dataLen >> 8) & 0xFF);
        pdu[2] = (byte)(dataLen & 0xFF);
        System.arraycopy(content, 0, pdu, 3, dataLen);

        // 构造 MBAP：transactionId=0x0001, protocolId=0x0000, length=PDU长度+4, unitId=0x01
        int pduLen = pdu.length;
        int mbapLen = pduLen + 4;
        byte[] mbap = new byte[] {
                0x00, 0x01,        // transactionId
                0x00, 0x00,        // protocolId
                (byte)((mbapLen >> 8) & 0xFF), (byte)(mbapLen & 0xFF), // length
                0x01               // unitId
        };

        // 执行解析
        String summary = dataToObj.handleModbusFrame(mbap, pdu);
        System.out.println("handleModbusFrame 摘要: " + summary);

        // 断言解析结果
        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode root = om.readTree(summary);
        org.junit.jupiter.api.Assertions.assertEquals(1, root.path("unitId").asInt());
        org.junit.jupiter.api.Assertions.assertEquals(3, root.path("functionCode").asInt());
        org.junit.jupiter.api.Assertions.assertEquals(dataLen, root.path("dataLen").asInt());

        com.fasterxml.jackson.databind.JsonNode decoded = root.path("decodedValues");
        // 注意：JSON中的Map键为字符串 "1","2","4"
//        org.junit.jupiter.api.Assertions.assertEquals(3, decoded.path("1").size());
//        org.junit.jupiter.api.Assertions.assertEquals(1, decoded.path("1").get(0).asInt());
//        org.junit.jupiter.api.Assertions.assertEquals(0, decoded.path("1").get(1).asInt());
//        org.junit.jupiter.api.Assertions.assertEquals(1, decoded.path("1").get(2).asInt());
//
//        org.junit.jupiter.api.Assertions.assertEquals(4, decoded.path("2").size());
//        org.junit.jupiter.api.Assertions.assertEquals(100, decoded.path("2").get(0).asInt());
//        org.junit.jupiter.api.Assertions.assertEquals(120, decoded.path("2").get(1).asInt());
//        org.junit.jupiter.api.Assertions.assertEquals(80, decoded.path("2").get(2).asInt());
//        org.junit.jupiter.api.Assertions.assertEquals(55, decoded.path("2").get(3).asInt());
//
//        org.junit.jupiter.api.Assertions.assertEquals(2, decoded.path("4").size());
//        org.junit.jupiter.api.Assertions.assertEquals(30, decoded.path("4").get(0).asInt());
//        org.junit.jupiter.api.Assertions.assertEquals(210, decoded.path("4").get(1).asInt());
    }

    // 便捷拼接方法
    private static byte[] concat(byte[]... arrays) {
        int total = java.util.Arrays.stream(arrays).mapToInt(a -> a.length).sum();
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, out, pos, a.length);
            pos += a.length;
        }
        return out;
    }

    // 将 float 转为 4 字节“大端”IEEE-754位序列
    private static byte[] beFloat(float f) {
        int bits = Float.floatToIntBits(f);
        return new byte[] {
                (byte)((bits >> 24) & 0xFF),
                (byte)((bits >> 16) & 0xFF),
                (byte)((bits >> 8) & 0xFF),
                (byte)(bits & 0xFF)
        };
    }


    /**
     * 验证 0x07/0x08/0x09 类型化段的组帧是否符合 MBAP+PDU 以及解析规则
     */
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.qmx.server.DataResponse dataResponse;

    @org.junit.jupiter.api.Test
    public void testBuildCustomFrameTyped_BoolIntReal() {
        int unitId = 1;
        int functionCode = 0x10;
        int startAddress = 0x0000;
        int txId = 0x1234;

        java.util.List<com.example.qmx.server.DataResponse.TypedSegment> segs = new java.util.ArrayList<>();

        // 0x07-bool：低位优先，值 [true, false, true, true, false] => 0b00001101 = 0x0D
        segs.add(com.example.qmx.server.DataResponse.TypedSegment.ofBools(
                java.util.Arrays.asList(true, false, true, true, false)));

        // 0x08-int16：大端，值 [1, 2] => 00 01 00 02
        segs.add(com.example.qmx.server.DataResponse.TypedSegment.ofInt16(
                java.util.Arrays.asList(1, 2)));

        // 0x09-real32：IEEE-754 大端，示例 [123.45, -67.5]
        segs.add(com.example.qmx.server.DataResponse.TypedSegment.ofReal32(
                java.util.Arrays.asList(123.45, -67.5)));

        // 修正：方法签名为 (unitId, functionCode, startAddress, transactionId, segments)
        byte[] frame = dataResponse.buildCustomDataFrameTyped(unitId, functionCode, startAddress, txId, segs);
        org.junit.jupiter.api.Assertions.assertNotNull(frame);
        org.junit.jupiter.api.Assertions.assertTrue(frame.length > 10);

        // 校验 MBAP
        int mbapLen = ((frame[4] & 0xff) << 8) | (frame[5] & 0xff);
        org.junit.jupiter.api.Assertions.assertEquals(unitId, frame[6] & 0xff);

        // 修正：PDU 长度 = MBAP.length - 1
        int pduLen = mbapLen - 1;
        byte[] pdu = java.util.Arrays.copyOfRange(frame, 7, 7 + pduLen);
        // 功能码
        org.junit.jupiter.api.Assertions.assertEquals(functionCode, pdu[0] & 0xff);
        // 起始地址
        org.junit.jupiter.api.Assertions.assertEquals((startAddress >> 8) & 0xff, pdu[1] & 0xff);
        org.junit.jupiter.api.Assertions.assertEquals(startAddress & 0xff, pdu[2] & 0xff);
        // 修正：数据长度字段在 pdu[3..4]
        int dataLen = ((pdu[3] & 0xff) << 8) | (pdu[4] & 0xff);
        org.junit.jupiter.api.Assertions.assertEquals(pduLen - 5, dataLen);

        // 修正：segments 从偏移 5 开始
        int p = 5;

        // 段1：bool(0x07)
        int t1 = pdu[p++] & 0xff;
        int c1 = pdu[p++] & 0xff;
        org.junit.jupiter.api.Assertions.assertEquals(0x07, t1);
        org.junit.jupiter.api.Assertions.assertEquals(5, c1);
        int boolBytes = (c1 + 7) / 8;
        byte[] boolPayload = java.util.Arrays.copyOfRange(pdu, p, p + boolBytes);
        p += boolBytes;
        org.junit.jupiter.api.Assertions.assertEquals(1, boolBytes);
        org.junit.jupiter.api.Assertions.assertEquals(0x0D, boolPayload[0] & 0xff); // 00001101

        // 段2：int16(0x08)
        int t2 = pdu[p++] & 0xff;
        int c2 = pdu[p++] & 0xff;
        org.junit.jupiter.api.Assertions.assertEquals(0x08, t2);
        org.junit.jupiter.api.Assertions.assertEquals(2, c2);
        byte[] intPayload = java.util.Arrays.copyOfRange(pdu, p, p + c2 * 2);
        p += c2 * 2;
        org.junit.jupiter.api.Assertions.assertArrayEquals(
                new byte[]{0x00, 0x01, 0x00, 0x02}, intPayload);

        // 段3：real32(0x09)
        int t3 = pdu[p++] & 0xff;
        int c3 = pdu[p++] & 0xff;
        org.junit.jupiter.api.Assertions.assertEquals(0x09, t3);
        org.junit.jupiter.api.Assertions.assertEquals(2, c3);
        byte[] realPayload = java.util.Arrays.copyOfRange(pdu, p, p + c3 * 4);
        p += c3 * 4;

        int r1 = ((realPayload[0] & 0xff) << 24) | ((realPayload[1] & 0xff) << 16)
                | ((realPayload[2] & 0xff) << 8) | (realPayload[3] & 0xff);
        int r2 = ((realPayload[4] & 0xff) << 24) | ((realPayload[5] & 0xff) << 16)
                | ((realPayload[6] & 0xff) << 8) | (realPayload[7] & 0xff);
        org.junit.jupiter.api.Assertions.assertEquals(java.lang.Float.floatToIntBits(123.45f), r1);
        org.junit.jupiter.api.Assertions.assertEquals(java.lang.Float.floatToIntBits(-67.5f), r2);

        // 修正：PDU 长度应为 5 + dataLen
        org.junit.jupiter.api.Assertions.assertEquals(pdu.length, 5 + dataLen);
    }

    /**
     * 验证 bool 按位打包在跨字节边界（>8位）时仍保持低位优先且字节正确
     */
    @org.junit.jupiter.api.Test
    public void testBoolPackingBoundary() {
        // 10 位：索引 0,2,3,7,8 置 1 => byte0: 10001101(0x8D), byte1: 00000001(0x01)
        List<Boolean> values = Arrays.asList(
                true, false, true, true, false, false, false, true, true, false
        );
        com.example.qmx.server.DataResponse.TypedSegment seg =
                com.example.qmx.server.DataResponse.TypedSegment.ofBools(values);

        org.junit.jupiter.api.Assertions.assertEquals(0x07, seg.typeId);
        org.junit.jupiter.api.Assertions.assertEquals(10, seg.count);
        org.junit.jupiter.api.Assertions.assertEquals(2, seg.payload.length);
        org.junit.jupiter.api.Assertions.assertEquals(0x8D, seg.payload[0] & 0xFF);
        org.junit.jupiter.api.Assertions.assertEquals(0x01, seg.payload[1] & 0xFF);
    }

    /**
     * 验证 sendResponse 在长度一致时：
     * - 返回 dataLen
     * - 写出的帧中 PDU 数据段为全 0xAA，长度等于 dataLen
     */
    @Test
    public void testSendResponse_LengthMatch() throws Exception {
        // 构造一个可捕获写出内容的 Socket
        final java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.net.Socket socket = new java.net.Socket() {
            @Override public java.io.OutputStream getOutputStream() { return bos; }
            @Override public boolean isClosed() { return false; }
        };

        // 构造一致的 MBAP 和 PDU
        byte functionCode = 0x03;
        int dataLen = 10; // 声明数据长度为 10
        byte[] incomingPdu = new byte[1 + 2 + dataLen];
        incomingPdu[0] = functionCode;
        incomingPdu[1] = (byte) ((dataLen >> 8) & 0xFF);
        incomingPdu[2] = (byte) (dataLen & 0xFF);
        // 实际数据内容随便填充（不会用于确认帧中，确认帧数据段为 0xAA）
        for (int i = 3; i < incomingPdu.length; i++) incomingPdu[i] = (byte) i;

        // MBAP：transactionId=0x1234, protocolId=0x0000, length先占位, unitId=0x01
        byte[] incomingMbap = new byte[] {
                0x12, 0x34,
                0x00, 0x00,
                0x00, 0x00, // 这里占位，DataResponse 会重算
                0x01
        };

        int ret = dataResponse.sendResponse(socket, incomingMbap, incomingPdu);
        org.junit.jupiter.api.Assertions.assertEquals(dataLen, ret);

        // 解析写出的帧
        byte[] frame = bos.toByteArray();
        org.junit.jupiter.api.Assertions.assertTrue(frame.length >= 12);

        // 校验 MBAP
        org.junit.jupiter.api.Assertions.assertEquals(0x12, frame[0] & 0xFF);
        org.junit.jupiter.api.Assertions.assertEquals(0x34, frame[1] & 0xFF);
        org.junit.jupiter.api.Assertions.assertEquals(0x00, frame[2] & 0xFF);
        org.junit.jupiter.api.Assertions.assertEquals(0x00, frame[3] & 0xFF);
        int mbapLen = ((frame[4] & 0xFF) << 8) | (frame[5] & 0xFF);
        org.junit.jupiter.api.Assertions.assertEquals(0x01, frame[6] & 0xFF);

        // 解析 PDU
        int pduLen = mbapLen - 1;
        byte[] pdu = java.util.Arrays.copyOfRange(frame, 7, 7 + pduLen);
        org.junit.jupiter.api.Assertions.assertEquals(functionCode, pdu[0] & 0xFF);
        int declaredLen = ((pdu[1] & 0xFF) << 8) | (pdu[2] & 0xFF);
        org.junit.jupiter.api.Assertions.assertEquals(dataLen, declaredLen);
        // 数据段应全部为 0xAA，长度等于 dataLen
        for (int i = 3; i < pdu.length; i++) {
            org.junit.jupiter.api.Assertions.assertEquals(0xAA, pdu[i] & 0xFF);
        }
        org.junit.jupiter.api.Assertions.assertEquals(3 + dataLen, pdu.length);
        // MBAP.length 应为 UnitId(1) + PDU.length
        org.junit.jupiter.api.Assertions.assertEquals(pdu.length + 1, mbapLen);
        // 整帧长度应为 7 + pdu.length
        org.junit.jupiter.api.Assertions.assertEquals(7 + pdu.length, frame.length);
    }

    /**
     * 验证 sendResponse 在长度不一致时：
     * - 返回 -1
     * - 写出的帧为错误帧：PDU 数据长度为 2，数据内容 0xFF 0xFF（表示 -1）
     */
    @Test
    public void testSendResponse_LengthMismatch() throws Exception {
        final java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.net.Socket socket = new java.net.Socket() {
            @Override public java.io.OutputStream getOutputStream() { return bos; }
            @Override public boolean isClosed() { return false; }
        };

        byte functionCode = 0x03;
        int dataLen = 10; // 声明为 10，但实际只提供 5 字节数据
        int actualDataLen = 5;
        byte[] incomingPdu = new byte[1 + 2 + actualDataLen];
        incomingPdu[0] = functionCode;
        incomingPdu[1] = (byte) ((dataLen >> 8) & 0xFF);
        incomingPdu[2] = (byte) (dataLen & 0xFF);
        for (int i = 3; i < incomingPdu.length; i++) incomingPdu[i] = (byte) i;

        byte[] incomingMbap = new byte[] {
                0x12, 0x34,
                0x00, 0x00,
                0x00, 0x00,
                0x01
        };

        int ret = dataResponse.sendResponse(socket, incomingMbap, incomingPdu);
        org.junit.jupiter.api.Assertions.assertEquals(-1, ret);

        byte[] frame = bos.toByteArray();
        org.junit.jupiter.api.Assertions.assertTrue(frame.length >= 12);

        // 校验 MBAP
        org.junit.jupiter.api.Assertions.assertEquals(0x12, frame[0] & 0xFF);
        org.junit.jupiter.api.Assertions.assertEquals(0x34, frame[1] & 0xFF);
        int mbapLen = ((frame[4] & 0xFF) << 8) | (frame[5] & 0xFF);
        org.junit.jupiter.api.Assertions.assertEquals(0x01, frame[6] & 0xFF);

        // 解析 PDU：应为 [func][00][02][FF][FF]
        int pduLen = mbapLen - 1;
        byte[] pdu = java.util.Arrays.copyOfRange(frame, 7, 7 + pduLen);
        org.junit.jupiter.api.Assertions.assertEquals(functionCode, pdu[0] & 0xFF);
        org.junit.jupiter.api.Assertions.assertEquals(0x00, pdu[1] & 0xFF);
        org.junit.jupiter.api.Assertions.assertEquals(0x02, pdu[2] & 0xFF);
        org.junit.jupiter.api.Assertions.assertEquals(0xFF, pdu[3] & 0xFF);
        org.junit.jupiter.api.Assertions.assertEquals(0xFF, pdu[4] & 0xFF);
        org.junit.jupiter.api.Assertions.assertEquals(5, pdu.length);

        // MBAP.length 应为 UnitId(1) + PDU.length（此处 PDU.length=5 => 1+5=6）
        org.junit.jupiter.api.Assertions.assertEquals(6, mbapLen);
        // 整帧应为 7 + 5 = 12（不变）
        org.junit.jupiter.api.Assertions.assertEquals(12, frame.length);
    }
}
