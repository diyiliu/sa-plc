package com.tiza.gw.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.diyiliu.plugin.util.CommonUtil;
import com.tiza.gw.support.model.MsgMemory;
import com.tiza.gw.support.model.MsgPool;
import com.tiza.gw.support.model.SendMsg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Queue;

/**
 * 指令下发任务
 * Description: SenderTask
 * Author: DIYILIU
 * Update: 2018-01-29 10:45
 */

@Slf4j
@Service
public class SenderTask implements ITask {

    @Resource
    private ICache singlePoolCache;

    @Resource
    private ICache sendCacheProvider;

    @Resource
    private TimerTask timerTask;


    public void execute() {

    }

    public byte[] fetchData(String deviceId) {
        if (isBlock(deviceId) || !singlePoolCache.containsKey(deviceId)) {
            return null;
        }

        MsgPool msgPool = (MsgPool) singlePoolCache.get(deviceId);
        Queue<SendMsg> pool = msgPool.getMsgQueue();
        if (pool.isEmpty()) {
            return null;
        }

        SendMsg sendMsg = pool.poll();
        byte[] bytes = sendMsg.getBytes();
        log.info("设备[{}, {}]指令已下发!", deviceId, CommonUtil.bytesToStr(bytes));

        // 清除查询指令队列
        String key = sendMsg.getKey();
        if (StringUtils.isNotEmpty(key)) {
            msgPool.getKeyList().remove(key);
        }

        // 参数设置
        if (1 == sendMsg.getType()) {
            timerTask.updateLog(sendMsg, 1, "");
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

        return bytes;
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

                return true;
            }
        }

        return false;
    }

    /**
     * 超时丢弃未应答指令
     *
     * @param deviceId
     */
    public void timeOut(String deviceId) {
        if (sendCacheProvider.containsKey(deviceId)) {
            MsgMemory msgMemory = (MsgMemory) sendCacheProvider.get(deviceId);
            SendMsg current = msgMemory.getCurrent();
            if (current != null && current.getResult() == 0) {
                if (System.currentTimeMillis() - current.getDateTime() > 3 * 1000) {
                    log.info("设备[{}]丢弃超时未应答指令[{}]!", current.getDeviceId(), CommonUtil.bytesToStr(current.getBytes()));
                    current.setResult(1);
                    if (current.getType() == 1) {
                        timerTask.updateLog(current, 4, "");
                    }
                }
            }
        }
    }
}
