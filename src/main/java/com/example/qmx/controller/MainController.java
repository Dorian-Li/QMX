package com.example.qmx.controller;

import com.example.qmx.domain.*;
import com.example.qmx.server.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController

@CrossOrigin(origins = "*")
public class MainController {

    private final List<SseEmitter> alarmEmitters = new CopyOnWriteArrayList<>();
    private DataServer dataServer;
    private DataToObj dataToObj;
    
    @Autowired
    public MainController(DataServer dataServer, DataToObj dataToObj) {
        this.dataServer = dataServer;
        this.dataToObj = dataToObj;
    }

    @GetMapping(value = "/getAlarmStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getAlarmStream() {
        SseEmitter emitter = new SseEmitter(0L); // 不设置超时
        alarmEmitters.add(emitter);

        // 连接生命周期管理
        emitter.onCompletion(() -> alarmEmitters.remove(emitter));
        emitter.onTimeout(() -> alarmEmitters.remove(emitter));
        emitter.onError(e -> alarmEmitters.remove(emitter));

        // 初始事件，方便前端确认连接建立
        try {
            Map<String, Object> hello = new HashMap<>();
            hello.put("type", "connected");
            hello.put("timestamp", System.currentTimeMillis());
            emitter.send(hello);
        } catch (IOException ignored) {}

        return emitter;
    }

    public void publishAlarm(Map<String, Object> alarm) {
        if (alarm == null || alarm.isEmpty()) return;

        // 控制台打印
        System.out.println("报警触发: " + alarm);

        // 推送到所有在线 SSE 连接
        List<SseEmitter> toRemove = new ArrayList<>();
        for (SseEmitter emitter : alarmEmitters) {
            try {
                emitter.send(alarm);
            } catch (IOException e) {
                toRemove.add(emitter);
            }
        }
        // 清理失效连接
        alarmEmitters.removeAll(toRemove);
    }

    // 报警测试
    @PostMapping("/getAlarmstest")
    public Map<String, Object> getAlarmstest(@RequestBody Map<String, Object> alarm) {
        publishAlarm(alarm);
        Map<String, Object> resp = new HashMap<>();
        resp.put("ok", true);
        return resp;
    }

}