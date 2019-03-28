package com.tiza.gw.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.SpringUtil;
import com.tiza.gw.support.model.MsgMemory;
import com.tiza.gw.support.model.MsgPool;
import com.tiza.gw.support.model.SendMsg;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Queue;

/**
 * Description: SendService
 * Author: DIYILIU
 * Update: 2019-03-26 14:59
 */

@Slf4j
public class SendService implements Runnable {

    private MsgPool msgPool;

    private ChannelHandlerContext context;

    private ICache sendCacheProvider;

    private ICache sendServiceCache;


    public SendService(MsgPool msgPool, ChannelHandlerContext context) {
        this.msgPool = msgPool;
        this.context = context;

        // 指令下发缓存
        sendCacheProvider = SpringUtil.getBean("sendCacheProvider");
        // 下发线程缓存
        sendServiceCache = SpringUtil.getBean("sendServiceCache");
    }

    @Override
    public void run() {
        String deviceId = msgPool.getDeviceId();
        Queue<SendMsg> pool = msgPool.getMsgQueue();
        while (!pool.isEmpty()) {
            if (isBlock(deviceId)) {
                log.info("设备[{}]指令下发阻塞!", deviceId);

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                continue;
            }

            SendMsg sendMsg = pool.poll();
            context.writeAndFlush(Unpooled.copiedBuffer(sendMsg.getBytes()));
            log.info("设备[{}, {}, {}]指令已下发!", deviceId, sendMsg.getKey(), CommonUtil.bytesToStr(sendMsg.getBytes()));

            // 清除查询指令队列
            String key = sendMsg.getKey();
            if (StringUtils.isNotEmpty(key)){
                msgPool.getKeyList().remove(key);
            }

            // 参数设置
            if (1 == sendMsg.getType()) {
                log.info("[设置] 设备[{}]参数[{}, {}]设置...", deviceId, sendMsg.getTags(), CommonUtil.bytesToStr(sendMsg.getBytes()));
                updateLog(sendMsg, 1, "");
            }

            MsgMemory msgMemory;
            if (sendCacheProvider.containsKey(deviceId)) {
                msgMemory = (MsgMemory) sendCacheProvider.get(deviceId);
            } else {
                msgMemory = new MsgMemory();
                msgMemory.setDeviceId(deviceId);
                sendCacheProvider.put(deviceId, msgMemory);
            }

            sendMsg.setDateTime(System.currentTimeMillis());
            msgMemory.setCurrent(sendMsg);
        }

        // 线程结束,移除设备
        sendServiceCache.remove(deviceId);
        log.info("[{}]当前线程执行结束!", deviceId);
    }

    /**
     * 更新下发指令状态
     *
     * @param msg
     * @param result
     * @param replyMsg
     */
    public void updateLog(SendMsg msg, int result, String replyMsg) {
        TimerTask timerTask = SpringUtil.getBean("timerTask");
        timerTask.updateLog(msg, result, replyMsg);
    }

    /**
     * 判断设备下行指令是否阻塞
     *
     * @param deviceId
     * @return
     */
    private boolean isBlock(String deviceId) {
        if (sendCacheProvider.containsKey(deviceId)) {
            MsgMemory msgMemory = (MsgMemory) sendCacheProvider.get(deviceId);
            SendMsg current = msgMemory.getCurrent();
            if (current != null && current.getResult() == 0) {
                // 超时 手动置为已处理
                if (System.currentTimeMillis() - current.getDateTime() > 3 * 1000) {
                    log.info("丢弃超时未应答指令, 设备[{}]内容[{}]!", current.getDeviceId(), CommonUtil.bytesToStr(current.getBytes()));
                    current.setResult(1);
                    if (current.getType() == 1) {
                        updateLog(current, 4, "");
                    }

                    return false;
                }

                return true;
            }
        }

        return false;
    }

}
