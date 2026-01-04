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
import java.util.Collections;
import java.util.List;

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

    /**
     * 构造 Modbus TCP 写多个保持寄存器（功能码 0x10）完整帧
     */
    public byte[] buildWriteMultipleRegistersFrame(int transactionId, int unitId, int startAddress, int[] registerValues) {
        if (registerValues == null || registerValues.length == 0) {
            throw new IllegalArgumentException("写入的寄存器值不能为空");
        }
        int quantity = registerValues.length;
        int byteCount = quantity * 2;

        // PDU: [0x10][起始地址Hi][起始地址Lo][数量Hi][数量Lo][字节数][数据...]
        byte[] pdu = new byte[1 + 2 + 2 + 1 + byteCount];
        pdu[0] = (byte) 0x10;
        pdu[1] = (byte) ((startAddress >> 8) & 0xFF);
        pdu[2] = (byte) (startAddress & 0xFF);
        pdu[3] = (byte) ((quantity >> 8) & 0xFF);
        pdu[4] = (byte) (quantity & 0xFF);
        pdu[5] = (byte) byteCount;
        int off = 6;
        for (int v : registerValues) {
            pdu[off++] = (byte) ((v >> 8) & 0xFF);
            pdu[off++] = (byte) (v & 0xFF);
        }

        // MBAP: [事务ID2][协议ID2=0][长度2=PDU+1][单元ID1]
        byte[] mbap = new byte[7];
        mbap[0] = (byte) ((transactionId >> 8) & 0xFF);
        mbap[1] = (byte) (transactionId & 0xFF);
        mbap[2] = 0x00;
        mbap[3] = 0x00;
        int length = pdu.length + 1;
        mbap[4] = (byte) ((length >> 8) & 0xFF);
        mbap[5] = (byte) (length & 0xFF);
        mbap[6] = (byte) (unitId & 0xFF);

        // 拼装完整帧
        byte[] frame = new byte[mbap.length + pdu.length];
        System.arraycopy(mbap, 0, frame, 0, mbap.length);
        System.arraycopy(pdu, 0, frame, mbap.length, pdu.length);
        return frame;
    }

    /**
     * 构造 Modbus TCP 写单个保持寄存器（功能码 0x06）完整帧
     * PDU: [0x06][寄存器地址Hi][寄存器地址Lo][值Hi][值Lo]
     */
    public byte[] buildWriteSingleRegisterFrame(int transactionId, int unitId, int address, int value) {
        byte[] pdu = new byte[1 + 2 + 2];
        pdu[0] = (byte) 0x06;
        pdu[1] = (byte) ((address >> 8) & 0xFF);
        pdu[2] = (byte) (address & 0xFF);
        pdu[3] = (byte) ((value >> 8) & 0xFF);
        pdu[4] = (byte) (value & 0xFF);

        byte[] mbap = new byte[7];
        mbap[0] = (byte) ((transactionId >> 8) & 0xFF);
        mbap[1] = (byte) (transactionId & 0xFF);
        mbap[2] = 0x00;
        mbap[3] = 0x00;
        int length = pdu.length + 1;
        mbap[4] = (byte) ((length >> 8) & 0xFF);
        mbap[5] = (byte) (length & 0xFF);
        mbap[6] = (byte) (unitId & 0xFF);

        byte[] frame = new byte[mbap.length + pdu.length];
        System.arraycopy(mbap, 0, frame, 0, mbap.length);
        System.arraycopy(pdu, 0, frame, mbap.length, pdu.length);
        return frame;
    }

    // 类型安全的段：根据数据类型自动打包 payload，并设置 count（0x07=bool，0x08=int16，0x09=real32）
    public static class TypedSegment {
        public final int typeId;   // 1B
        public final int count;    // 1B（bool=位数；int=元素数；real=元素数）
        public final byte[] payload;

        private TypedSegment(int typeId, int count, byte[] payload) {
            this.typeId = typeId & 0xFF;
            this.count = count & 0xFF;
            this.payload = payload == null ? new byte[0] : payload;
        }

        // 0x07：bool（1bit，低位优先；与解析中的 (byte >> bitIndex) & 0x01 一致）
        public static TypedSegment ofBools(List<Boolean> values) {
            if (values == null) values = Collections.emptyList();
            int count = values.size();
            int bytes = (count + 7) / 8;
            byte[] payload = new byte[bytes];
            for (int i = 0; i < count; i++) {
                boolean v = Boolean.TRUE.equals(values.get(i));
                int bIndex = i / 8;
                int bitIndex = i % 8; // 低位开始
                if (v) payload[bIndex] |= (byte) (1 << bitIndex);
            }
            return new TypedSegment(0x07, count, payload);
        }

        // 0x08：int（16bit，大端）
        public static TypedSegment ofInt16(List<Integer> values) {
            if (values == null) values = Collections.emptyList();
            int count = values.size();
            byte[] payload = new byte[count * 2];
            int p = 0;
            for (int v : values) {
                payload[p++] = (byte) ((v >> 8) & 0xFF);
                payload[p++] = (byte) (v & 0xFF);
            }
            return new TypedSegment(0x08, count, payload);
        }

        // 0x09：real（32bit IEEE-754，大端）
        public static TypedSegment ofReal32(List<Double> values) {
            if (values == null) values = Collections.emptyList();
            int count = values.size();
            byte[] payload = new byte[count * 4];
            int p = 0;
            for (double d : values) {
                int bits = Float.floatToIntBits((float) d);
                payload[p++] = (byte) ((bits >> 24) & 0xFF);
                payload[p++] = (byte) ((bits >> 16) & 0xFF);
                payload[p++] = (byte) ((bits >> 8) & 0xFF);
                payload[p++] = (byte) (bits & 0xFF);
            }
            return new TypedSegment(0x09, count, payload);
        }
    }

    // 新增：构造自定义数据帧
    public byte[] buildCustomDataFrameTyped(int unitId, int functionCode, int startAddress, int transactionId, List<TypedSegment> segments) {
        if (segments == null) segments = java.util.Collections.emptyList();

        // 计算数据内容 N 字节长度（各段：typeId(1) + count(1) + payload）
        int dataLen = 0;
        for (TypedSegment seg : segments) {
            dataLen += 2 + (seg.payload == null ? 0 : seg.payload.length);
        }
        if (dataLen > 512) {
            throw new IllegalArgumentException("数据长度超过上限 512 字节，当前=" + dataLen);
        }

        // PDU: [functionCode][startAddress 2B][dataLen 2B][segments...]
        byte[] pdu = new byte[1 + 2 + 2 + dataLen];
        int p = 0;
        pdu[p++] = (byte) (functionCode & 0xFF);
        pdu[p++] = (byte) ((startAddress >>> 8) & 0xFF);
        pdu[p++] = (byte) (startAddress & 0xFF);
        pdu[p++] = (byte) ((dataLen >>> 8) & 0xFF);
        pdu[p++] = (byte) (dataLen & 0xFF);
        for (TypedSegment seg : segments) {
            pdu[p++] = (byte) (seg.typeId & 0xFF);
            pdu[p++] = (byte) (seg.count & 0xFF);
            if (seg.payload != null && seg.payload.length > 0) {
                System.arraycopy(seg.payload, 0, pdu, p, seg.payload.length);
                p += seg.payload.length;
            }
        }

        // MBAP: [事务ID2][协议ID2=0][长度2=N+6][unitId 1B]
        byte[] mbap = new byte[7];
        mbap[0] = (byte) ((transactionId >>> 8) & 0xFF);
        mbap[1] = (byte) (transactionId & 0xFF);
        mbap[2] = 0x00;
        mbap[3] = 0x00;
        int mbapLen = pdu.length + 1; // N + 6（unitId 1B + PDU 1+2+2+N）
        mbap[4] = (byte) ((mbapLen >>> 8) & 0xFF);
        mbap[5] = (byte) (mbapLen & 0xFF);
        mbap[6] = (byte) (unitId & 0xFF);

        byte[] frame = new byte[mbap.length + pdu.length];
        System.arraycopy(mbap, 0, frame, 0, mbap.length);
        System.arraycopy(pdu, 0, frame, mbap.length, pdu.length);
        return frame;
    }

    /**
     * 发送自定义数据帧（Typed 段版）
     */
    public void sendCustomDataFrameTyped(Socket socket, int unitId, int functionCode, int startAddress, int transactionId, java.util.List<TypedSegment> segments) throws java.io.IOException {
        byte[] frame = buildCustomDataFrameTyped(unitId, functionCode, startAddress, transactionId, segments);
        java.io.OutputStream out = socket.getOutputStream();
        out.write(frame);
        out.flush();
        log.info("已发送自定义数据帧（Typed）：len={}，func=0x{}，startAddr=0x{}", frame.length, Integer.toHexString(functionCode), Integer.toHexString(startAddress));
    }
}
