package com.example.tcpsendmessagedemo.tcp;

/**
 * Created by Administrator on 2014/6/30.
 */
public class Heartbeat {
    public byte[] heartbeat = null;
    /**
     * 消息发送是否成功       0 发送失败  ; 1成功; 2正在发送
     */
    public int send;

    public long timeStamp;

    public boolean isDetect;

    /**
     * @param send     消息状态
     * @param isDetect 是否是探测心跳
     */
    public Heartbeat(int send, boolean isDetect) {
        heartbeat = KeepAlivePacket.encodeKeepAlivePacket(TcpClient.KEEP_ALIVE_MSG_ID);
        this.send = send;
        this.isDetect = isDetect;
        this.timeStamp = System.currentTimeMillis();
    }
}
