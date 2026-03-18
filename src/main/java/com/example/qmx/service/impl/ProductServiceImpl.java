package com.example.qmx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.qmx.common.CheckUtils;
import com.example.qmx.common.ErrorCode;
import com.example.qmx.common.ThrowUtils;
import com.example.qmx.domain.ProductDaily;
import com.example.qmx.domain.ProductHourly;
import com.example.qmx.dto.ProductSheetReq;
import com.example.qmx.mapper.ProductDailyMapper;
import com.example.qmx.mapper.ProductHourlyMapper;
import com.example.qmx.service.ProductService;
import com.example.qmx.vo.ProductVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductDailyMapper, ProductDaily> implements ProductService {

    @Resource
    private ProductDailyMapper productDailyMapper;

    @Resource
    private ProductHourlyMapper productHourlyMapper;

    @Override
    public List<ProductVO> getDataSheet(ProductSheetReq req) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        Date startTime = req.getStartTime();
        Date endTime = req.getEndTime();
        String type = req.getType();
        
        ThrowUtils.throwIf(startTime == null || endTime == null, ErrorCode.PARAMS_ERROR);
        CheckUtils.checkTime(startTime, endTime);
        
        if ("daily".equals(type)) {
            return getDataSheetDaily(startTime, endTime);
        } else if ("hourly".equals(type)) {
            return getDataSheetHourly(startTime, endTime);
        } else {
            ThrowUtils.throwIf(true, "报表类型错误，必须是 daily 或 hourly");
            return null;
        }
    }

    private List<ProductVO> getDataSheetDaily(Date startTime, Date endTime) {
        LambdaQueryWrapper<ProductDaily> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(ProductDaily::getTime, startTime, endTime)
               .orderByAsc(ProductDaily::getTime);
        List<ProductDaily> list = productDailyMapper.selectList(wrapper);
        return list.stream().map(daily -> {
            ProductVO vo = new ProductVO();
            vo.setId(daily.getId());
            vo.setNumDaily(daily.getNumDaily());
            vo.setTime(daily.getTime());
            return vo;
        }).collect(Collectors.toList());
    }

    private List<ProductVO> getDataSheetHourly(Date startTime, Date endTime) {
        LambdaQueryWrapper<ProductHourly> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(ProductHourly::getTime, startTime, endTime)
               .orderByAsc(ProductHourly::getTime);
        List<ProductHourly> list = productHourlyMapper.selectList(wrapper);
        return list.stream().map(hourly -> {
            ProductVO vo = new ProductVO();
            vo.setId(hourly.getId());
            vo.setNumHourly(hourly.getNumHourly());
            vo.setTime(hourly.getTime());
            return vo;
        }).collect(Collectors.toList());
    }
}
