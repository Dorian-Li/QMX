package com.example.qmx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.qmx.domain.Sensor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SensorMapper extends BaseMapper<Sensor> {
    // 根据设备名查询最新传感器记录
    @Select("SELECT * FROM sensor WHERE devName = #{devName} ORDER BY id DESC LIMIT 1")
    Sensor getLatestSensor(String devName);
}