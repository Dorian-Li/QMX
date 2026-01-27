package com.example.qmx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.qmx.domain.SprayRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SprayRecordMapper extends BaseMapper<SprayRecord> {
    // 查询最新喷涂记录
    @Select("SELECT * FROM spray_record WHERE devName = #{devName} ORDER BY id DESC LIMIT 1")
    SprayRecord getLatestRecord(String devName);

    @Insert({
            "<script>",
            "INSERT INTO spray_record (devName, rate, time) VALUES",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.devName}, #{item.rate}, #{item.time})",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("list") List<SprayRecord> list);
}
