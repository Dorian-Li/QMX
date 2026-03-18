package com.example.qmx.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.qmx.common.PageR;
import com.example.qmx.domain.*;
import com.example.qmx.dto.*;
import com.example.qmx.mapper.*;
import com.example.qmx.server.*;
import com.example.qmx.service.*;
import com.example.qmx.vo.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
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
    @Resource
    private ControlParameterService controlParameterService;

    @Resource
    private DeviceStatusService deviceStatusService;

    @Resource
    private ProductDailyService productDailyService;

    @Resource
    private ProductHourlyService productHourlyService;

    @Resource
    private QualityDetectionService qualityDetectionService;

    @Resource
    private SensorService sensorService;

    @Resource
    private SprayRecordService sprayRecordService;

    @Value("${config.auth.username:}")
    private String configAuthUsername;
    @Value("${config.auth.password:}")
    private String configAuthPassword;

    @Autowired
    public MainController(DataServer dataServer, DataToObj dataToObj) {
        this.dataServer = dataServer;
        this.dataToObj = dataToObj;
    }



    @PostMapping("/controlParameter")
    @ApiOperation(value = "控制参数报表导出", notes = "根据参数名称列表和时间范围导出控制参数数据")
    public List<ControlParameterVO> exportControlParameter(@RequestBody ControlParameterSheetReq req) {
        return controlParameterService.getDataSheet(req);
    }

    @PostMapping("/controlParameter/page")
    @ApiOperation(value = "控制参数分页查询", notes = "分页查询控制参数数据")
    public PageR<List<ControlParameterVO>> pageSelectControlParameter(@RequestBody CommonPageReq req) {
        return controlParameterService.pageSelect(req);
    }

    @PostMapping("/deviceStatus")
    @ApiOperation(value = "设备状态报表导出", notes = "根据设备名称列表和时间范围导出设备状态数据")
    public List<DeviceStatusVO> exportDeviceStatus(@RequestBody DeviceStatusSheetReq req) {
        return deviceStatusService.getDataSheet(req);
    }

    @PostMapping("/deviceStatus/page")
    @ApiOperation(value = "设备状态分页查询", notes = "分页查询设备状态数据")
    public PageR<List<DeviceStatusVO>> pageSelectDeviceStatus(@RequestBody CommonPageReq req) {
        return deviceStatusService.pageSelect(req);
    }

    @PostMapping("/productDaily")
    @ApiOperation(value = "产品日产量报表导出", notes = "根据时间范围导出产品日产量数据")
    public List<ProductDailyVO> exportProductDaily(@RequestBody ProductDailySheetReq req) {
        return productDailyService.getDataSheet(req);
    }

    @PostMapping("/productDaily/page")
    @ApiOperation(value = "产品日产量分页查询", notes = "分页查询产品日产量数据")
    public PageR<List<ProductDailyVO>> pageSelectProductDaily(@RequestBody CommonPageReq req) {
        return productDailyService.pageSelect(req);
    }

    @PostMapping("/productHourly")
    @ApiOperation(value = "产品小时产量报表导出", notes = "根据时间范围导出产品小时产量数据")
    public List<ProductHourlyVO> exportProductHourly(@RequestBody ProductHourlySheetReq req) {
        return productHourlyService.getDataSheet(req);
    }

    @PostMapping("/productHourly/page")
    @ApiOperation(value = "产品小时产量分页查询", notes = "分页查询产品小时产量数据")
    public PageR<List<ProductHourlyVO>> pageSelectProductHourly(@RequestBody CommonPageReq req) {
        return productHourlyService.pageSelect(req);
    }

    @PostMapping("/qualityDetection")
    @ApiOperation(value = "质量检测报表导出", notes = "根据时间范围导出质量检测数据")
    public List<QualityDetectionVO> exportQualityDetection(@RequestBody QualityDetectionSheetReq req) {
        return qualityDetectionService.getDataSheet(req);
    }

    @PostMapping("/qualityDetection/page")
    @ApiOperation(value = "质量检测分页查询", notes = "分页查询质量检测数据")
    public PageR<List<QualityDetectionVO>> pageSelectQualityDetection(@RequestBody CommonPageReq req) {
        return qualityDetectionService.pageSelect(req);
    }

    @PostMapping("/sensor")
    @ApiOperation(value = "传感器数据报表导出", notes = "根据设备名称列表和时间范围导出传感器数据")
    public List<SensorVO> exportSensor(@RequestBody SensorSheetReq req) {
        return sensorService.getDataSheet(req);
    }

    @PostMapping("/sensor/page")
    @ApiOperation(value = "传感器数据分页查询", notes = "分页查询传感器数据")
    public PageR<List<SensorVO>> pageSelectSensor(@RequestBody CommonPageReq req) {
        return sensorService.pageSelect(req);
    }

    @PostMapping("/sprayRecord")
    @ApiOperation(value = "喷洒记录报表导出", notes = "根据设备名称列表和时间范围导出喷洒记录数据")
    public List<SprayRecordVO> exportSprayRecord(@RequestBody SprayRecordSheetReq req) {
        return sprayRecordService.getDataSheet(req);
    }

    @PostMapping("/sprayRecord/page")
    @ApiOperation(value = "喷洒记录分页查询", notes = "分页查询喷洒记录数据")
    public PageR<List<SprayRecordVO>> pageSelectSprayRecord(@RequestBody CommonPageReq req) {
        return sprayRecordService.pageSelect(req);
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
        List<Sensor> gun1 = sensorMapper.getLatestByMinuteInRange("喷涂管路1压力", fromTime, limit);
        List<Sensor> gun2 = sensorMapper.getLatestByMinuteInRange("喷涂管路2压力", fromTime, limit);

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
                    } else if (dataId == 0x04) {
                        int iv = v == null ? 0 : Integer.parseInt(String.valueOf(v));
                        val = iv;
                    } else {
                         double dv = v == null ? 0.0 : Double.parseDouble(String.valueOf(v));
                         val = dv;
                     }
                     cp.setValue(val);
                     cp.setTime(java.time.LocalDateTime.now());
                     snapshots.add(cp);
                 }
                if (dataId >= 0x01 && dataId <= 0x03) {
                    int iv = v == null ? 0 : Integer.parseInt(String.valueOf(v));
                    items.add(com.example.qmx.server.DataResponse.ConfigItem.ofChar(dataId, iv));
                } else if (dataId == 0x04) {
                    int iv = v == null ? 0 : Integer.parseInt(String.valueOf(v));
                    items.add(com.example.qmx.server.DataResponse.ConfigItem.ofInt(dataId, iv));
                } else if (dataId >= 0x05 && dataId <= 0x13) {
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
                return "人工一键清洗";
            case 0x02:
                return "枪头清洗控制";
            case 0x03:
                return "供料桶切换";
            case 0x04:
                return "现场运行控制";
            case 0x05:
                return "机器人喷涂速度";
            case 0x06:
                return "定时清洗间隔";
            case 0x07:
                return "喷涂管路1压力报警阈值";
            case 0x08:
                return "喷涂管路2压力报警阈值";
            case 0x09:
                return "清洗泵压力报警阈值";
            case 0x0A:
                return "搅拌器1转速";
            case 0x0B:
                return "搅拌器2转速";
            case 0x0C:
                return "液位传感器1报警阈值";
            case 0x0D:
                return "液位传感器2报警阈值";
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
                "人工一键清洗",
                "枪头清洗控制",
                "供料桶切换",
                "现场运行控制",
                "机器人喷涂速度",
                "定时清洗间隔",
                "喷涂管路1压力报警阈值",
                "喷涂管路2压力报警阈值",
                "清洗泵压力报警阈值",
                "搅拌器1转速",
                "搅拌器2转速",
                "液位传感器1报警阈值",
                "液位传感器2报警阈值"
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

    // 升级文件请求体
    public static class UpgradeRequest {
        public String fileName;
        public String ipAddress;
        public Integer port;
    }

    @PostMapping("/upgrade")
    @ApiOperation(value = "设备升级", notes = "接收前端传递的升级文件信息，调用Python脚本执行升级操作")
    public java.util.Map<String, Object> upgradeDevice(@RequestBody UpgradeRequest req) {
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        
        // 打印接收到的请求参数
        System.out.println("接收到升级请求：fileName=" + req.fileName + ", ipAddress=" + req.ipAddress + ", port=" + req.port);
        
        // 参数验证
        if (req == null) {
            resp.put("ok", false);
            resp.put("code", "400");
            resp.put("msg", "请求参数不能为空");
            return resp;
        }

        if (req.fileName == null || req.fileName.isEmpty()) {
            resp.put("ok", false);
            resp.put("code", "400");
            resp.put("msg", "文件名不能为空");
            return resp;
        }
        
        // 验证文件名格式：必须是 "【名】V【版本号】.bin" 格式
        // 例如：firmware_V1.6.bin、update_V2.0.bin
        if (!req.fileName.matches(".*V\\d+\\.\\d+\\.bin$")) {
            resp.put("ok", false);
            resp.put("code", "400");
            resp.put("msg", "文件名格式错误，必须是 \"【文件名】V【版本号】.bin\" 格式，例如：firmware_V1.6.bin");
            return resp;
        }
        
        if (req.ipAddress == null || req.ipAddress.isEmpty()) {
            resp.put("ok", false);
            resp.put("code", "400");
            resp.put("msg", "IP地址不能为空");
            return resp;
        }
        
        if (req.port == null || req.port <= 0 || req.port > 65535) {
            resp.put("ok", false);
            resp.put("code", "400");
            resp.put("msg", "端口号无效，必须在1-65535之间");
            return resp;
        }
        
        // 构建升级文件路径
        String upgradeFilePath = "E:\\test\\" + req.fileName;
        
        // 检查文件是否存在
        java.io.File file = new java.io.File(upgradeFilePath);
        if (!file.exists()) {
            resp.put("ok", false);
            resp.put("code", "404");
            resp.put("msg", "升级文件不存在");
            return resp;
        }
        
        // 检查脚本是否存在
        String scriptPath = "E:\\test\\test5.py";
        java.io.File scriptFile = new java.io.File(scriptPath);
        if (!scriptFile.exists()) {
            resp.put("ok", false);
            resp.put("code", "500");
            resp.put("msg", "升级脚本不存在");
            return resp;
        }
        
        try {
            // 构建命令
            String[] cmd = new String[]{
                "python",
                scriptPath,
                upgradeFilePath,
                req.ipAddress,
                req.port.toString()
            };
            
            // 打印开始执行脚本信息
            System.out.println("开始执行升级脚本...");
            System.out.println("命令：python " + scriptPath + " " + upgradeFilePath + " " + req.ipAddress + " " + req.port);
            
            // 执行命令
            Process process = Runtime.getRuntime().exec(cmd);
            
            // 读取输出
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream(), "UTF-8")
            );
            
            java.io.BufferedReader errorReader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getErrorStream(), "UTF-8")
            );
            
            // 等待进程执行完成
            int exitCode = process.waitFor();
            
            // 读取输出信息
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                // 实时打印 Python 脚本输出
                System.out.println("[Python输出] " + line);
                output.append(line).append("\n");
            }
            
            // 读取错误信息
            StringBuilder error = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                // 实时打印 Python 脚本错误
                System.err.println("[Python错误] " + line);
                error.append(line).append("\n");
            }
            
            // 关闭流
            reader.close();
            errorReader.close();
            
            String outStr = output.toString();
            String errStr = error.toString();

            if (exitCode == 0) {
                // 检查输出中是否包含成功信息
                if (output.toString().contains("Upgrade Process Completed Successfully")) {
                    // 打印升级成功信息
                    System.out.println("升级操作执行成功");
                    resp.put("ok", true);
                    resp.put("code", "200");
                    resp.put("msg", "升级操作执行成功");
                    resp.put("output", outStr);
                } else {
                    // 打印升级异常信息
                    System.out.println("升级操作执行异常");
                    resp.put("ok", false);
                    resp.put("code", "500");
                    resp.put("msg", "升级操作执行异常");
                    resp.put("output", outStr);
                    if (error.length() > 0) {
                        resp.put("error", errStr);
                    }
                }
            } else {
                // 打印升级失败信息
                System.out.println("升级操作执行失败，退出码：" + exitCode);
                resp.put("ok", false);
                resp.put("code", "500");
                resp.put("msg", "升级操作执行失败");
                if (error.length() > 0) {
                    resp.put("error", errStr);
                } else if (output.length() > 0) {
                    resp.put("error", outStr);
                }
            }
            
        } catch (java.io.IOException e) {
            System.out.println("升级失败: IO错误");
            resp.put("ok", false);
            resp.put("code", "500");
            resp.put("msg", "执行升级操作时发生IO错误: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("升级失败: 进程被中断");
            resp.put("ok", false);
            resp.put("code", "500");
            resp.put("msg", "升级操作被中断: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("升级失败: 未知错误");
            resp.put("ok", false);
            resp.put("code", "500");
            resp.put("msg", "执行升级操作时发生未知错误: " + e.getMessage());
        }
        
        return resp;
    }

}
