package com.tiza.gw.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.SpringUtil;
import com.tiza.gw.support.dao.dto.SendLog;
import com.tiza.gw.support.dao.jpa.SendLogJpa;
import com.tiza.gw.support.model.MsgMemory;
import com.tiza.gw.support.model.SendMsg;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * 指令下发任务
 * <p>
 * Description: SenderTask
 * Author: DIYILIU
 * Update: 2018-01-29 10:45
 */

@Slf4j
@Service
public class SenderTask implements ITask {
    /**
     * 查询指令
     */
    private static Queue<SendMsg> msgPool = new ConcurrentLinkedQueue();
    /**
     * 设置指令
     */
    private static Queue<SendMsg> setupPool = new ConcurrentLinkedQueue();


    /**
     * 在线设备
     */
    @Resource
    private ICache onlineCacheProvider;


    /**
     * 发送缓存
     */
    @Resource
    private ICache sendCacheProvider;


    /**
     * 生产缓存
     **/
    private static ConcurrentMap<String, Long> produceMap = new ConcurrentHashMap();


    @Scheduled(fixedDelay = 1000, initialDelay = 5 * 1000)
    public void execute() {
        Map<String, Long> blockCache = new HashMap();

        List<SendMsg> tempMsg = new ArrayList();
        List<SendMsg> tempSet = new ArrayList();
        while (!msgPool.isEmpty() || !setupPool.isEmpty()) {
            SendMsg sendMsg;
            if (setupPool.isEmpty()) {
                sendMsg = msgPool.poll();
            } else {
                sendMsg = setupPool.poll();
            }
            String deviceId = sendMsg.getDeviceId();

            String proKey = deviceId + "_" + sendMsg.getKey();
            if (produceMap.containsKey(proKey)){
                produceMap.remove(proKey);
            }

            /*
            // 过滤重复查询
            if (sendCacheProvider.containsKey(deviceId)) {
                String qKey = sendMsg.getKey();
                long frequency = sendMsg.getUnitList().get(0).getFrequency();
                MsgMemory msgMemory = (MsgMemory) sendCacheProvider.get(deviceId);
                SendMsg msg = msgMemory.getMsgMap().get(qKey);
                if (msg != null && (System.currentTimeMillis() - msg.getDateTime()) * 0.001 < frequency) {

                    continue;
                }
            }
            */

            if (onlineCacheProvider.containsKey(deviceId)) {
                // 设备阻塞状态
                if (blockCache.containsKey(deviceId)) {
                    if (1 == sendMsg.getType()) {
                        tempSet.add(sendMsg);
                    } else {
                        tempMsg.add(sendMsg);
                    }

                    continue;
                }

                if (isBlock(deviceId)) {
                    blockCache.put(deviceId, System.currentTimeMillis());
                    if (sendMsg.getTryCount() < 10) {
                        if (1 == sendMsg.getType()) {
                            tempSet.add(sendMsg);
                        } else {
                            tempMsg.add(sendMsg);
                        }
                    } else {
                        log.warn("设备[{}]下行[{}]阻塞...", deviceId, CommonUtil.bytesToStr(sendMsg.getBytes()));
                        // 下发失败
                        if (1 == sendMsg.getType()) {
                            updateLog(sendMsg, 3, "");
                        }
                    }

                    continue;
                }

                ChannelHandlerContext context = (ChannelHandlerContext) onlineCacheProvider.get(deviceId);
                context.writeAndFlush(Unpooled.copiedBuffer(sendMsg.getBytes()));

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
        }

        msgPool.addAll(tempMsg);
        setupPool.addAll(tempSet);
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
        if (sendLog != null) {
            sendLog.setResult(result);
            sendLog.setSendData(CommonUtil.bytesToStr(msg.getBytes()));
            sendLog.setReplyData(replyMsg);
            sendLogJpa.save(sendLog);
        }
    }

    /**
     * 是否为设置指令
     *
     * @param sendMsg
     * @param flag
     */
    public static void send(SendMsg sendMsg, boolean flag) {
        if (flag) {
            setupPool.add(sendMsg);
        } else {
            msgPool.add(sendMsg);
        }
    }

    public static boolean send(SendMsg sendMsg) {
        String proKey = sendMsg.getDeviceId() + "_" + sendMsg.getKey();
        if (produceMap.containsKey(proKey) &&
                produceMap.get(proKey) - System.currentTimeMillis() < 60 * 1000) {

            return false;
        }

        produceMap.put(proKey, System.currentTimeMillis());
        msgPool.add(sendMsg);

        return true;
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
                if (System.currentTimeMillis() - current.getDateTime() > 20 * 1000) {
                    log.warn("丢弃超时未应答指令, 设备[{}]内容[{}]!", current.getDeviceId(), CommonUtil.bytesToStr(current.getBytes()));

                    current.setResult(1);
                    if (current.getType() == 1) {
                        updateLog(current, 4, "");
                    }
                }

                return true;
            }
        }

        return false;
    }
}
