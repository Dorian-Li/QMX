package com.example.qmx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.qmx.domain.ProductDaily;
import com.example.qmx.dto.ProductSheetReq;
import com.example.qmx.vo.ProductVO;

import java.util.List;

public interface ProductService extends IService<ProductDaily> {

    List<ProductVO> getDataSheet(ProductSheetReq req);
}
