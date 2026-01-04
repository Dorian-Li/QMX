package com.example.qmx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.qmx.domain.*;
import com.example.qmx.mapper.*;
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
    @Autowired
    private DataServer dataServer;
    @Autowired
    private DataToObj dataToObj;
    @Autowired
    private DeviceStatusMapper deviceStatusMapper;
    @Autowired
    private ProductHourlyMapper productHourlyMapper;
    @Autowired
    private ProductWeekMapper productWeekMapper;
    @Autowired
    private SensorMapper sensorMapper;
    @Autowired
    private SprayRecordMapper sprayRecordMapper;

    @Autowired
    public MainController(DataServer dataServer, DataToObj dataToObj) {
        this.dataServer = dataServer;
        this.dataToObj = dataToObj;
    }

    @GetMapping(value = "/getAlarmStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "获取报警", notes = "返回实时报警")
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

    @GetMapping(value = "/getLatestStatus")
    @ApiOperation(value = "获取设备状态", notes = "返回对应设备的最新状态")
    public DeviceStatus getLatestStatus(String devName) {
        return deviceStatusMapper.getLatestStatus(devName);
    }

    @GetMapping(value = "/getLatestProductHourly")
    @ApiOperation(value = "获取产品小时记录", notes = "返回最新产品小时记录")
    public ProductHourly getLatestProductHourly() {
        return productHourlyMapper.getLatestProductHourly();
    }

    @GetMapping(value = "/getLatestProductWeek")
    @ApiOperation(value = "获取产品周记录", notes = "返回对应设备的最新产品周记录")
    public ProductWeek getLatestProductWeek() {
        return productWeekMapper.getLatestProductWeek();
    }

    @GetMapping(value = "/getLatestSensor")
    @ApiOperation(value = "获取传感器记录", notes = "返回对应设备的最新传感器记录")
    public Sensor getLatestSensor(String devName) {
        return sensorMapper.getLatestSensor(devName);
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


    // 类型安全段的请求体（typeId 固定：0x07-bool、0x08-int、0x09-real）
    public static class TypedSegmentDto {
        public Integer typeId;
        public List<Object> values;
    }

    public static class TypedFrameRequest {
        public Integer unitId;
        public Integer functionCode;
        public Integer startAddress; // 新增：起始地址
        public java.util.List<TypedSegmentDto> segments;
    }

    @PostMapping("/sendConfigData")
    @ApiOperation(value = "按类型标识组帧下发", notes = "0x07-bool、0x08-int16、0x09-real32，按解析格式组帧下发")
    public java.util.Map<String, Object> sendCustomFrameTyped(@RequestBody TypedFrameRequest req) throws Exception {
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        if (req == null || req.unitId == null || req.functionCode == null || req.startAddress == null || req.segments == null || req.segments.isEmpty()) {
            resp.put("ok", false);
            resp.put("msg", "参数不完整：需要 unitId、functionCode、startAddress、segments");
            return resp;
        }
        if (!dataServer.isGatewayConnected()) {
            resp.put("ok", false);
            resp.put("msg", "网关未连接，无法下发配置");
            return resp;
        }

        java.util.List<com.example.qmx.server.DataResponse.TypedSegment> segs = new java.util.ArrayList<>();
        for (TypedSegmentDto dto : req.segments) {
            if (dto == null || dto.typeId == null) {
                resp.put("ok", false);
                resp.put("msg", "segments 中存在空项或缺少 typeId");
                return resp;
            }
            int tid = dto.typeId;
            if (tid == 0x07) {
                java.util.List<Boolean> vals = new java.util.ArrayList<>();
                for (Object o : dto.values) {
                    if (o instanceof Boolean) vals.add((Boolean) o);
                    else if (o instanceof Number) vals.add(((Number) o).intValue() != 0);
                    else if (o instanceof String) vals.add(!"0".equals(o));
                    else vals.add(Boolean.FALSE);
                }
                segs.add(com.example.qmx.server.DataResponse.TypedSegment.ofBools(vals));
            } else if (tid == 0x08) {
                java.util.List<Integer> vals = new java.util.ArrayList<>();
                for (Object o : dto.values) {
                    vals.add(o == null ? 0 : Integer.parseInt(String.valueOf(o)));
                }
                segs.add(com.example.qmx.server.DataResponse.TypedSegment.ofInt16(vals));
            } else if (tid == 0x09) {
                java.util.List<Double> vals = new java.util.ArrayList<>();
                for (Object o : dto.values) {
                    vals.add(o == null ? 0.0 : Double.parseDouble(String.valueOf(o)));
                }
                segs.add(com.example.qmx.server.DataResponse.TypedSegment.ofReal32(vals));
            } else {
                resp.put("ok", false);
                resp.put("msg", "不支持的 typeId: " + tid + "（仅支持 0x07/0x08/0x09）");
                return resp;
            }
        }

        boolean ok = dataServer.sendTypedSegments(req.unitId, req.functionCode, req.startAddress, segs);
        resp.put("ok", ok);
        resp.put("msg", ok ? "发送成功" : "发送失败");
        return resp;
    }

    // 模拟网关连接
    @PostMapping("/listen")
    public java.util.Map<String, Object> startGatewayListen() {
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        new Thread(() -> {
            try {
                // 启动服务端监听并阻塞直到有客户端接入
                dataServer.fetchData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "gateway-listen-thread").start();
        resp.put("ok", true);
        resp.put("msg", "已启动网关监听（等待客户端接入）");
        return resp;
    }
}