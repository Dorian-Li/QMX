package com.example.qmx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.qmx.common.PageR;
import com.example.qmx.domain.QualityDetection;
import com.example.qmx.dto.CommonPageReq;
import com.example.qmx.dto.QualityDetectionSheetReq;
import com.example.qmx.vo.QualityDetectionVO;

import java.util.List;

public interface QualityDetectionService extends IService<QualityDetection> {

    List<QualityDetectionVO> getDataSheet(QualityDetectionSheetReq req);
    PageR<List<QualityDetectionVO>> pageSelect(CommonPageReq request);
}
