package com.example.qmx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.qmx.domain.ProductHourly;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Date;

@Mapper
public interface ProductHourlyMapper extends BaseMapper<ProductHourly> {
    // 查询最新产品小时记录
    @Select("SELECT * FROM product_hourly WHERE time = (SELECT MAX(time) FROM product_hourly)")
    ProductHourly getLatestProductHourly();

    // 查询最近 limit 条小时产量，按时间倒序
    @Select("SELECT * FROM product_hourly ORDER BY time DESC LIMIT #{limit}")
    List<ProductHourly> getRecentProductHourly(@Param("limit") int limit);

    // 查询最近 limit 条“整点”小时产量（time 的分钟和秒为 0）
    @Select("SELECT * FROM product_hourly " +
            "WHERE MINUTE(time) = 0 " +
            "AND SECOND(time) = 0 " +
            "ORDER BY time DESC LIMIT #{limit}")
    List<ProductHourly> getRecentHourlyOnHour(@Param("limit") int limit);

    @Select("SELECT * FROM product_hourly " +
            "ORDER BY ABS(TIMESTAMPDIFF(SECOND, time, #{targetTime})) ASC " +
            "LIMIT 1")
    ProductHourly getNearestHourlyByTime(@Param("targetTime") Date targetTime);

    // 按小时分组，获取每个小时最新的一条记录，返回最近 limit 个小时，按时间升序
    @Select("SELECT ph.* " +
            "FROM product_hourly ph " +
            "JOIN ( " +
            "    SELECT DATE_FORMAT(time, '%Y-%m-%d %H:00:00') AS hour_key, " +
            "           MAX(id) AS max_id " +
            "    FROM product_hourly " +
            "    GROUP BY hour_key " +
            "    ORDER BY hour_key DESC " +
            "    LIMIT #{limit} " +
            ") t ON ph.id = t.max_id " +
            "ORDER BY ph.time ASC")
    List<ProductHourly> getHourlyLatestByHour(@Param("limit") int limit);
}
