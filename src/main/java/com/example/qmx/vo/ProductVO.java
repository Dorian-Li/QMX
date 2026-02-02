package com.example.qmx.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel(description = "产品统计响应")
public class ProductVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "日产量")
    private Integer numDaily;

    @ApiModelProperty(value = "小时产量")
    private Integer numHourly;

    @ApiModelProperty(value = "时间")
    private Date time;
}
