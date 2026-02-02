package com.example.qmx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.qmx.common.PageR;
import com.example.qmx.domain.ControlParameter;
import com.example.qmx.dto.CommonPageReq;
import com.example.qmx.dto.ControlParameterSheetReq;
import com.example.qmx.vo.ControlParameterVO;

import java.util.List;

public interface ControlParameterService extends IService<ControlParameter> {

    List<ControlParameterVO> getDataSheet(ControlParameterSheetReq req);

    PageR<List<ControlParameterVO>> pageSelect(CommonPageReq request);
}
