package com.example.qmx.common;

import java.util.Date;

public class CheckUtils {

    public static void checkTime(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            throw new BusinessException("时间不能为空");
        }
        if (startTime.after(endTime)) {
            throw new BusinessException("开始时间不能大于结束时间");
        }
    }

    public static void checkCloseTime(String startTime, String endTime) {
        if (startTime == null || endTime == null) {
            throw new BusinessException("时间不能为空");
        }
        if (startTime.compareTo(endTime) > 0) {
            throw new BusinessException("开始时间不能大于结束时间");
        }
    }

    public static void checkPage(long current, long pageSize) {
        if (current <= 0) {
            throw new BusinessException("当前页码必须大于0");
        }
        if (pageSize <= 0) {
            throw new BusinessException("每页大小必须大于0");
        }
    }

    public static void checkPageType(int type) {
        if (type < 0 || type > 4) {
            throw new BusinessException("分页类型错误");
        }
    }
}
