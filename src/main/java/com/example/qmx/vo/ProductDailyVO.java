package com.example.qmx.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel(description = "产品日产量响应")
public class ProductDailyVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "日产量")
    private Integer numDaily;

    @ApiModelProperty(value = "时间")
    private Date time;
}
