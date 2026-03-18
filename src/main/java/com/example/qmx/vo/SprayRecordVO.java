package com.example.qmx.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel(description = "喷洒记录响应")
public class SprayRecordVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "设备名称")
    private String devName;

    @ApiModelProperty(value = "阶段")
    private Integer stage;

    @ApiModelProperty(value = "喷洒速率")
    private Double rate;

    @ApiModelProperty(value = "时间")
    private Date time;
}
