package com.example.qmx.server;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


@Service
public class DataProcessingServer {

    private DataServer dataServer;
    private DataToObj dataToObj;

    // 保持线程引用
    private Thread loopThread;
    public DataProcessingServer(DataServer dataServer,DataToObj dataToObj){
        this.dataServer=dataServer;
        this.dataToObj=dataToObj;
    }
    /**
     * 服务端接收循环，持续监听并处理数据
     */
    // 在应用启动后进入服务端接收循环（守护线程）
    @javax.annotation.PostConstruct
    public void startServerLoop() {
        Thread serverLoopThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 阻塞等待并接收一帧，内部已完成解析与入库
                    String summary = dataServer.fetchData();
                    System.out.println("接收并解析完成: " + summary);
                } catch (Exception e) {
                    System.err.println("接收/解析失败: " + e.getMessage());
                    // 简单退避，避免异常导致忙等
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }, "modbus-gateway-server-loop");
        serverLoopThread.setDaemon(true);
        serverLoopThread.start();

        // 保存线程引用以便停止时使用
        this.loopThread = serverLoopThread;
    }

    // 停止循环
    @javax.annotation.PreDestroy
    public void stopServerLoop() {
        if (this.loopThread != null) {
            this.loopThread.interrupt();
        }
    }



}
