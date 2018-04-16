package com.tiza.gw.support.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Description: SendMsg
 * Author: DIYILIU
 * Update: 2018-02-09 14:54
 */

public class SendMsg {

    private String deviceId;

    private int cmd;

    private byte[] bytes;

    // 重复次数
    private AtomicInteger time = new AtomicInteger(0);

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public int getTime() {
        return time.incrementAndGet();
    }
}
