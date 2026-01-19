package com.example.qmx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.qmx.domain.Sensor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SensorMapper extends BaseMapper<Sensor> {
    // 根据设备名查询最新传感器记录
     @Select("SELECT * FROM sensor WHERE devName = #{devName} ORDER BY id DESC LIMIT 1")
     Sensor getLatestSensor(String devName);

    @Select("SELECT s.* " +
            "FROM sensor s " +
            "JOIN ( " +
            "    SELECT DATE_FORMAT(time, '%Y-%m-%d %H:%i:00') AS minute_key, " +
            "           MAX(id) AS max_id " +
            "    FROM sensor " +
            "    WHERE devName = #{devName} " +
            "      AND time >= #{fromTime} " +
            "    GROUP BY minute_key " +
            "    ORDER BY minute_key DESC " +
            "    LIMIT #{limit} " +
            ") t ON s.id = t.max_id " +
            "ORDER BY s.time ASC")
    List<Sensor> getLatestByMinuteInRange(@Param("devName") String devName,
                                          @Param("fromTime") java.util.Date fromTime,
                                          @Param("limit") int limit);
}
