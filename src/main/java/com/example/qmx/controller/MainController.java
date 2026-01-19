package com.example.qmx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.qmx.domain.*;
import com.example.qmx.mapper.*;
import com.example.qmx.server.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.HashMap;

@RestController

@CrossOrigin(origins = "*")
public class MainController {

    @Autowired
    private DataServer dataServer;
    @Autowired
    private DataToObj dataToObj;
    @Autowired
    private DeviceStatusMapper deviceStatusMapper;
    @Autowired
    private ProductHourlyMapper productHourlyMapper;
    @Autowired
    private ProductDailyMapper productDailyMapper;
    @Autowired
    private SensorMapper sensorMapper;
    @Autowired
    private SprayRecordMapper sprayRecordMapper;
    @Autowired
    private QualityDetectionMapper qualityDetectionMapper;
    @Autowired
    private ControlParameterMapper controlParameterMapper;

    @Value("${config.auth.username:}")
    private String configAuthUsername;
    @Value("${config.auth.password:}")
    private String configAuthPassword;

    @Autowired
    public MainController(DataServer dataServer, DataToObj dataToObj) {
        this.dataServer = dataServer;
        this.dataToObj = dataToObj;
    }

    @GetMapping(value = "/getAlarmStream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "获取报警", notes = "返回实时报警")
    public SseEmitter getAlarmStream() {
        return dataToObj.registerAlarmEmitter();
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

    @GetMapping(value = "/getLatestProductDaily")
    @ApiOperation(value = "获取产品日记录", notes = "返回最新产品日记录")
    public ProductDaily getLatestProductDaily() {
        return productDailyMapper.getLatestProductDaily();
    }

    @GetMapping(value = "/getLatestSensor")
    @ApiOperation(value = "获取传感器记录", notes = "返回对应设备的最新传感器记录")
    public Sensor getLatestSensor(String devName) {
        return sensorMapper.getLatestSensor(devName);
    }

    @GetMapping(value = "/getLatestQualityResult")
    @ApiOperation(value = "获取质量检测结果", notes = "返回最新质量检测结果")
    public QualityDetection getLatestQualityResult() {
        return qualityDetectionMapper.getLatestResult();
    }

    @GetMapping(value = "/getDashboardOverview")
    @ApiOperation(value = "获取大屏总览数据", notes = "返回喷涂产量和质量聚合数据")
    public Map<String, Object> getDashboardOverview() {
        Map<String, Object> result = new HashMap<>();

        ProductHourly latestHourly = productHourlyMapper.getLatestProductHourly();
        ProductDaily latestDay = productDailyMapper.getLatestProductDaily();
        QualityDetection latestQuality = qualityDetectionMapper.getLatestResult();

        Integer latestHourlyOutput = latestHourly != null ? latestHourly.getNumHourly() : null;
        Integer latestDailyOutput = latestDay != null ? latestDay.getNumDaily() : null;
        Integer latestQualityResult = latestQuality != null ? latestQuality.getResult() : null;

        List<ProductHourly> allHourly = productHourlyMapper.selectList(null);
        int totalHourlyOutput = 0;
        for (ProductHourly ph : allHourly) {
            if (ph.getNumHourly() != null) {
                totalHourlyOutput += ph.getNumHourly();
            }
        }

        List<ProductDaily> allDay = productDailyMapper.selectList(null);
        int totalDailyOutput = 0;
        for (ProductDaily pw : allDay) {
            if (pw.getNumDaily() != null) {
                totalDailyOutput += pw.getNumDaily();
            }
        }

        List<QualityDetection> allQuality = qualityDetectionMapper.selectList(null);
        int totalQualityCount = allQuality.size();
        int totalPassQualityCount = 0;
        for (QualityDetection qd : allQuality) {
            if (qd.getResult() != null && qd.getResult() == 1) {
                totalPassQualityCount++;
            }
        }

        result.put("latestHourlyOutput", latestHourlyOutput);
        result.put("latestDailyOutput", latestDailyOutput);
        result.put("totalHourlyOutput", totalHourlyOutput);
        result.put("totalDailyOutput", totalDailyOutput);
        result.put("latestQualityResult", latestQualityResult);
        result.put("totalQualityCount", totalQualityCount);
        result.put("totalPassQualityCount", totalPassQualityCount);

        return result;
    }

    @GetMapping(value = "/getProductHourlyHistory")
    @ApiOperation(value = "获取小时产量历史", notes = "按时间升序返回最近N条小时产量数据")
    public List<ProductHourly> getProductHourlyHistory(@RequestParam(value = "limit", required = false, defaultValue = "24") Integer limit) {
        int l = (limit == null || limit <= 0) ? 24 : Math.min(limit, 1000);
        List<ProductHourly> list = productHourlyMapper.getRecentProductHourly(l);
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        list.sort(java.util.Comparator.comparing(ProductHourly::getTime));
        return list;
    }

    @GetMapping(value = "/getProductWeekHistory")
    @ApiOperation(value = "获取日产量历史", notes = "按时间升序返回最近N条日产量数据")
    public List<ProductDaily> getProductWeekHistory(@RequestParam(value = "limit", required = false, defaultValue = "12") Integer limit) {
        int l = (limit == null || limit <= 0) ? 12 : Math.min(limit, 1000);
        List<ProductDaily> list = productDailyMapper.getRecentProductDaily(l);
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        list.sort(java.util.Comparator.comparing(ProductDaily::getTime));
        return list;
    }

    @GetMapping(value = "/getProductHourlyOnHourHistory")
    @ApiOperation(value = "获取整点小时产量历史", notes = "返回最近N条每小时整点的小时产量数据，按时间升序")
    public List<ProductHourly> getProductHourlyOnHourHistory(@RequestParam(value = "limit", required = false, defaultValue = "24") Integer limit) {
        int l = (limit == null || limit <= 0) ? 24 : Math.min(limit, 1000);
        List<ProductHourly> list = productHourlyMapper.getHourlyLatestByHour(l);
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        list.sort(java.util.Comparator.comparing(ProductHourly::getTime));
        return list;
    }

    @GetMapping(value = "/getProductDailyAtMidnightHistory")
    @ApiOperation(value = "获取0点日产量历史", notes = "返回最近N条每天0点的日产量数据，按时间升序")
    public List<ProductDaily> getProductDailyAtMidnightHistory(@RequestParam(value = "limit", required = false, defaultValue = "30") Integer limit) {
        int l = (limit == null || limit <= 0) ? 30 : Math.min(limit, 1000);
        List<ProductDaily> list = productDailyMapper.getDailyLatestByDay(l);
        if (list == null || list.isEmpty()) {
            return new ArrayList<>();
        }
        list.sort(java.util.Comparator.comparing(ProductDaily::getTime));
        return list;
    }

    @GetMapping(value = "/getBatchLatestStatus")
    @ApiOperation(value = "批量获取设备状态", notes = "根据设备名列表返回最新状态")
    public List<DeviceStatus> getBatchLatestStatus(@RequestParam("devNames") List<String> devNames) {
        List<DeviceStatus> list = new ArrayList<>();
        if (devNames == null) {
            return list;
        }
        for (String devName : devNames) {
            if (devName == null || devName.isEmpty()) {
                continue;
            }
            DeviceStatus status = deviceStatusMapper.getLatestStatus(devName);
            if (status != null) {
                list.add(status);
            }
        }
        return list;
    }

    @GetMapping(value = "/getBatchLatestSensor")
    @ApiOperation(value = "批量获取传感器记录", notes = "根据设备名列表返回最新传感器记录")
    public List<Sensor> getBatchLatestSensor(@RequestParam("devNames") List<String> devNames) {
        List<Sensor> list = new ArrayList<>();
        if (devNames == null) {
            return list;
        }
        for (String devName : devNames) {
            if (devName == null || devName.isEmpty()) {
                continue;
            }
            Sensor sensor = sensorMapper.getLatestSensor(devName);
            if (sensor != null) {
                list.add(sensor);
            }
        }
        return list;
    }

    @GetMapping(value = "/getGunPressureLast15Minutes")
    @ApiOperation(value = "获取喷枪压力最近15分钟数据", notes = "返回喷涂管路1和喷涂管路2在最近15分钟的传感器记录")
    public Map<String, Object> getGunPressureLast15Minutes() {
        long nowMillis = System.currentTimeMillis();
        java.util.Date fromTime = new java.util.Date(nowMillis - 15L * 60L * 1000L);

        int limit = 15;
        List<Sensor> gun1 = sensorMapper.getLatestByMinuteInRange("喷涂管路1", fromTime, limit);
        List<Sensor> gun2 = sensorMapper.getLatestByMinuteInRange("喷涂管路2", fromTime, limit);

        Map<String, Object> result = new HashMap<>();
        result.put("gun1", gun1);
        result.put("gun2", gun2);
        return result;
    }

    public void publishAlarm(Map<String, Object> alarm) {
        dataToObj.publishAlarm(alarm);
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

    public static class ConfigParamDto {
        public Integer dataId;
        public Object value;
    }

    public static class ConfigFrameRequest {
        public Integer unitId;
        public Integer functionCode;
        public java.util.List<ConfigParamDto> params;
    }

    public static class ConfigAuthRequest {
        public String username;
        public String password;
    }

    @PostMapping("/checkConfigAuth")
    @ApiOperation(value = "配置权限校验", notes = "验证用户名和密码是否有权限修改参数配置")
    public java.util.Map<String, Object> checkConfigAuth(@RequestBody ConfigAuthRequest req) {
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        if (req == null || req.username == null || req.password == null) {
            resp.put("ok", false);
            resp.put("msg", "用户名或密码不能为空");
            return resp;
        }
        String username = req.username.trim();
        String password = req.password.trim();
        if (username.isEmpty() || password.isEmpty()) {
            resp.put("ok", false);
            resp.put("msg", "用户名或密码不能为空");
            return resp;
        }
        if (configAuthUsername == null || configAuthUsername.isEmpty() || configAuthPassword == null || configAuthPassword.isEmpty()) {
            resp.put("ok", false);
            resp.put("msg", "后台未配置权限账户，请联系管理员配置");
            return resp;
        }
        if (username.equals(configAuthUsername) && password.equals(configAuthPassword)) {
            resp.put("ok", true);
            resp.put("msg", "权限验证通过");
            return resp;
        }
        resp.put("ok", false);
        resp.put("msg", "用户名或密码错误");
        return resp;
    }

    // @PostMapping("/sendConfigData")
    // @ApiOperation(value = "按类型标识组帧下发", notes = "0x07-bool、0x08-int16、0x09-real32，按解析格式组帧下发")
    // public java.util.Map<String, Object> sendCustomFrameTyped(@RequestBody TypedFrameRequest req) throws Exception {
    //     java.util.Map<String, Object> resp = new java.util.HashMap<>();
    //     if (req == null || req.unitId == null || req.functionCode == null || req.startAddress == null || req.segments == null || req.segments.isEmpty()) {
    //         resp.put("ok", false);
    //         resp.put("msg", "参数不完整：需要 unitId、functionCode、startAddress、segments");
    //         return resp;
    //     }
    //     if (!dataServer.isGatewayConnected()) {
    //         resp.put("ok", false);
    //         resp.put("msg", "网关未连接，无法下发配置");
    //         return resp;
    //     }

    //     java.util.List<com.example.qmx.server.DataResponse.TypedSegment> segs = new java.util.ArrayList<>();
    //     for (TypedSegmentDto dto : req.segments) {
    //         if (dto == null || dto.typeId == null) {
    //             resp.put("ok", false);
    //             resp.put("msg", "segments 中存在空项或缺少 typeId");
    //             return resp;
    //         }
    //         int tid = dto.typeId;
    //         if (tid == 0x07) {
    //             java.util.List<Boolean> vals = new java.util.ArrayList<>();
    //             for (Object o : dto.values) {
    //                 if (o instanceof Boolean) vals.add((Boolean) o);
    //                 else if (o instanceof Number) vals.add(((Number) o).intValue() != 0);
    //                 else if (o instanceof String) vals.add(!"0".equals(o));
    //                 else vals.add(Boolean.FALSE);
    //             }
    //             segs.add(com.example.qmx.server.DataResponse.TypedSegment.ofBools(vals));
    //         } else if (tid == 0x08) {
    //             java.util.List<Integer> vals = new java.util.ArrayList<>();
    //             for (Object o : dto.values) {
    //                 vals.add(o == null ? 0 : Integer.parseInt(String.valueOf(o)));
    //             }
    //             segs.add(com.example.qmx.server.DataResponse.TypedSegment.ofInt16(vals));
    //         } else if (tid == 0x09) {
    //             java.util.List<Double> vals = new java.util.ArrayList<>();
    //             for (Object o : dto.values) {
    //                 vals.add(o == null ? 0.0 : Double.parseDouble(String.valueOf(o)));
    //             }
    //             segs.add(com.example.qmx.server.DataResponse.TypedSegment.ofReal32(vals));
    //         } else {
    //             resp.put("ok", false);
    //             resp.put("msg", "不支持的 typeId: " + tid + "（仅支持 0x07/0x08/0x09）");
    //             return resp;
    //         }
    //     }

    //     boolean ok = dataServer.sendTypedSegments(req.unitId, req.functionCode, req.startAddress, segs);
    //     resp.put("ok", ok);
    //     resp.put("msg", ok ? "发送成功" : "发送失败");
    //     return resp;
    // }

    @PostMapping("/sendConfigDataV2")
    @ApiOperation(value = "下发前端参数配置(V2)", notes = "基于数据标识0x01-0x13的设备参数配置下发")
    public java.util.Map<String, Object> sendConfigDataV2(@RequestBody ConfigFrameRequest req) {
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        if (req == null || req.unitId == null || req.functionCode == null || req.params == null || req.params.isEmpty()) {
            resp.put("ok", false);
            resp.put("msg", "参数不完整：需要 unitId、functionCode、params");
            return resp;
        }
        if (!dataServer.isGatewayConnected()) {
            resp.put("ok", false);
            resp.put("msg", "网关未连接，无法下发配置");
            return resp;
        }

        java.util.List<com.example.qmx.server.DataResponse.ConfigItem> items = new java.util.ArrayList<>();
        java.util.List<ControlParameter> snapshots = new java.util.ArrayList<>();
        try {
            for (ConfigParamDto dto : req.params) {
                if (dto == null || dto.dataId == null) {
                    resp.put("ok", false);
                    resp.put("msg", "params 中存在空项或缺少 dataId");
                    return resp;
                }
                int dataId = dto.dataId;
                Object v = dto.value;
                 String name = mapDataIdToName(dataId);
                 if (name != null) {
                     ControlParameter cp = new ControlParameter();
                     cp.setName(name);
                     double val;
                     if (dataId >= 0x01 && dataId <= 0x05) {
                         int iv = v == null ? 0 : Integer.parseInt(String.valueOf(v));
                         val = iv;
                     } else if (dataId == 0x06) {
                         int iv = v == null ? 0 : Integer.parseInt(String.valueOf(v));
                         val = iv;
                     } else {
                         double dv = v == null ? 0.0 : Double.parseDouble(String.valueOf(v));
                         val = dv;
                     }
                     cp.setValue(val);
                     cp.setTime(new java.util.Date());
                     snapshots.add(cp);
                 }
                if (dataId >= 0x01 && dataId <= 0x05) {
                    int iv = v == null ? 0 : Integer.parseInt(String.valueOf(v));
                    items.add(com.example.qmx.server.DataResponse.ConfigItem.ofChar(dataId, iv));
                } else if (dataId == 0x06) {
                    int iv = v == null ? 0 : Integer.parseInt(String.valueOf(v));
                    items.add(com.example.qmx.server.DataResponse.ConfigItem.ofInt(dataId, iv));
                } else if (dataId >= 0x07 && dataId <= 0x13) {
                    double dv = v == null ? 0.0 : Double.parseDouble(String.valueOf(v));
                    items.add(com.example.qmx.server.DataResponse.ConfigItem.ofReal(dataId, dv));
                } else {
                    resp.put("ok", false);
                    resp.put("msg", "不支持的数据标识: 0x" + Integer.toHexString(dataId));
                    return resp;
                }
            }
        } catch (Exception e) {
            resp.put("ok", false);
            resp.put("msg", "参数转换失败: " + e.getMessage());
            return resp;
        }

        boolean ok = dataServer.sendConfigItemsV2(req.unitId, req.functionCode, items);
        if (ok) {
            for (ControlParameter cp : snapshots) {
                controlParameterMapper.insert(cp);
            }
        }
        resp.put("ok", ok);
        resp.put("msg", ok ? "发送成功" : "发送失败");
        return resp;
    }

    private String mapDataIdToName(int dataId) {
        switch (dataId) {
            case 0x01:
                return "mode01";
            case 0x02:
                return "mode02";
            case 0x03:
                return "mode03";
            case 0x04:
                return "mode04";
            case 0x05:
                return "mode05";
            case 0x06:
                return "mode06";
            case 0x07:
                return "speed07";
            case 0x08:
                return "mode08";
            case 0x09:
                return "time09";
            case 0x0A:
                return "pressure10";
            case 0x0B:
                return "pressure11";
            case 0x0C:
                return "freq12";
            case 0x0D:
                return "freq13";
            default:
                return null;
        }
    }

    @GetMapping("/getConfigData")
    @ApiOperation(value = "获取当前配置参数", notes = "返回最新的控制参数配置")
    public java.util.Map<String, Object> getConfigData() {
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        String[] keys = new String[]{
                "mode01",
                "mode02",
                "mode03",
                "mode04",
                "mode05",
                "mode06",
                "mode08",
                "speed07",
                "time09",
                "pressure10",
                "pressure11",
                "freq12",
                "freq13"
        };
        for (String key : keys) {
            ControlParameter latest = controlParameterMapper.selectLatestByName(key);
            if (latest != null) {
                data.put(key, latest.getValue());
            }
        }
        resp.put("ok", true);
        resp.put("data", data);
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
