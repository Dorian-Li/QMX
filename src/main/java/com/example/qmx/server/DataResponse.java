package com.example.qmx.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

@Service
@Resource
public class DataResponse {

    /**
     * 响应数据到网关，用于确认数据包完整
     */
    private static final Logger log = LoggerFactory.getLogger(DataResponse.class);
    
    public byte[] buildResponseFrame(byte[] incomingMbap, byte[] incomingPdu) {
        if (incomingMbap == null || incomingMbap.length != 7) {
            throw new IllegalArgumentException("MBAP 头不能为空且长度必须为 7 字节");
        }
        if (incomingPdu == null || incomingPdu.length < 3) {
            throw new IllegalArgumentException("PDU 不能为空且长度至少为 3 字节（功能码+数据长度2字节）");
        }

        // 解析 PDU
        byte functionCode = incomingPdu[0];
        int dataLen = ((incomingPdu[1] & 0xFF) << 8) | (incomingPdu[2] & 0xFF);
        int expectedPduLen = 1 + 2 + dataLen;
        if (incomingPdu.length != expectedPduLen) {
            log.warn("接收的 PDU 长度({}) 与声明的数据长度({})不一致，按声明长度构造响应", incomingPdu.length, expectedPduLen);
        }

        // 构造响应 PDU：功能码 + 数据长度(原样) + 数据内容(全部 0xAA)
        byte[] respPdu = new byte[expectedPduLen];
        respPdu[0] = functionCode;
        respPdu[1] = incomingPdu[1];
        respPdu[2] = incomingPdu[2];
        Arrays.fill(respPdu, 3, respPdu.length, (byte) 0xAA);

        // 构造响应 MBAP：沿用 transactionId/protocolId/unitId，length=respPdu.length+4
        byte[] respMbap = new byte[7];
        // transactionId
        respMbap[0] = incomingMbap[0];
        respMbap[1] = incomingMbap[1];
        // protocolId
        respMbap[2] = incomingMbap[2];
        respMbap[3] = incomingMbap[3];
        // length (PDU长度+4)
        int mbapLen = respPdu.length + 4;
        respMbap[4] = (byte) ((mbapLen >> 8) & 0xFF);
        respMbap[5] = (byte) (mbapLen & 0xFF);
        // unitId
        respMbap[6] = incomingMbap[6];

        // 拼装完整帧
        byte[] frame = new byte[respMbap.length + respPdu.length];
        System.arraycopy(respMbap, 0, frame, 0, respMbap.length);
        System.arraycopy(respPdu, 0, frame, respMbap.length, respPdu.length);
        return frame;
    }

    /**
     * 发送响应帧到网关（使用 Socket）
     */
    public void sendResponse(Socket socket, byte[] incomingMbap, byte[] incomingPdu) throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException("Socket 不可用或已关闭，无法发送响应");
        }
        byte[] frame = buildResponseFrame(incomingMbap, incomingPdu);
        OutputStream os = socket.getOutputStream();
        os.write(frame);
        os.flush();
        log.info("已向网关发送确认帧，长度={} 字节", frame.length);
    }
}
