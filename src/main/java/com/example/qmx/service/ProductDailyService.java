package com.example.qmx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.qmx.common.PageR;
import com.example.qmx.domain.ProductDaily;
import com.example.qmx.dto.CommonPageReq;
import com.example.qmx.dto.ProductDailySheetReq;
import com.example.qmx.vo.ProductDailyVO;

import java.util.List;

public interface ProductDailyService extends IService<ProductDaily> {

    List<ProductDailyVO> getDataSheet(ProductDailySheetReq req);
    PageR<List<ProductDailyVO>> pageSelect(CommonPageReq request);
}
