package com.example.qmx.server;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.slf4j.Logger;

import javax.annotation.Resource;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;


@Service
@Resource
public class DataServer {

    Logger logger = LoggerFactory.getLogger(this.getClass());

    // 通过配置读取 Modbus TCP 参数
    @Value("${modbus.host:127.0.0.1}")
    private String modbusHost;

    @Value("${modbus.port:502}")
    private int modbusPort;

    @Value("${modbus.slave-id:1}")
    private int modbusSlaveId;

    // 读取起始地址与数量（可在配置中调整）
    @Value("${modbus.start-address:0}")
    private int modbusStartAddress;

    @Value("${modbus.quantity:10}")
    private int modbusQuantity;

    @Autowired
    private DataToObj dataToObj;

    // 固定事务ID
    private static final int FIXED_TX_ID = 0x0001;

    // 服务端监听端口与首次连接等待超时（毫秒）
    @Value("${modbus.server.listen-port:8087}")
    private int listenPort;

    @Value("${modbus.server.initial-timeout-ms:30000}")
    private int initialTimeoutMs;

    // 保存与网关的当前连接（最近一次接入）
    private volatile Socket gatewaySocket;
    private volatile OutputStream gatewayOut;

    @Autowired
    private DataResponse dataResponse;

    

    /**
     * 采集数据：作为服务端接收网关（客户端）发来的 Modbus TCP 帧
     * 成功连接后打印“与网关连接成功”，读取一帧并交由 DataToObj 解析与入库
     * 返回解析后的简要 JSON（便于接口联调）
     */
    public String fetchData() {
        // 启动服务端监听，等待网关连接
        try (ServerSocket server = new ServerSocket(listenPort)) {
            server.setSoTimeout(initialTimeoutMs);
            logger.info("服务端已启动，等待网关连接: 0.0.0.0:{}（首次连接超时{}ms）", listenPort, initialTimeoutMs);

            // 阻塞等待连接（保持 socket 供后续下发配置使用）
            Socket socket = server.accept();
            this.gatewaySocket = socket;
            try { socket.setKeepAlive(true); } catch (Exception ignore) {}
            this.gatewayOut = socket.getOutputStream();

            String remote = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
            logger.info("与网关连接成功: {}", remote);

            // 读取一帧 Modbus TCP 数据：先读 MBAP 7 字节，再读 PDU（长度字段-1）
            InputStream in = socket.getInputStream();

            byte[] mbap = readFully(in, 7);
            if (mbap == null) {
                throw new RuntimeException("读取 MBAP 头失败（连接关闭或超时）");
            }

            int transactionId = ((mbap[0] & 0xFF) << 8) | (mbap[1] & 0xFF);
            int protocolId   = ((mbap[2] & 0xFF) << 8) | (mbap[3] & 0xFF);
            int length       = ((mbap[4] & 0xFF) << 8) | (mbap[5] & 0xFF);
            byte unitId      = mbap[6]; // UnitId 在 MBAP[6]

            // 读取 PDU 长度
            int pduLength = length - 1;
            if (pduLength <= 0) {
                throw new RuntimeException("非法长度字段（length<=1）");
            }

            byte[] pdu = readFully(in, pduLength);
            if (pdu == null || pdu.length != pduLength) {
                throw new RuntimeException("读取 PDU 失败（长度不匹配或连接关闭）");
            }

            // 交给 DataToObj 做解析与入库，拿到解析摘要 JSON
            String summary = dataToObj.handleModbusFrame(mbap, pdu);

            // 注意：不在此处关闭 socket，保持连接以便后续向网关下发配置
            return summary;
        } catch (Exception e) {
            logger.error("服务端接收或解析数据失败: {}", e.toString());
            throw new RuntimeException("failed to receive/parse modbus tcp frame: " + e.getMessage(), e);
        }
    }

    /**
     * 是否与网关保持连接
     */
    public boolean isGatewayConnected() {
        return gatewaySocket != null && !gatewaySocket.isClosed() && gatewayOut != null;
    }

    /**
     * 通过当前网关连接写多个保持寄存器（功能码 0x10）
     */
    public synchronized void sendWriteMultipleRegisters(int unitId, int startAddress, int[] values) throws java.io.IOException {
        if (!isGatewayConnected()) {
            throw new java.io.IOException("网关未连接或连接不可用，无法下发配置");
        }
        // 使用固定事务ID
        int transactionId = FIXED_TX_ID;
        byte[] frame = dataResponse.buildWriteMultipleRegistersFrame(transactionId, unitId, startAddress, values);
        gatewayOut.write(frame);
        gatewayOut.flush();
        logger.info("已向网关下发配置（0x10 写保持寄存器）：unitId={}, startAddress={}, quantity={}, frameLen={}",
                unitId, startAddress, values != null ? values.length : 0, frame.length);
    }

    // 精确读取指定长度的字节，若读取失败返回 null
    private byte[] readFully(InputStream in, int len) throws Exception {
        byte[] buf = new byte[len];
        int off = 0;
        while (off < len) {
            int r = in.read(buf, off, len - off);
            if (r < 0) {
                return null; // 流结束
            }
            off += r;
        }
        return buf;
    }

    // 一次性发送
    // 修改：增加 startAddress，从前端读取传递到 DataResponse
    public boolean sendTypedSegments(int unitId, int functionCode, int startAddress, java.util.List<DataResponse.TypedSegment> segments) {
        try {
            if (!isGatewayConnected()) {
                logger.error("网关未连接，无法发送 Typed 段自定义帧");
                return false;
            }
            // 使用固定事务ID
            int txId = FIXED_TX_ID;
            dataResponse.sendCustomDataFrameTyped(gatewaySocket, unitId, functionCode, startAddress, txId, segments);
            logger.info("已发送 Typed 段帧: txId={}, unitId={}, func=0x{}, startAddr=0x{}", txId, unitId, Integer.toHexString(functionCode), Integer.toHexString(startAddress));
            return true;
        } catch (Exception e) {
            logger.error("发送 Typed 段帧失败: {}", e.getMessage());
            return false;
        }
    }

    // 新增：发送“参数配置下发 V2（无类型标识）”帧
    public boolean sendConfigItemsV2(int unitId, int functionCode, java.util.List<DataResponse.ConfigItem> items) {
        try {
            if (!isGatewayConnected()) {
                logger.error("网关未连接，无法发送配置下发(V2)");
                return false;
            }
            int txId = FIXED_TX_ID;
            // 直接使用 DataResponse 发送 V2 帧
            dataResponse.sendConfigDataFrameV2(gatewaySocket, unitId, functionCode, txId, items);
            logger.info("已发送配置下发帧(V2)：txId={}, unitId={}, func=0x{}, itemCount={}",
                    txId, unitId, Integer.toHexString(functionCode), (items == null ? 0 : items.size()));
            return true;
        } catch (Exception e) {
            logger.error("发送配置下发帧(V2)失败: {}", e.getMessage());
            return false;
        }
    }
}

