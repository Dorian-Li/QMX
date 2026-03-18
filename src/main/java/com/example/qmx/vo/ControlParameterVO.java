package com.example.qmx.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;



@Data
@ApiModel(description = "控制参数响应")
public class ControlParameterVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "参数名称")
    private String name;

    @ApiModelProperty(value = "参数值")
    private double value;

    @ApiModelProperty(value = "时间")
    private Date time;
}
