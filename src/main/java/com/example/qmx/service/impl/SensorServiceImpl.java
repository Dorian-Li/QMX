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
import com.example.qmx.domain.Sensor;
import com.example.qmx.dto.CommonPageReq;
import com.example.qmx.dto.SensorSheetReq;
import com.example.qmx.mapper.SensorMapper;
import com.example.qmx.service.SensorService;
import com.example.qmx.vo.SensorVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SensorServiceImpl extends ServiceImpl<SensorMapper, Sensor> implements SensorService {

    @Resource
    private SensorMapper sensorMapper;

    @Override
    public List<SensorVO> getDataSheet(SensorSheetReq req) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        Date startTime = req.getStartTime();
        Date endTime = req.getEndTime();
        List<String> devNames = req.getDevNames();
        
        ThrowUtils.throwIf(startTime == null || endTime == null, ErrorCode.PARAMS_ERROR);
        CheckUtils.checkTime(startTime, endTime);
        
        List<Sensor> res = new ArrayList<>();
        
        if (devNames != null && !devNames.isEmpty()) {
            Collections.sort(devNames);
            for (String devName : devNames) {
                LambdaQueryWrapper<Sensor> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Sensor::getDevName, devName)
                       .between(Sensor::getTime, startTime, endTime)
                       .orderByAsc(Sensor::getTime);
                
                List<Sensor> queryResult = sensorMapper.selectList(wrapper);
                res.addAll(queryResult);
            }
        } else {
            LambdaQueryWrapper<Sensor> wrapper = new LambdaQueryWrapper<>();
            wrapper.between(Sensor::getTime, startTime, endTime)
                   .orderByAsc(Sensor::getTime);
            res = sensorMapper.selectList(wrapper);
        }
        
        return res.stream().map(entity -> {
            SensorVO vo = new SensorVO();
            vo.setId(entity.getId());
            vo.setDevName(entity.getDevName());
            vo.setValue(entity.getValue());
            vo.setTime(entity.getTime());
            return vo;
        }).collect(Collectors.toList());
    }


    public PageR<List<SensorVO>> pageSelect(CommonPageReq request) {
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

        QueryWrapper<Sensor> query = new QueryWrapper<>();
        
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

        Page<Sensor> page = new Page<>(current, pageSize);
        IPage<Sensor> pageRes = sensorMapper.selectPage(page, query);
        long total = pageRes.getTotal();
        List<Sensor> records = pageRes.getRecords();
        List<SensorVO> ans = records.stream().map(entity -> {
            SensorVO vo = new SensorVO();
            vo.setId(entity.getId());
            vo.setDevName(entity.getDevName());
            vo.setValue(entity.getValue());
            vo.setTime(entity.getTime());
            return vo;
        }).collect(Collectors.toList());
        return new PageR(total, ans);
    }
}
