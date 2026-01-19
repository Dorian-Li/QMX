package com.example.qmx.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = "device_status")
public class DeviceStatus {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("devName")
    private String devName;

    @TableField("status")
    private Integer status;

    @TableField("time")
    private Date time;
}
