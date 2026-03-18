package com.example.qmx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.qmx.common.PageR;
import com.example.qmx.domain.SprayRecord;
import com.example.qmx.dto.CommonPageReq;
import com.example.qmx.dto.SprayRecordSheetReq;
import com.example.qmx.vo.SprayRecordVO;

import java.util.List;

public interface SprayRecordService extends IService<SprayRecord> {

    List<SprayRecordVO> getDataSheet(SprayRecordSheetReq req);
    PageR<List<SprayRecordVO>> pageSelect(CommonPageReq request);
}
