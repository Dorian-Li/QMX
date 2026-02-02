package com.example.qmx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.qmx.common.*;
import com.example.qmx.domain.ProductDaily;
import com.example.qmx.dto.CommonPageReq;
import com.example.qmx.dto.ProductDailySheetReq;
import com.example.qmx.mapper.ProductDailyMapper;
import com.example.qmx.service.ProductDailyService;
import com.example.qmx.vo.ProductDailyVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductDailyServiceImpl extends ServiceImpl<ProductDailyMapper, ProductDaily> implements ProductDailyService {

    @Override
    public List<ProductDailyVO> getDataSheet(ProductDailySheetReq req) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        Date startTime = req.getStartTime();
        Date endTime = req.getEndTime();
        
        ThrowUtils.throwIf(startTime == null || endTime == null, ErrorCode.PARAMS_ERROR);
        CheckUtils.checkTime(startTime, endTime);
        
        LambdaQueryWrapper<ProductDaily> wrapper = new LambdaQueryWrapper<>();
        LocalDateTime start = toLocalDateTime(startTime);
        LocalDateTime end = toLocalDateTime(endTime);
        wrapper.between(ProductDaily::getTime, start, end)
               .orderByAsc(ProductDaily::getTime);
        List<ProductDaily> list = baseMapper.selectList(wrapper);
        
        return list.stream().map(entity -> {
            ProductDailyVO vo = new ProductDailyVO();
            vo.setId(entity.getId());
            vo.setNumDaily(entity.getNumDaily());
            vo.setTime(toDate(entity.getTime()));
            return vo;
        }).collect(Collectors.toList());
    }

    private LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    @Override
    public PageR<List<ProductDailyVO>> pageSelect(CommonPageReq request) {
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

        QueryWrapper<ProductDaily> query = new QueryWrapper<>();
        
        if (StringUtils.isNotBlank(startTime) && StringUtils.isNotBlank(endTime)) {
            query.between("time", startTime, endTime);
        }

        if (Constants.SORT_ORDER_ASC.equals(sortOrder)) {
            query.orderByAsc(sortField);
        } else {
            query.orderByDesc(sortField);
        }

        Page<ProductDaily> page = new Page<>(current, pageSize);
        IPage<ProductDaily> pageRes = baseMapper.selectPage(page, query);
        long total = pageRes.getTotal();
        List<ProductDaily> records = pageRes.getRecords();
        List<ProductDailyVO> ans = records.stream().map(entity -> {
            ProductDailyVO vo = new ProductDailyVO();
            vo.setId(entity.getId());
            vo.setNumDaily(entity.getNumDaily());
            vo.setTime(toDate(entity.getTime()));
            return vo;
        }).collect(Collectors.toList());
        return new PageR(total, ans);
    }
}
