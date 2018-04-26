package com.tiza.gw.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.diyiliu.plugin.util.CommonUtil;
import com.tiza.gw.support.model.MsgMemory;
import com.tiza.gw.support.model.SendMsg;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Description: SenderTask
 * Author: DIYILIU
 * Update: 2018-01-29 10:45
 */

@Slf4j
public class SenderTask implements ITask {
    private static Queue<SendMsg> msgPool = new ConcurrentLinkedQueue();

    public SenderTask(ICache onlineCache, ICache sendCache) {
        this.onlineCache = onlineCache;
        this.sendCache = sendCache;
    }

    /**
     * 在线设备
     */
    private ICache onlineCache;

    /**
     * 发送缓存
     */
    private ICache sendCache;


    @Override
    public void execute() {
        Set keys = onlineCache.getKeys();
        if (keys.size() < 1) {

            log.warn("无设备在线!");
            return;
        }

        List<SendMsg> temps = new ArrayList();
        while (!msgPool.isEmpty()) {
            SendMsg sendMsg = msgPool.poll();
            String deviceId = sendMsg.getDeviceId();
            if (onlineCache.containsKey(deviceId)) {
                if (isBlock(deviceId)) {
                    if (sendMsg.getTryCount() < 3) {
                        temps.add(sendMsg);
                    }else {
                        log.warn("[{}]设备下行[{}]阻塞超时...", deviceId, CommonUtil.bytesToStr(sendMsg.getBytes()));
                    }
                    continue;
                }

                ChannelHandlerContext context = (ChannelHandlerContext) onlineCache.get(deviceId);
                context.writeAndFlush(Unpooled.copiedBuffer(sendMsg.getBytes()));

                MsgMemory msgMemory;
                if (sendCache.containsKey(deviceId)) {
                    msgMemory = (MsgMemory) sendCache.get(deviceId);
                } else {
                    msgMemory = new MsgMemory();
                    msgMemory.setDeviceId(deviceId);
                    sendCache.put(deviceId, msgMemory);
                }
                sendMsg.setDatetime(System.currentTimeMillis());
                msgMemory.setCurrent(sendMsg);
            }
        }

        msgPool.addAll(temps);
    }

    public static void send(SendMsg sendMsg) {

        msgPool.add(sendMsg);
    }

    /**
     * 判断设备下行指令是否阻塞
     *
     * @param deviceId
     * @return
     */
    private boolean isBlock(String deviceId) {

        if (sendCache.containsKey(deviceId)) {
            MsgMemory msgMemory = (MsgMemory) sendCache.get(deviceId);
            SendMsg current = msgMemory.getCurrent();
            if (current != null && current.getResult() == 0){

                return  true;
            }
        }


        return false;
    }
}
