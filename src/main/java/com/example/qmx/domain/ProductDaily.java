package com.example.qmx.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "product_daily")
public class ProductDaily {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("numDaily")
    private Integer numDaily;

    @TableField("time")
    private LocalDateTime time;
}
