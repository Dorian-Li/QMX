package com.example.qmx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.qmx.domain.ControlParameter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ControlParameterMapper extends BaseMapper<ControlParameter> {
    // 按照name查最新的一条记录
    @Select("SELECT * FROM control_parameter WHERE name = #{name} ORDER BY id DESC LIMIT 1")
    ControlParameter selectLatestByName(String name);
}
