package com.example.qmx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.qmx.common.PageR;
import com.example.qmx.domain.DeviceStatus;
import com.example.qmx.dto.CommonPageReq;
import com.example.qmx.dto.DeviceStatusSheetReq;
import com.example.qmx.vo.DeviceStatusVO;

import java.util.List;

public interface DeviceStatusService extends IService<DeviceStatus> {

    List<DeviceStatusVO> getDataSheet(DeviceStatusSheetReq req);

    PageR<List<DeviceStatusVO>> pageSelect(CommonPageReq request);
}
