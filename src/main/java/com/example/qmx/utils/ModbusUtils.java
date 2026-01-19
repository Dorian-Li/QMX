package com.example.qmx.utils;

import com.serotonin.modbus4j.BatchRead;
import com.serotonin.modbus4j.BatchResults;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ErrorResponseException;
import com.serotonin.modbus4j.exception.ModbusInitException;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.locator.BaseLocator;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Modbus通讯工具类(生产级改进版)
 * <p>
 * 核心特性：
 * 1. 线程安全的连接池管理
 * 2. 引用计数机制防止提前释放连接
 * 3. 细粒度的超时控制（连接获取/操作执行）
 * 4. 自动重试和指数退避策略
 * 5. 定期清理空闲连接
 * <p>
 * 设计原则：
 * - 每个物理连接对应一个ModbusMaster实例
 * - 读写操作自动管理连接生命周期
 * - 强制超时限制防止阻塞
 * - 异常处理区分网络错误和业务错误
 *
 * @author jyy
 * @data 2025-07-01
 */
@Slf4j
public class ModbusUtils {

    /**
     * Modbus4j工厂实例（线程安全）
     */
    private static final ModbusFactory MODBUS_FACTORY = new ModbusFactory();

    /**
     * 连接池：host:port -> ModbusMaster实例
     */
    private static final Map<String, ModbusMaster> MASTER_MAP = new ConcurrentHashMap<>();

    /**
     * 引用计数器：host:port -> 当前引用数
     */
    private static final Map<String, AtomicInteger> CONNECTION_REF_COUNTS = new ConcurrentHashMap<>();

    /**
     * 连接锁：host:port -> 专用锁对象（解决惊群效应）
     */
    private static final Map<String, ReentrantLock> CONNECTION_LOCKS = new ConcurrentHashMap<>();

    /**
     * 默认TCP超时3秒
     */
    private static final int DEFAULT_TIMEOUT = 3000;

    /**
     * 默认重试次数
     */
    private static final int DEFAULT_RETRIES = 3;

    /**
     * 默认使用封装模式
     */
    private static final boolean DEFAULT_ENCAPSULATED = true;

    /**
     * 默认操作超时5秒
     */
    private static final long DEFAULT_OPERATION_TIMEOUT = 5000;

    /**
     * 连接等待超时10秒
     */
    private static final long CONNECTION_WAIT_TIMEOUT = 10000;

    /**
     * 连接清理线程（单例）
     */
    private static final ScheduledExecutorService CLEANUP_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    static {
        /*
         * 启动定期清理任务（每5分钟执行一次）
         * 清理策略：
         * 1. 引用计数为0的空闲连接
         * 2. 初始化失败或已关闭的连接
         */
        CLEANUP_EXECUTOR.scheduleAtFixedRate(
                ModbusUtils::cleanupIdleConnections,
                5, 5, TimeUnit.MINUTES);

        // 添加JVM关闭钩子确保资源释放
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            destroyAll();
            CLEANUP_EXECUTOR.shutdownNow();
        }));
    }


    // ----------------------------------------------- 内部方法 ---------------------------------------------------------

    /**
     * 获取ModbusMaster连接（线程安全 + 超时控制）
     * <p>
     * 实现要点：
     * 1. 使用双重检查锁确保单例
     * 2. 引用计数自动递增
     * 3. 带超时的锁获取防止死锁
     *
     * @param host 设备IP地址
     * @param port 设备端口
     * @return 可用的ModbusMaster实例
     * @throws ModbusTransportException 当连接获取超时或初始化失败时抛出
     */
    public static ModbusMaster getMaster(String host, int port) throws ModbusTransportException {
        String key = getConnectionKey(host, port);
        ReentrantLock lock = CONNECTION_LOCKS.computeIfAbsent(key, k -> new ReentrantLock(true));

        try {
            // 尝试获取锁（带超时）
            if (!lock.tryLock(CONNECTION_WAIT_TIMEOUT, TimeUnit.MILLISECONDS)) {
                throw new ModbusTransportException("获取Modbus连接超时: " + key);
            }

            try {
                // 增加引用计数（原子操作）
                CONNECTION_REF_COUNTS.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();

                // 双重检查锁创建连接
                return MASTER_MAP.computeIfAbsent(key, k -> {
                    try {
                        ModbusMaster master = createTcpMaster(host, port,
                                DEFAULT_TIMEOUT, DEFAULT_RETRIES, DEFAULT_ENCAPSULATED);
                        master.init();
                        log.info("ModbusMaster连接已创建: {}", key);
                        return master;
                    } catch (ModbusInitException e) {
                        log.error("ModbusMaster初始化失败: {}", key, e);
                        throw new RuntimeException("ModbusMaster初始化失败", e);
                    }
                });
            } finally {
                // 释放锁
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModbusTransportException("获取Modbus连接被中断: " + e.getMessage());
        }
    }

    /**
     * 释放连接引用
     * <p>
     * 注意：
     * - 只有引用计数降为0时才实际销毁连接
     * - 线程安全的递减操作
     *
     * @param host host
     * @param port 端口
     */
    public static void release(String host, int port) {
        String key = getConnectionKey(host, port);
        AtomicInteger refCount = CONNECTION_REF_COUNTS.get(key);

        if (refCount != null && refCount.decrementAndGet() <= 0) {
            Object lock = CONNECTION_LOCKS.get(key);
            if (lock != null) {
                synchronized (lock) {
                    // 双重检查防止竞态条件
                    if (refCount.get() <= 0) {
                        destroyInternal(host, port);
                        CONNECTION_REF_COUNTS.remove(key);
                        CONNECTION_LOCKS.remove(key);
                    }
                }
            }
        }
    }

    /**
     * 带超时的任务执行
     * <p>
     * 技术要点：
     * 1. 使用独立线程池执行任务
     * 2. Future.get()实现超时控制
     * 3. 异常转换（ExecutionException -> 业务异常）
     *
     * @param task    要执行的任务
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 任务执行结果
     * @throws ModbusTransportException Modbus传输异常
     * @throws ErrorResponseException   错误响应异常
     * @throws InterruptedException     中断异常
     * @throws TimeoutException         超时异常
     */
    private static <T> T executeWithTimeout(Callable<T> task, long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException, ErrorResponseException, ModbusTransportException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<T> future = executor.submit(task);

        try {
            return future.get(timeout, unit);
        } catch (ExecutionException e) {
            // 异常类型转换
            Throwable cause = e.getCause();
            if (cause instanceof ModbusTransportException) {
                throw (ModbusTransportException) cause;
            }
            if (cause instanceof ErrorResponseException) {
                throw (ErrorResponseException) cause;
            }
            throw new RuntimeException("Modbus操作执行异常", cause);
        } finally {
            // 中断任务
            future.cancel(true);
            // 立即释放资源
            executor.shutdownNow();
        }
    }

    /**
     * 清理空闲连接
     * <p>
     * 策略：
     * 1. 遍历所有连接
     * 2. 移除引用计数为0且未初始化的连接
     * 3. 线程安全的移除操作
     */
    public static void cleanupIdleConnections() {
        MASTER_MAP.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            if (CONNECTION_REF_COUNTS.getOrDefault(key, new AtomicInteger(0)).get() <= 0) {
                try {
                    if (!entry.getValue().isInitialized()) {
                        entry.getValue().destroy();
                        log.info("清理空闲Modbus连接: {}", key);
                        CONNECTION_REF_COUNTS.remove(key);
                        CONNECTION_LOCKS.remove(key);
                        return true;
                    }
                } catch (Exception e) {
                    log.warn("清理Modbus连接失败: {}", key, e);
                }
            }
            return false;
        });
    }

    /**
     * 销毁所有连接（系统关闭时调用）
     */
    public static void destroyAll() {
        MASTER_MAP.forEach((key, master) -> {
            try {
                master.destroy();
                log.info("Modbus连接已关闭: {}", key);
            } catch (Exception e) {
                log.warn("关闭Modbus连接失败: {}", key, e);
            }
        });
        MASTER_MAP.clear();
        CONNECTION_REF_COUNTS.clear();
        CONNECTION_LOCKS.clear();
    }

    /**
     * 创建TCP Master连接
     *
     * @param host         host
     * @param port         端口
     * @param timeout      超时时间
     * @param retries      重试次数
     * @param encapsulated 是否封装
     * @return ModbusMaster
     */
    private static ModbusMaster createTcpMaster(String host, int port,
                                                int timeout, int retries, boolean encapsulated) {
        IpParameters params = new IpParameters();
        params.setHost(host);
        params.setPort(port);

        ModbusMaster master = MODBUS_FACTORY.createTcpMaster(params, encapsulated);
        master.setTimeout(timeout);
        master.setRetries(retries);
        return master;
    }

    /**
     * 生成连接键（host:port格式）
     *
     * @param host host
     * @param port 端口
     * @return 连接键
     */
    private static String getConnectionKey(String host, int port) {
        return host + ":" + port;
    }

    /**
     * 内部销毁方法（无锁版本）
     *
     * @param host host
     * @param port 端口
     */
    private static void destroyInternal(String host, int port) {
        String key = getConnectionKey(host, port);
        ModbusMaster master = MASTER_MAP.remove(key);

        if (master != null) {
            try {
                master.destroy();
                log.info("Modbus连接已关闭: {}", key);
            } catch (Exception e) {
                log.warn("关闭Modbus连接失败: {}", key, e);
            }
        }
    }


    // ----------------------------------------------- 操作方法 ---------------------------------------------------------

    /**
     * 带超时和重试的读取操作
     * <p>
     * 特性：
     * 1. 自动管理连接生命周期（try-with-resources模式）
     * 2. 指数退避重试策略
     * 3. 精确的超时控制
     *
     * @param host       设备IP
     * @param port       设备端口
     * @param locator    数据定位器
     * @param maxRetries 最大重试次数
     * @param timeoutMs  超时时间（毫秒）
     * @return 读取到的数据
     * @throws ModbusTransportException Modbus传输异常
     * @throws ErrorResponseException   错误响应异常
     * @throws InterruptedException     中断异常
     * @throws TimeoutException         超时异常
     */
    public static <T> T readWithRetry(String host, int port, BaseLocator<T> locator, int maxRetries, long timeoutMs)
            throws ModbusTransportException,
            ErrorResponseException, InterruptedException, TimeoutException {

        ModbusMaster master = getMaster(host, port);
        try {
            return executeWithTimeout(() -> {
                int retries = 0;
                while (true) {
                    try {
                        return master.getValue(locator);
                    } catch (ModbusTransportException e) {
                        if (retries++ >= maxRetries) {
                            throw e;
                        }
                        log.warn("Modbus读取失败，第{}次重试...错误: {}", retries, e.getMessage());
                        // 指数退避（最大不超过1秒）
                        TimeUnit.MILLISECONDS.sleep(Math.min(1000, timeoutMs / maxRetries));
                    }
                }
            }, timeoutMs, TimeUnit.MILLISECONDS);
        } finally {
            // 确保释放连接
            release(host, port);
        }
    }

    /**
     * 带超时和重试的批量读取操作
     *
     * @param host      设备IP
     * @param port      设备端口
     * @param batchRead 批量读取对象
     * @param <T>       泛型
     * @return 批量读取结果
     * @throws ModbusTransportException Modbus传输异常
     * @throws ErrorResponseException   错误响应异常
     * @throws InterruptedException     中断异常
     * @throws TimeoutException         超时异常
     */
    public static <T> BatchResults<T> batchReadWithRetry(String host, int port, BatchRead<T> batchRead)
            throws ModbusTransportException, ErrorResponseException, InterruptedException, TimeoutException {
        ModbusMaster master = getMaster(host, port);
        try {
            return executeWithTimeout(() -> {
                int retries = 0;
                while (true) {
                    try {
                        return master.send(batchRead);
                    } catch (ModbusTransportException e) {
                        if (retries++ >= DEFAULT_RETRIES) {
                            throw e;
                        }
                        log.warn("Modbus读取失败，第{}次重试...错误: {}", retries, e.getMessage());
                        // 指数退避（最大不超过1秒）
                        TimeUnit.MILLISECONDS.sleep(Math.min(1000, DEFAULT_OPERATION_TIMEOUT / DEFAULT_RETRIES));
                    }
                }
            }, DEFAULT_OPERATION_TIMEOUT, TimeUnit.MILLISECONDS);
        } finally {
            // 确保释放连接
            release(host, port);
        }
    }

    /**
     * 带超时和重试的写入操作
     * <p>
     * 实现逻辑与readWithRetry类似，区别在于：
     * 1. 使用setValue而非getValue
     * 2. 返回void类型
     *
     * @param host       host
     * @param port       端口
     * @param locator    数据定位器
     * @param value      写入的值
     * @param maxRetries 最大重试次数
     * @param timeoutMs  超时时间（毫秒）
     * @throws ModbusTransportException Modbus传输异常
     * @throws ErrorResponseException   错误响应异常
     * @throws InterruptedException     中断异常
     * @throws TimeoutException         超时异常
     */
    public static <T> void writeWithRetry(String host, int port, BaseLocator<T> locator, T value, int maxRetries, long timeoutMs)
            throws ModbusTransportException,
            ErrorResponseException, InterruptedException, TimeoutException {

        ModbusMaster master = getMaster(host, port);
        try {
            executeWithTimeout(() -> {
                int retries = 0;
                while (true) {
                    try {
                        master.setValue(locator, value);
                        return null;
                    } catch (ModbusTransportException e) {
                        if (retries++ >= maxRetries) {
                            throw e;
                        }
                        log.warn("Modbus写入失败，第{}次重试...错误: {}", retries, e.getMessage());
                        TimeUnit.MILLISECONDS.sleep(Math.min(1000, timeoutMs / maxRetries));
                    }
                }
            }, timeoutMs, TimeUnit.MILLISECONDS);
        } finally {
            release(host, port);
        }
    }

    /**
     * 读取[01 Coil Status 0x]类型 开关数据
     * 读取线圈状态（简化版，使用默认配置）
     *
     * @param slaveId slaveId
     * @param offset  位置
     * @return 读取值
     * @throws ModbusTransportException 异常
     * @throws ErrorResponseException   异常
     */
    public static Boolean readCoilStatus(String host, int port, int slaveId, int offset)
            throws ModbusTransportException, ErrorResponseException,
            InterruptedException, TimeoutException {
        // 01 Coil Status
        BaseLocator<Boolean> loc = BaseLocator.coilStatus(slaveId, offset);

        return readWithRetry(host, port, loc, DEFAULT_RETRIES, DEFAULT_OPERATION_TIMEOUT);
    }


    /**
     * 读取[02 Input Status 1x]类型 开关数据
     *
     * @param slaveId slaveId
     * @param offset  偏移量
     * @return Boolean
     * @throws ModbusTransportException Modbus传输异常
     * @throws ErrorResponseException   错误响应异常
     * @throws InterruptedException     中断异常
     * @throws TimeoutException         超时异常
     */
    public static Boolean readInputStatus(String host, int port, int slaveId, int offset)
            throws ModbusTransportException, ErrorResponseException, InterruptedException, TimeoutException {
        // 02 Input Status
        BaseLocator<Boolean> loc = BaseLocator.inputStatus(slaveId, offset);

        return readWithRetry(host, port, loc, DEFAULT_RETRIES, DEFAULT_OPERATION_TIMEOUT);
    }

    /**
     * 读取[03 Holding Register类型 2x]模拟量数据
     *
     * @param slaveId  slave Id
     * @param offset   位置
     * @param dataType 数据类型,来自com.serotonin.modbus4j.code.DataType
     * @return Number
     * @throws ModbusTransportException Modbus传输异常
     * @throws ErrorResponseException   错误响应异常
     * @throws InterruptedException     中断异常
     * @throws TimeoutException         超时异常
     */
    public static Number readHoldingRegister(String host, int port, int slaveId, int offset, int dataType)
            throws ModbusTransportException, ErrorResponseException, InterruptedException, TimeoutException {
        // 03 Holding Register类型数据读取
        BaseLocator<Number> loc = BaseLocator.holdingRegister(slaveId, offset, dataType);

        return readWithRetry(host, port, loc, DEFAULT_RETRIES, DEFAULT_OPERATION_TIMEOUT);
    }

    /**
     * 读取[04 Input Registers 3x]类型 模拟量数据
     *
     * @param slaveId  slaveId
     * @param offset   位置
     * @param dataType 数据类型,来自com.serotonin.modbus4j.code.DataType
     * @return 返回结果
     * @throws ModbusTransportException Modbus传输异常
     * @throws ErrorResponseException   错误响应异常
     * @throws InterruptedException     中断异常
     * @throws TimeoutException         超时异常
     */
    public static Number readInputRegisters(String host, int port, int slaveId, int offset, int dataType)
            throws ModbusTransportException, ErrorResponseException, InterruptedException, TimeoutException {
        // 04 Input Registers类型数据读取
        BaseLocator<Number> loc = BaseLocator.inputRegister(slaveId, offset, dataType);

        return readWithRetry(host, port, loc, DEFAULT_RETRIES, DEFAULT_OPERATION_TIMEOUT);
    }

    /**
     * 写入数字类型的模拟量（如:写入Float类型的模拟量、Double类型模拟量、整数类型Short、Integer、Long）
     *
     * @param host     host
     * @param port     端口
     * @param slaveId  设备id
     * @param offset   偏移量
     * @param value    写入值,Number的子类,例如写入Float浮点类型,Double双精度类型,以及整型short,int,long
     * @param dataType com.serotonin.modbus4j.code.DataType
     * @throws ModbusTransportException Modbus传输异常
     * @throws ErrorResponseException   错误响应异常
     * @throws InterruptedException     中断异常
     * @throws TimeoutException         超时异常
     */
    public static void writeHoldingRegister(String host, int port, int slaveId, int offset, Number value, int dataType)
            throws ModbusTransportException, ErrorResponseException, InterruptedException, TimeoutException {

        BaseLocator<Number> loc = BaseLocator.holdingRegister(slaveId, offset, dataType);

        writeWithRetry(host, port, loc, value, DEFAULT_RETRIES, DEFAULT_OPERATION_TIMEOUT);
    }
}

