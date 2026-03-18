package com.example.qmx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.qmx.common.*;
import com.example.qmx.domain.QualityDetection;
import com.example.qmx.dto.CommonPageReq;
import com.example.qmx.dto.QualityDetectionSheetReq;
import com.example.qmx.mapper.QualityDetectionMapper;
import com.example.qmx.service.QualityDetectionService;
import com.example.qmx.vo.QualityDetectionVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QualityDetectionServiceImpl extends ServiceImpl<QualityDetectionMapper, QualityDetection> implements QualityDetectionService {

    @Resource
    private QualityDetectionMapper qualityDetectionMapper;

    @Override
    public List<QualityDetectionVO> getDataSheet(QualityDetectionSheetReq req) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        Date startTime = req.getStartTime();
        Date endTime = req.getEndTime();
        
        ThrowUtils.throwIf(startTime == null || endTime == null, ErrorCode.PARAMS_ERROR);
        CheckUtils.checkTime(startTime, endTime);
        
        LambdaQueryWrapper<QualityDetection> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(QualityDetection::getTime, startTime, endTime)
               .orderByAsc(QualityDetection::getTime);
        List<QualityDetection> list = qualityDetectionMapper.selectList(wrapper);
        
        return list.stream().map(entity -> {
            QualityDetectionVO vo = new QualityDetectionVO();
            vo.setId(entity.getId());
            vo.setResult(entity.getResult());
            vo.setTime(entity.getTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public PageR<List<QualityDetectionVO>> pageSelect(CommonPageReq request) {
        ThrowUtils.throwIf(ObjectUtils.isEmpty(request), ErrorCode.PARAMS_ERROR);
        String startTime = request.getStartTime();
        String endTime = request.getEndTime();
        long pageSize = ObjectUtils.isEmpty(request.getPageSize()) ? InfoConstants.DEFAULT_PAGE_SIZE : request.getPageSize();
        long current = ObjectUtils.isEmpty(request.getCurrent()) ? InfoConstants.DEFAULT_PAGE_CUR : request.getCurrent();
        String sortField = StringUtils.isBlank(request.getSortField()) ? InfoConstants.COLLECTION_TIME_FIELD : request.getSortField();
        String sortOrder = StringUtils.isBlank(request.getSortOrder()) ? Constants.SORT_ORDER_ASC : request.getSortOrder();
        
        if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
            CheckUtils.checkCloseTime(startTime, endTime);
        }
        CheckUtils.checkPage(current, pageSize);

        QueryWrapper<QualityDetection> query = new QueryWrapper<>();
        
        if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
            query.between("time", startTime, endTime);
        }

        if (Constants.SORT_ORDER_ASC.equals(sortOrder)) {
            query.orderByAsc(sortField);
        } else {
            query.orderByDesc(sortField);
        }

        Page<QualityDetection> page = new Page<>(current, pageSize);
        IPage<QualityDetection> pageRes = qualityDetectionMapper.selectPage(page, query);
        long total = pageRes.getTotal();
        List<QualityDetection> records = pageRes.getRecords();
        List<QualityDetectionVO> ans = records.stream().map(entity -> {
            QualityDetectionVO vo = new QualityDetectionVO();
            vo.setId(entity.getId());
            vo.setResult(entity.getResult());
            vo.setTime(entity.getTime());
            return vo;
        }).collect(Collectors.toList());
        return new PageR(total, ans);
    }
}
