package com.example.qmx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.qmx.domain.ControlParameter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ControlParameterMapper extends BaseMapper<ControlParameter> {
    // 按照name查最新的一条记录
    @Select("SELECT * FROM control_param WHERE name = #{name} ORDER BY id DESC LIMIT 1")
    ControlParameter selectLatestByName(String name);

    @Insert({
            "<script>",
            "INSERT INTO control_param (name, value, time) VALUES",
            "<foreach collection='list' item='item' separator=','>",
            "(#{item.name}, #{item.value}, #{item.time})",
            "</foreach>",
            "</script>"
    })
    int insertBatch(@Param("list") List<ControlParameter> list);
}
