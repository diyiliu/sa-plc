package com.tiza.gw.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.SpringUtil;
import com.tiza.gw.support.jpa.SendLogJpa;
import com.tiza.gw.support.model.MsgMemory;
import com.tiza.gw.support.model.SendMsg;
import com.tiza.gw.support.model.bean.SendLog;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
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

        Map<String, Long> blockCache = new HashMap();
        List<SendMsg> temps = new ArrayList();
        while (!msgPool.isEmpty()) {
            SendMsg sendMsg = msgPool.poll();
            String deviceId = sendMsg.getDeviceId();

            if (onlineCache.containsKey(deviceId)) {
                // 设备阻塞状态
                if (blockCache.containsKey(deviceId)) {
                    temps.add(sendMsg);
                    continue;
                }

                if (isBlock(deviceId)) {
                    blockCache.put(deviceId, System.currentTimeMillis());
                    if (sendMsg.getTryCount() < 3) {
                        temps.add(sendMsg);
                    } else {
                        log.warn("设备[{}]下行[{}]阻塞超时...", deviceId, CommonUtil.bytesToStr(sendMsg.getBytes()));

                        // 参数设置
                        if (1 == sendMsg.getType()) {
                            updateLog(sendMsg, 3, "");
                        }
                    }

                    continue;
                }

                ChannelHandlerContext context = (ChannelHandlerContext) onlineCache.get(deviceId);
                context.writeAndFlush(Unpooled.copiedBuffer(sendMsg.getBytes()));

                // 参数设置
                if (1 == sendMsg.getType()) {
                    log.info("设备[{}]参数[{}]设置...", deviceId, CommonUtil.bytesToStr(sendMsg.getBytes()));
                    updateLog(sendMsg, 1, "");
                }

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
     * 更新下发指令状态
     *
     * @param msg
     * @param result
     * @param replyMsg
     */
    public static void updateLog(SendMsg msg, int result, String replyMsg) {
        SendLogJpa sendLogJpa = SpringUtil.getBean("sendLogJpa");

        SendLog sendLog = sendLogJpa.findById(msg.getRowId().longValue());
        // 0:未发送;1:已发送;2:成功;3:失败;4:超时;
        sendLog.setResult(result);
        sendLog.setSendData(CommonUtil.bytesToStr(msg.getBytes()));
        sendLog.setReplyData(replyMsg);
        sendLogJpa.save(sendLog);
    }

    /**
     * 清除前面的数据
     *
     * @param sendMsg
     * @param flag
     */
    public static void send(SendMsg sendMsg, boolean flag) {
        if (flag) {
            msgPool.clear();
        }

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
            if (current != null && current.getResult() == 0) {
                if (current.getType() == 1) {
                    updateLog(current, 4, "");
                }

                return true;
            }
        }

        return false;
    }
}
