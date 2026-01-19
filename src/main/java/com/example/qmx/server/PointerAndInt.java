package com.example.qmx.server;

import com.sun.jna.*;

import java.util.Arrays;
import java.util.List;

public class PointerAndInt extends Structure {
    public Pointer pointer; // 对应 int* 指针
    public int value;       // 对应 int 值

    // 指定结构体字段的顺序
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("pointer", "value");
    }

    // 定义一个用于 Java 调用的帮助类
    public static class ByReference extends PointerAndInt implements Structure.ByReference {}
    public static class ByValue extends PointerAndInt implements Structure.ByValue {}
}
