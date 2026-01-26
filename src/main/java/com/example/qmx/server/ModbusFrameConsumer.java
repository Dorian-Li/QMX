package com.example.qmx.server;

import com.example.qmx.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class ModbusFrameConsumer {

    private static final Logger log = LoggerFactory.getLogger(ModbusFrameConsumer.class);

    private final DataToObj dataToObj;

    public ModbusFrameConsumer(DataToObj dataToObj) {
        this.dataToObj = dataToObj;
    }

    @RabbitListener(queues = RabbitConfig.MODBUS_QUEUE)
    public void onMessage(byte[] body, Message message) {
        long consumeStart = System.currentTimeMillis();
        Object receivedAtObj = message.getMessageProperties().getHeaders().get("receivedAt");
        Long receivedAt = null;
        if (receivedAtObj instanceof Long) {
            receivedAt = (Long) receivedAtObj;
        } else if (receivedAtObj instanceof Number) {
            receivedAt = ((Number) receivedAtObj).longValue();
        }

        if (body == null || body.length < 1) {
            log.error("收到空消息体");
            return;
        }

        int mbapLen = body[0] & 0xFF;
        if (mbapLen <= 0 || mbapLen + 1 > body.length) {
            log.error("消息体格式不正确，mbapLen={}", mbapLen);
            return;
        }

        int pduLen = body.length - 1 - mbapLen;
        if (pduLen <= 0) {
            log.error("消息体格式不正确，pduLen={}", pduLen);
            return;
        }

        byte[] mbap = new byte[mbapLen];
        System.arraycopy(body, 1, mbap, 0, mbapLen);
        byte[] pdu = new byte[pduLen];
        System.arraycopy(body, 1 + mbapLen, pdu, 0, pduLen);

        int transactionId = ((mbap[0] & 0xFF) << 8) | (mbap[1] & 0xFF);

        if (receivedAt != null) {
            long queueDelay = consumeStart - receivedAt;
            log.info("E2E[出队] txId={}, queueDelayMs={}", transactionId, queueDelay);
        }

        String summary = dataToObj.handleModbusFrame(mbap, pdu);
        long afterHandle = System.currentTimeMillis();
        log.info("E2E[消费完成] txId={}, consumeCostMs={}", transactionId, (afterHandle - consumeStart));
    }
}

