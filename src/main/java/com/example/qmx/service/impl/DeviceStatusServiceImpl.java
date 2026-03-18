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
import com.example.qmx.domain.DeviceStatus;
import com.example.qmx.dto.CommonPageReq;
import com.example.qmx.dto.DeviceStatusSheetReq;
import com.example.qmx.mapper.DeviceStatusMapper;
import com.example.qmx.service.DeviceStatusService;
import com.example.qmx.vo.DeviceStatusVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeviceStatusServiceImpl extends ServiceImpl<DeviceStatusMapper, DeviceStatus> implements DeviceStatusService {

    @Override
    public List<DeviceStatusVO> getDataSheet(DeviceStatusSheetReq req) {
        // 1. 基础参数校验
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        Date startTime = req.getStartTime();
        Date endTime = req.getEndTime();
        // 给devNames赋默认空列表，避免空指针
        List<String> devNames = req.getDevNames() == null ? new ArrayList<>() : req.getDevNames();
        
        // 2. 时间参数合法性校验
        ThrowUtils.throwIf(startTime == null || endTime == null, ErrorCode.PARAMS_ERROR);
        CheckUtils.checkTime(startTime, endTime);
        
        // 3. 构建查询条件（统一批量查询，替代循环）
        LambdaQueryWrapper<DeviceStatus> wrapper = new LambdaQueryWrapper<>();
        // 基础条件：时间范围 + 按时间升序
        wrapper.between(DeviceStatus::getTime, startTime, endTime)
               .orderByAsc(DeviceStatus::getTime);
        
        // 可选条件：批量（无需循环）
        if (!devNames.isEmpty()) {
            wrapper.in(DeviceStatus::getDevName, devNames);
        }
        
        // 4. 复用父类baseMapper查询（删除手动注入的mapper）
        List<DeviceStatus> res = baseMapper.selectList(wrapper);
        
        // 5. 转换为VO（使用BeanUtil简化赋值）
        return res.stream().map(entity -> {
            DeviceStatusVO vo = new DeviceStatusVO();
            BeanUtils.copyProperties(entity, vo); // 自动拷贝id/devName/status/time
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public PageR<List<DeviceStatusVO>> pageSelect(CommonPageReq request) {
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

        QueryWrapper<DeviceStatus> query = new QueryWrapper<>();
        
        if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
            query.between("time", startTime, endTime);
        }

        if (names != null && !names.isEmpty()) {
            query.in("devName", names);
        }

        if (Constants.SORT_ORDER_ASC.equals(sortOrder)) {
            query.orderByAsc(sortField);
        } else {
            query.orderByDesc(sortField);
        }

        Page<DeviceStatus> page = new Page<>(current, pageSize);
        IPage<DeviceStatus> pageRes = baseMapper.selectPage(page, query);
        long total = pageRes.getTotal();
        List<DeviceStatus> records = pageRes.getRecords();
        List<DeviceStatusVO> ans = records.stream().map(entity -> {
            DeviceStatusVO vo = new DeviceStatusVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).collect(Collectors.toList());
        return new PageR(total, ans);
    }
}