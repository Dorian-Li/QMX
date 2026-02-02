package com.example.qmx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.qmx.common.PageR;
import com.example.qmx.domain.ProductHourly;
import com.example.qmx.dto.CommonPageReq;
import com.example.qmx.dto.ProductHourlySheetReq;
import com.example.qmx.vo.ProductHourlyVO;

import java.util.List;

public interface ProductHourlyService extends IService<ProductHourly> {

    List<ProductHourlyVO> getDataSheet(ProductHourlySheetReq req);
    PageR<List<ProductHourlyVO>> pageSelect(CommonPageReq request);
}
