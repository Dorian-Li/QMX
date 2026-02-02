package com.example.qmx.service.impl;

import org.springframework.util.ObjectUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.qmx.common.CheckUtils;
import com.example.qmx.common.Constants;
import com.example.qmx.common.ErrorCode;
import com.example.qmx.common.InfoConstants;
import com.example.qmx.common.PageR;
import com.example.qmx.common.ThrowUtils;
import com.example.qmx.domain.ControlParameter;
import com.example.qmx.dto.CommonPageReq;
import com.example.qmx.dto.ControlParameterSheetReq;
import com.example.qmx.mapper.ControlParameterMapper;
import com.example.qmx.service.ControlParameterService;
import com.example.qmx.vo.ControlParameterVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ControlParameterServiceImpl extends ServiceImpl<ControlParameterMapper, ControlParameter> implements ControlParameterService {

    @Override
    public List<ControlParameterVO> getDataSheet(ControlParameterSheetReq req) {
        // 1. 基础参数校验
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        Date startTime = req.getStartTime();
        Date endTime = req.getEndTime();
        // 给names赋默认空列表，避免空指针
        List<String> names = req.getNames() == null ? new ArrayList<>() : req.getNames();
        
        // 2. 时间参数校验
        ThrowUtils.throwIf(startTime == null || endTime == null, ErrorCode.PARAMS_ERROR);
        CheckUtils.checkTime(startTime, endTime);
        
        // 3. 构建查询条件（统一构建，批量查询）
        LambdaQueryWrapper<ControlParameter> wrapper = new LambdaQueryWrapper<>();
        // 基础条件：时间范围 + 按时间升序
        LocalDateTime start = toLocalDateTime(startTime);
        LocalDateTime end = toLocalDateTime(endTime);
        wrapper.between(ControlParameter::getTime, start, end)
               .orderByAsc(ControlParameter::getTime);
        
        // 可选条件：批量匹配名称（替代循环eq）
        if (!names.isEmpty()) {
            wrapper.in(ControlParameter::getName, names);
        }
        
        // 4. 复用父类baseMapper查询（删除手动注入的mapper）
        List<ControlParameter> res = baseMapper.selectList(wrapper);
        
        // 5. 转换为VO（使用BeanUtil简化赋值）
        return res.stream().map(entity -> {
            ControlParameterVO vo = new ControlParameterVO();
            BeanUtils.copyProperties(entity, vo);
            vo.setTime(toDate(entity.getTime()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public PageR<List<ControlParameterVO>> pageSelect(CommonPageReq request) {
        ThrowUtils.throwIf(ObjectUtils.isEmpty(request), ErrorCode.PARAMS_ERROR);
        String startTime = request.getStartTime();
        String endTime = request.getEndTime();
        List<String> names = request.getNames();
        long pageSize = ObjectUtils.isEmpty(request.getPageSize()) ? InfoConstants.DEFAULT_PAGE_SIZE : request.getPageSize();
        long current = ObjectUtils.isEmpty(request.getCurrent()) ? InfoConstants.DEFAULT_PAGE_CUR : request.getCurrent();
        String sortField = StringUtils.isBlank(request.getSortField()) ? InfoConstants.COLLECTION_TIME_FIELD : request.getSortField();
        String sortOrder = StringUtils.isBlank(request.getSortOrder()) ? Constants.SORT_ORDER_ASC : request.getSortOrder();
        
        if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
            CheckUtils.checkCloseTime(startTime, endTime);
        }
        CheckUtils.checkPage(current, pageSize);

        QueryWrapper<ControlParameter> query = new QueryWrapper<>();
        
        if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
            query.between("time", startTime, endTime);
        }

        if (names != null && !names.isEmpty()) {
            query.in("name", names);
        }

        if (Constants.SORT_ORDER_ASC.equals(sortOrder)) {
            query.orderByAsc(sortField);
        } else {
            query.orderByDesc(sortField);
        }

        Page<ControlParameter> page = new Page<>(current, pageSize);
        IPage<ControlParameter> pageRes = baseMapper.selectPage(page, query);
        long total = pageRes.getTotal();
        List<ControlParameter> records = pageRes.getRecords();
        List<ControlParameterVO> ans = records.stream().map(entity -> {
            ControlParameterVO vo = new ControlParameterVO();
            BeanUtils.copyProperties(entity, vo);
            vo.setTime(toDate(entity.getTime()));
            return vo;
        }).collect(Collectors.toList());
        return new PageR(total, ans);
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
