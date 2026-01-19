package com.example.qmx.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("quality_result")
public class QualityDetection {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("result")
    private Integer result;

    @TableField("time")
    private Date time;

}
