package com.example.qmx.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@ApiModel(description = "控制参数报表导出请求")
public class ControlParameterSheetReq implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "参数名称列表")
    private List<String> names;

    @ApiModelProperty(value = "开始时间")
    private Date startTime;

    @ApiModelProperty(value = "结束时间")
    private Date endTime;
}