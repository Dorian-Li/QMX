package com.example.qmx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.qmx.common.*;
import com.example.qmx.domain.ProductHourly;
import com.example.qmx.dto.CommonPageReq;
import com.example.qmx.dto.ProductHourlySheetReq;
import com.example.qmx.mapper.ProductHourlyMapper;
import com.example.qmx.service.ProductHourlyService;
import com.example.qmx.vo.ProductHourlyVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductHourlyServiceImpl extends ServiceImpl<ProductHourlyMapper, ProductHourly> implements ProductHourlyService {

    @Override
    public List<ProductHourlyVO> getDataSheet(ProductHourlySheetReq req) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        Date startTime = req.getStartTime();
        Date endTime = req.getEndTime();
        
        ThrowUtils.throwIf(startTime == null || endTime == null, ErrorCode.PARAMS_ERROR);
        CheckUtils.checkTime(startTime, endTime);
        
        LambdaQueryWrapper<ProductHourly> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(ProductHourly::getTime, startTime, endTime)
               .orderByAsc(ProductHourly::getTime);
        List<ProductHourly> list = baseMapper.selectList(wrapper);
        
        return list.stream().map(entity -> {
            ProductHourlyVO vo = new ProductHourlyVO();
            vo.setId(entity.getId());
            vo.setNumHourly(entity.getNumHourly());
            vo.setTime(entity.getTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public PageR<List<ProductHourlyVO>> pageSelect(CommonPageReq request) {
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

        QueryWrapper<ProductHourly> query = new QueryWrapper<>();
        
        if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
            query.between("time", startTime, endTime);
        }

        if (Constants.SORT_ORDER_ASC.equals(sortOrder)) {
            query.orderByAsc(sortField);
        } else {
            query.orderByDesc(sortField);
        }

        Page<ProductHourly> page = new Page<>(current, pageSize);
        IPage<ProductHourly> pageRes = baseMapper.selectPage(page, query);
        long total = pageRes.getTotal();
        List<ProductHourly> records = pageRes.getRecords();
        List<ProductHourlyVO> ans = records.stream().map(entity -> {
            ProductHourlyVO vo = new ProductHourlyVO();
            vo.setId(entity.getId());
            vo.setNumHourly(entity.getNumHourly());
            vo.setTime(entity.getTime());
            return vo;
        }).collect(Collectors.toList());
        return new PageR(total, ans);
    }
}
