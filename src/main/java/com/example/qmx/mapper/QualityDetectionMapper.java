package com.example.qmx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.qmx.domain.QualityDetection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QualityDetectionMapper extends BaseMapper<QualityDetection> {
    @Select("SELECT * FROM quality_result ORDER BY id DESC LIMIT 1")
    QualityDetection getLatestResult();
}
