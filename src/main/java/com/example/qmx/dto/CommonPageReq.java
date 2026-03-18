package com.example.qmx.dto;

import lombok.Data;

import java.util.List;

/**
 * 通用分页查询请求DTO
 */
@Data
public class CommonPageReq {
    /**
     * 开始时间（格式建议：yyyy-MM-dd HH:mm:ss）
     */
    private String startTime;

    /**
     * 结束时间（格式建议：yyyy-MM-dd HH:mm:ss）
     */
    private String endTime;

    /**
     * 名称列表（设备名或参数名）
     */
    private List<String> names;

    /**
     * 分页类型（预留扩展字段）
     */
    private Integer type;

    /**
     * 每页条数（默认10条）
     */
    private Long pageSize = 10L;

    /**
     * 当前页码（默认第1页）
     */
    private Long current = 1L;

    /**
     * 排序字段（如：time、devName）
     */
    private String sortField;

    /**
     * 排序方式（ASC：升序，DESC：降序）
     */
    private String sortOrder;
}