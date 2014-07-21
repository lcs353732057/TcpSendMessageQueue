package com.example.tcpsendmessagedemo.tcp;

/**
 * Created by Administrator on 2014/7/4.
 */
public class NSMessage {
    /**
     * 消息发送是否成功       0发送失败  ; 1成功; 2正在发送
     */
    private int send;

    private int retry;

    private long localTime;

    private int localId;

    private int groupId;

    private String text;

    public NSMessage(int send, int retry, long localTime, int localId, int groupId, String text) {
        this.send = send;
        this.retry = retry;
        this.localTime = localTime;
        this.localId = localId;
        this.groupId = groupId;
        this.text = text;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public long getLocalTime() {
        return localTime;
    }

    public void setLocalTime(long localTime) {
        this.localTime = localTime;
    }

    public int getSend() {
        return send;
    }

    public void setSend(int send) {
        this.send = send;
    }

    public int getLocalId() {
        return localId;
    }

    public void setLocalId(int localId) {
        this.localId = localId;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
