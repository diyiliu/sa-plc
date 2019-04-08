package com.tiza.gw.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.JacksonUtil;
import com.tiza.gw.support.dao.dto.DeviceInfo;
import com.tiza.gw.support.dao.dto.SendLog;
import com.tiza.gw.support.dao.jpa.SendLogJpa;
import com.tiza.gw.support.model.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 定时查询任务
 * Description: TimerTask
 * Author: DIYILIU
 * Update: 2018-04-25 14:03
 */

@Slf4j
@Service
public class TimerTask implements ITask {

    /**
     * 设置线程池
     **/
    private static final ExecutorService sendService = Executors.newCachedThreadPool();


    /**
     * 设备缓存
     */
    @Resource
    private ICache deviceCacheProvider;

    /**
     * 在线设备
     */
    @Resource
    private ICache onlineCacheProvider;

    /**
     * 功能集定时任务
     */
    @Resource
    private ICache timerCacheProvider;

    /**
     * 发送缓存
     */
    @Resource
    private ICache sendCacheProvider;

    /**
     * 单个设备下发缓存
     */
    @Resource
    private ICache singlePoolCache;

    @Resource
    private SendLogJpa sendLogJpa;


    @Scheduled(fixedDelay = 3 * 1000, initialDelay = 10 * 1000)
    public void execute() {
        Set set = onlineCacheProvider.getKeys();
        for (Iterator iterator = set.iterator(); iterator.hasNext(); ) {
            String deviceId = (String) iterator.next();

            if (!deviceCacheProvider.containsKey(deviceId)) {
                continue;
            }
            DeviceInfo deviceInfo = (DeviceInfo) deviceCacheProvider.get(deviceId);
            String version = deviceInfo.getSoftVersion();

            Map<Integer, List<QueryFrame>> fnQuery = (Map<Integer, List<QueryFrame>>) timerCacheProvider.get(version);
            for (Iterator<Integer> iter = fnQuery.keySet().iterator(); iter.hasNext(); ) {
                int fnCode = iter.next();

                List<QueryFrame> frameList = fnQuery.get(fnCode);
                if (frameList.size() < 1) {
                    continue;
                }

                for (QueryFrame frame : frameList) {
                    String qKey = frame.getSite() + ":" + frame.getCode() + ":" + frame.getStart();
                    long frequency = frame.getPointUnits().get(0).getFrequency();

                    if (onTime(deviceId, qKey, frequency)) {
                        SendMsg msg = buildMsg(deviceId, frame);
                        toSend(msg);
                    }
                }
            }
        }
    }

    /**
     * 参数同步
     *
     * @param deviceId
     * @param fnCode
     */
    public void synchronize(String deviceId, Integer fnCode) {
        DeviceInfo deviceInfo = (DeviceInfo) deviceCacheProvider.get(deviceId);
        String version = deviceInfo.getSoftVersion();

        Map<Integer, List<QueryFrame>> fnQuery = (Map<Integer, List<QueryFrame>>) timerCacheProvider.get(version);
        if (MapUtils.isNotEmpty(fnQuery) && fnQuery.containsKey(fnCode)) {
            List<QueryFrame> frameList = fnQuery.get(fnCode);
            for (QueryFrame frame : frameList) {
                SendMsg msg = buildMsg(deviceId, frame);
                toSend(msg);
            }
        }
    }

    private SendMsg buildMsg(String deviceId, QueryFrame queryFrame) {
        List<PointUnit> units = queryFrame.getPointUnits();
        int type = units.get(0).getType();

        int site = queryFrame.getSite();
        int code = queryFrame.getCode();
        int star = queryFrame.getStart();
        int count = queryFrame.getCount().get();

        List<PointUnit> unitList = queryFrame.getPointUnits();
        if (type == 5) {
            count = unitList.get(0).getPoints().length;
        }

        ByteBuf byteBuf = Unpooled.buffer(6);
        byteBuf.writeByte(site);
        byteBuf.writeByte(code);
        byteBuf.writeShort(star);
        byteBuf.writeShort(count);
        byte[] bytes = byteBuf.array();

        String key = site + ":" + code + ":" + star;

        SendMsg sendMsg = new SendMsg();
        sendMsg.setDeviceId(deviceId);
        sendMsg.setCmd(code);
        sendMsg.setBytes(bytes);
        // 0: 查询; 1: 设置
        sendMsg.setType(0);
        sendMsg.setKey(key);
        sendMsg.setUnitList(unitList);

        return sendMsg;
    }

    /**
     * 更新下发指令状态
     *
     * @param msg
     * @param result
     * @param replyMsg
     */
    public void updateLog(SendMsg msg, int result, String replyMsg) {
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
     * 校验查询频率
     *
     * @param qKey
     * @param interval
     * @return
     */
    private boolean onTime(String deviceId, String qKey, long interval) {
        if (sendCacheProvider.containsKey(deviceId)) {
            MsgMemory msgMemory = (MsgMemory) sendCacheProvider.get(deviceId);
            SendMsg msg = msgMemory.getMsgMap().get(qKey);
            if (msg == null) {

                return true;
            }

            if ((System.currentTimeMillis() - msg.getDateTime()) * 0.001 < interval) {

                return false;
            }
        }

        return true;
    }


    public void toSend(SendMsg sendMsg) {
        String deviceId = sendMsg.getDeviceId();

        MsgPool pool;
        if (!onlineCacheProvider.containsKey(deviceId) && singlePoolCache.containsKey(deviceId)){
            pool = (MsgPool) singlePoolCache.get(deviceId);
            pool.getKeyList().clear();
            pool.getMsgQueue().clear();

            return;
        }

        if (singlePoolCache.containsKey(deviceId)) {
            pool = (MsgPool) singlePoolCache.get(deviceId);
        } else {
            pool = new MsgPool();
            pool.setDeviceId(deviceId);
            singlePoolCache.put(deviceId, pool);
        }

        // 指令标记
        String key = sendMsg.getKey();
        // 指令类型
        int type = sendMsg.getType();

        // 过滤重复查询指令
        if (0 == type && pool.getKeyList().contains(key)) {
            // log.info("设备[{}, {}, {}]指令已存在消费队列!", deviceId, key, JacksonUtil.toJson(pool.getKeyList()));
            return;
        }

        // 设置指令优先执行
        if (1 == type) {
            pool.getMsgQueue().addFirst(sendMsg);
        } else {
            pool.getKeyList().add(key);
            pool.getMsgQueue().add(sendMsg);
        }
    }
}
