package com.example.qmx.common;

import lombok.Data;

import java.util.List;

@Data
public class PageR<T> {
    private long total;
    private List<T> records;

    public PageR(long total, List<T> records) {
        this.total = total;
        this.records = records;
    }
}
