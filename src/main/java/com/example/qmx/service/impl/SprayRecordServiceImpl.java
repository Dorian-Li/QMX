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
import com.example.qmx.domain.SprayRecord;
import com.example.qmx.dto.CommonPageReq;
import com.example.qmx.dto.SprayRecordSheetReq;
import com.example.qmx.mapper.SprayRecordMapper;
import com.example.qmx.service.SprayRecordService;
import com.example.qmx.vo.SprayRecordVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SprayRecordServiceImpl extends ServiceImpl<SprayRecordMapper, SprayRecord> implements SprayRecordService {

    @Override
    public List<SprayRecordVO> getDataSheet(SprayRecordSheetReq req) {
        // 1. 参数非空校验
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        Date startTime = req.getStartTime();
        Date endTime = req.getEndTime();
        List<String> devNames = req.getDevNames();
        
        // 2. 时间参数校校验
        ThrowUtils.throwIf(startTime == null || endTime == null, ErrorCode.PARAMS_ERROR);
        CheckUtils.checkTime(startTime, endTime);

        LocalDateTime start = toLocalDateTime(startTime);
        LocalDateTime end = toLocalDateTime(endTime);

        // 3. 构建查询条件（批量查询，替代循环）
        LambdaQueryWrapper<SprayRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.between(SprayRecord::getTime, start, end)
               .orderByAsc(SprayRecord::getTime);
        
        // 3.1 若指定设备名称，批量筛选
        if (devNames != null && !devNames.isEmpty()) {
            wrapper.in(SprayRecord::getDevName, devNames);
        }
        
        // 4. 一次性查询所有符合条件的数据（复用父类baseMapper）
        List<SprayRecord> res = baseMapper.selectList(wrapper);

        // 5. (使用BeanUtil简化赋值)
        return res.stream().map(entity -> {
            SprayRecordVO vo = new SprayRecordVO();
            BeanUtils.copyProperties(entity, vo);
            vo.setTime(toDate(entity.getTime()));
            return vo;
        }).collect(Collectors.toList());
    }


    @Override
    public PageR<List<SprayRecordVO>> pageSelect(CommonPageReq request) {
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

        QueryWrapper<SprayRecord> query = new QueryWrapper<>();
        
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

        Page<SprayRecord> page = new Page<>(current, pageSize);
        IPage<SprayRecord> pageRes = baseMapper.selectPage(page, query);
        long total = pageRes.getTotal();
        List<SprayRecord> records = pageRes.getRecords();
        List<SprayRecordVO> ans = records.stream().map(entity -> {
            SprayRecordVO vo = new SprayRecordVO();
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
