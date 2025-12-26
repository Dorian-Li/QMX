package com.example.qmx;

import com.example.qmx.mapper.*;
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
        byte[] seg02 = new byte[] {
                (byte)0x02, (byte)0x04,
                0x00, 0x00, 0x00, 0x64,
                0x00, 0x00, 0x00, 0x78,
                0x00, 0x00, 0x00, 0x50,
                0x00, 0x00, 0x00, 0x37
        };

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
        org.junit.jupiter.api.Assertions.assertEquals(3, decoded.path("1").size());
        org.junit.jupiter.api.Assertions.assertEquals(1, decoded.path("1").get(0).asInt());
        org.junit.jupiter.api.Assertions.assertEquals(0, decoded.path("1").get(1).asInt());
        org.junit.jupiter.api.Assertions.assertEquals(1, decoded.path("1").get(2).asInt());

        org.junit.jupiter.api.Assertions.assertEquals(4, decoded.path("2").size());
        org.junit.jupiter.api.Assertions.assertEquals(100, decoded.path("2").get(0).asInt());
        org.junit.jupiter.api.Assertions.assertEquals(120, decoded.path("2").get(1).asInt());
        org.junit.jupiter.api.Assertions.assertEquals(80, decoded.path("2").get(2).asInt());
        org.junit.jupiter.api.Assertions.assertEquals(55, decoded.path("2").get(3).asInt());

        org.junit.jupiter.api.Assertions.assertEquals(2, decoded.path("4").size());
        org.junit.jupiter.api.Assertions.assertEquals(30, decoded.path("4").get(0).asInt());
        org.junit.jupiter.api.Assertions.assertEquals(210, decoded.path("4").get(1).asInt());
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


}
