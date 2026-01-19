package com.example.qmx.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = "product_hourly")
public class ProductHourly {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("numHourly")
    private Integer numHourly;

    @TableField("time")
    private Date time;
}
