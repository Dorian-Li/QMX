package com.example.qmx.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@ApiModel(description = "设备状态报表导出请求")
public class DeviceStatusSheetReq implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "设备名称列表")
    private List<String> devNames;

    @ApiModelProperty(value = "开始时间")
    private Date startTime;

    @ApiModelProperty(value = "结束时间")
    private Date endTime;
}
