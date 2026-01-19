package com.example.qmx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.qmx.domain.ProductDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
public interface ProductDailyMapper extends BaseMapper<ProductDaily> {
    @Select("SELECT * FROM product_daily WHERE time = (SELECT MAX(time) FROM product_daily)")
    ProductDaily getLatestProductDaily();

    @Select("SELECT * FROM product_daily ORDER BY time DESC LIMIT #{limit}")
    java.util.List<ProductDaily> getRecentProductDaily(@Param("limit") int limit);

    @Select("SELECT * FROM product_daily " +
            "ORDER BY ABS(TIMESTAMPDIFF(SECOND, time, #{targetTime})) ASC " +
            "LIMIT 1")
    ProductDaily getNearestDailyByTime(@Param("targetTime") Date targetTime);

    @Select("SELECT pd.* " +
            "FROM product_daily pd " +
            "JOIN ( " +
            "    SELECT DATE(time) AS day_key, " +
            "           MAX(id) AS max_id " +
            "    FROM product_daily " +
            "    GROUP BY day_key " +
            "    ORDER BY day_key DESC " +
            "    LIMIT #{limit} " +
            ") t ON pd.id = t.max_id " +
            "ORDER BY pd.time ASC")
    java.util.List<ProductDaily> getDailyLatestByDay(@Param("limit") int limit);
}
