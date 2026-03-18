package com.example.qmx.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel(description = "产品统计报表导出请求")
public class ProductSheetReq implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "报表类型（daily/hourly）")
    private String type;

    @ApiModelProperty(value = "开始时间")
    private Date startTime;

    @ApiModelProperty(value = "结束时间")
    private Date endTime;
}
