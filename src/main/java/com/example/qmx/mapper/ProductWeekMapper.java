package com.example.qmx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.qmx.domain.ProductHourly;
import com.example.qmx.domain.ProductWeek;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductWeekMapper extends BaseMapper<ProductWeek> {
    @Select("SELECT * FROM product_week WHERE time = (SELECT MAX(time) FROM product_week)")
    ProductWeek getLatestProductWeek();
}