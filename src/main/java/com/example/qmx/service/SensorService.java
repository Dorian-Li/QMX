package com.example.qmx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.qmx.common.PageR;
import com.example.qmx.domain.Sensor;
import com.example.qmx.dto.CommonPageReq;
import com.example.qmx.dto.SensorSheetReq;
import com.example.qmx.vo.SensorVO;

import java.util.List;

public interface SensorService extends IService<Sensor> {

    List<SensorVO> getDataSheet(SensorSheetReq req);
    PageR<List<SensorVO>> pageSelect(CommonPageReq request);
}
