package com.example.qmx.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel(description = "设备状态响应")
public class DeviceStatusVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "设备名称")
    private String devName;

    @ApiModelProperty(value = "状态")
    private Integer status;

    @ApiModelProperty(value = "时间")
    private Date time;
}
