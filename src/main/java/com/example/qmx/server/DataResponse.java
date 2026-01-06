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

        // 构造响应 MBAP：沿用 transactionId/protocolId/unitId，length=unitId(1) + PDU长度
        byte[] respMbap = new byte[7];
        // transactionId
        respMbap[0] = incomingMbap[0];
        respMbap[1] = incomingMbap[1];
        // protocolId
        respMbap[2] = incomingMbap[2];
        respMbap[3] = incomingMbap[3];
        // length (unitId + PDU长度)
        int mbapLen = respPdu.length + 1;
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

    // 构造“错误帧”：功能码原样，数据长度=2，数据内容=0xFFFF（表示 -1）
    private byte[] buildErrorResponseFrame(byte[] incomingMbap, byte[] incomingPdu) {
        byte functionCode = incomingPdu[0];

        // PDU: [func][len_hi=0x00][len_lo=0x02][0xFF][0xFF]
        byte[] respPdu = new byte[1 + 2 + 2];
        respPdu[0] = functionCode;
        respPdu[1] = 0x00;
        respPdu[2] = 0x02;
        respPdu[3] = (byte) 0xFF;
        respPdu[4] = (byte) 0xFF;

        // MBAP: 复用 transactionId/protocolId/unitId，length = unitId(1) + PDU长度
        byte[] respMbap = new byte[7];
        respMbap[0] = incomingMbap[0];
        respMbap[1] = incomingMbap[1];
        respMbap[2] = incomingMbap[2];
        respMbap[3] = incomingMbap[3];
        int mbapLen = respPdu.length + 1;
        respMbap[4] = (byte) ((mbapLen >> 8) & 0xFF);
        respMbap[5] = (byte) (mbapLen & 0xFF);
        respMbap[6] = incomingMbap[6];

        byte[] frame = new byte[respMbap.length + respPdu.length];
        System.arraycopy(respMbap, 0, frame, 0, respMbap.length);
        System.arraycopy(respPdu, 0, frame, respMbap.length, respPdu.length);
        return frame;
    }

    /**
     * 发送响应帧到网关（使用 Socket）
     */
    public int sendResponse(Socket socket, byte[] incomingMbap, byte[] incomingPdu) throws IOException {
        if (socket == null || socket.isClosed()) {
            throw new IOException("Socket 不可用或已关闭，无法发送响应");
        }
        if (incomingMbap == null || incomingMbap.length != 7) {
            throw new IllegalArgumentException("MBAP 头不能为空且长度必须为 7 字节");
        }
        if (incomingPdu == null || incomingPdu.length < 3) {
            throw new IllegalArgumentException("PDU 不能为空且长度至少为 3 字节（功能码+数据长度2字节）");
        }

        int dataLen = ((incomingPdu[1] & 0xFF) << 8) | (incomingPdu[2] & 0xFF);
        int expectedPduLen = 1 + 2 + dataLen;

        OutputStream os = socket.getOutputStream();
        if (incomingPdu.length != expectedPduLen) {
            // 长度不一致，发送表示 -1 的错误帧
            byte[] errorFrame = buildErrorResponseFrame(incomingMbap, incomingPdu);
            os.write(errorFrame);
            os.flush();
            log.warn("PDU 长度不一致，已向网关发送错误帧（数据=-1），长度={} 字节", errorFrame.length);
            return -1;
        }

        // 长度一致，发送正常确认帧
        byte[] frame = buildResponseFrame(incomingMbap, incomingPdu);
        os.write(frame);
        os.flush();
        log.info("PDU 长度一致，已向网关发送确认帧，长度={} 字节，数据长度={}", frame.length, dataLen);
        return dataLen;
    }

// ==========================================================================

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

// ================================== 定长 ====================================

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


// ================================== 不定长 ====================================

    /**
     * 配置项（新 PDU，无类型标识）：数据标识(1B) + 具体数据
     * 数据标识与数据长度：
     *  - 0x01-0x05: char -> 1 字节
     *  - 0x06:      int16 -> 2 字节 (大端)
     *  - 0x07-0x13: real32 -> 4 字节 (IEEE-754 float, 大端)
     */
    public static class ConfigItem {
        public final int dataId;   // 0x01..0x13
        public final byte[] value; // 已编码数据（长度由 dataId 决定）

        private ConfigItem(int dataId, byte[] value) {
            if (dataId < 0x01 || dataId > 0x13) {
                throw new IllegalArgumentException("数据标识不合法，必须为 0x01-0x13，当前=0x" + Integer.toHexString(dataId));
            }
            if (value == null) {
                throw new IllegalArgumentException("具体数据不能为空");
            }
            int expectedLen;
            if (dataId >= 0x01 && dataId <= 0x05) {
                expectedLen = 1;
            } else if (dataId == 0x06) {
                expectedLen = 2;
            } else {
                expectedLen = 4; // 0x07..0x13
            }
            if (value.length != expectedLen) {
                throw new IllegalArgumentException("数据标识 0x" + Integer.toHexString(dataId) +
                        " 的数据长度必须为 " + expectedLen + " 字节，当前=" + value.length);
            }
            this.dataId = dataId & 0xFF;
            this.value = value;
        }

        public static ConfigItem ofChar(int dataId, int byteValue) {
            if (dataId < 0x01 || dataId > 0x05) {
                throw new IllegalArgumentException("char(单字节) 的数据标识必须在 0x01-0x05 范围内");
            }
            if (byteValue < 0x00 || byteValue > 0xFF) {
                throw new IllegalArgumentException("char(单字节) 的取值必须在 0x00-0xFF 范围内");
            }
            byte[] payload = new byte[]{ (byte) (byteValue & 0xFF) };
            return new ConfigItem(dataId, payload);
        }

        public static ConfigItem ofInt(int dataId, int v) {
            if (dataId != 0x06) {
                throw new IllegalArgumentException("int16 的数据标识必须为 0x06");
            }
            byte[] payload = new byte[2];
            payload[0] = (byte) ((v >> 8) & 0xFF);
            payload[1] = (byte) (v & 0xFF);
            return new ConfigItem(dataId, payload);
        }

        public static ConfigItem ofReal(int dataId, double d) {
            if (dataId < 0x07 || dataId > 0x13) {
                throw new IllegalArgumentException("real32 的数据标识必须在 0x07-0x13 范围内");
            }
            int bits = Float.floatToIntBits((float) d);
            byte[] payload = new byte[4];
            payload[0] = (byte) ((bits >> 24) & 0xFF);
            payload[1] = (byte) ((bits >> 16) & 0xFF);
            payload[2] = (byte) ((bits >> 8) & 0xFF);
            payload[3] = (byte) (bits & 0xFF);
            return new ConfigItem(dataId, payload);
        }
    }
    
    /**
     * 构造“前端参数配置下发”完整帧
     * PDU: [functionCode 1B][dataCount 1B][items...]
     * item: [dataId 1B][value N(B)]
     * MBAP: [事务ID2][协议ID2=0][长度2=PDU+1][unitId 1B]
     */
    public byte[] buildConfigDataFrameV2(int transactionId, int unitId, int functionCode, java.util.List<ConfigItem> items) {
        if (items == null) items = java.util.Collections.emptyList();
        int count = items.size();
        if (count > 255) {
            throw new IllegalArgumentException("数据数量超过上限 255，当前=" + count);
        }

        // 计算 items 总字节长度（dataId 1B + value）
        int itemsBytes = 0;
        for (ConfigItem it : items) {
            itemsBytes += 1 + it.value.length;
        }

        // 组装 PDU
        byte[] pdu = new byte[1 + 1 + itemsBytes];
        int p = 0;
        pdu[p++] = (byte) (functionCode & 0xFF);
        pdu[p++] = (byte) (count & 0xFF);
        for (ConfigItem it : items) {
            pdu[p++] = (byte) (it.dataId & 0xFF);
            System.arraycopy(it.value, 0, pdu, p, it.value.length);
            p += it.value.length;
        }

        // MBAP
        byte[] mbap = new byte[7];
        mbap[0] = (byte) ((transactionId >> 8) & 0xFF);
        mbap[1] = (byte) (transactionId & 0xFF);
        mbap[2] = 0x00;
        mbap[3] = 0x00;
        int mbapLen = pdu.length + 1;
        mbap[4] = (byte) ((mbapLen >> 8) & 0xFF);
        mbap[5] = (byte) (mbapLen & 0xFF);
        mbap[6] = (byte) (unitId & 0xFF);

        byte[] frame = new byte[mbap.length + pdu.length];
        System.arraycopy(mbap, 0, frame, 0, mbap.length);
        System.arraycopy(pdu, 0, frame, mbap.length, pdu.length);
        return frame;
    }

    /**
     * 发送“前端参数配置下发”帧（新版：无类型标识）
     */
    public void sendConfigDataFrameV2(java.net.Socket socket, int unitId, int functionCode, int transactionId, java.util.List<ConfigItem> items) throws java.io.IOException {
        byte[] frame = buildConfigDataFrameV2(transactionId, unitId, functionCode, items);
        java.io.OutputStream out = socket.getOutputStream();
        out.write(frame);
        out.flush();
        log.info("已发送配置下发帧(V2，无类型标识)：len={}，func=0x{}，itemCount={}", frame.length, Integer.toHexString(functionCode), (items == null ? 0 : items.size()));
    }
}
