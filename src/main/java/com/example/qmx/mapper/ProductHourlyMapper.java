package com.example.qmx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.qmx.domain.ProductHourly;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Mapper
public interface ProductHourlyMapper extends BaseMapper<ProductHourly> {
    // 查询最新产品小时记录
    @Select("SELECT * FROM product_hourly WHERE time = (SELECT MAX(time) FROM product_hourly)")
    ProductHourly getLatestProductHourly();
}