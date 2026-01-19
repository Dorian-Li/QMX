package com.example.qmx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.qmx.domain.SprayRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SprayRecordMapper extends BaseMapper<SprayRecord> {
    // 查询最新喷涂记录
    @Select("SELECT * FROM spray_record WHERE devName = #{devName} ORDER BY id DESC LIMIT 1")
    SprayRecord getLatestRecord(String devName);
}