package com.example.qmx.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = "product_week")
public class ProductWeek {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("numWeekly")
    private Integer numWeekly;

    @TableField("time")
    private Date time;
}
