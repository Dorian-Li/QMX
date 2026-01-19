package com.example.qmx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.qmx.domain.DeviceStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DeviceStatusMapper extends BaseMapper<DeviceStatus> {
    // 根据设备名查询最新状态
    @Select("SELECT * FROM device_status WHERE devName = #{devName} ORDER BY id DESC LIMIT 1")
    DeviceStatus getLatestStatus(String devName);

}