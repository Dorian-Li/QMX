package com.example.qmx.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel(description = "传感器数据响应")
public class SensorVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "设备名称")
    private String devName;

    @ApiModelProperty(value = "传感器值")
    private Float value;

    @ApiModelProperty(value = "时间")
    private Date time;
}
