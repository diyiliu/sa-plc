package com.tiza.gw.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.tiza.gw.support.model.MsgMemory;
import com.tiza.gw.support.model.PointUnit;
import com.tiza.gw.support.model.QueryFrame;
import com.tiza.gw.support.model.SendMsg;
import com.tiza.gw.support.dao.dto.DeviceInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.collections.MapUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 定时查询任务
 * <p>
 * Description: TimerTask
 * Author: DIYILIU
 * Update: 2018-04-25 14:03
 */

public class TimerTask implements ITask {

    /**
     * 设备缓存
     */
    private ICache deviceCache;

    /**
     * 在线设备
     */
    private ICache onlineCache;

    /**
     * 功能集定时任务
     */
    private ICache timerCache;

    /**
     * 发送缓存
     */
    private ICache sendCache;

    public TimerTask(ICache onlineCache, ICache deviceCache, ICache timerCache, ICache sendCache) {
        this.onlineCache = onlineCache;
        this.deviceCache = deviceCache;
        this.timerCache = timerCache;
        this.sendCache = sendCache;
    }

    @Override
    public void execute() {
        Set set = onlineCache.getKeys();
        for (Iterator iterator = set.iterator(); iterator.hasNext(); ) {
            String deviceId = (String) iterator.next();

            DeviceInfo deviceInfo = (DeviceInfo) deviceCache.get(deviceId);
            String version = deviceInfo.getSoftVersion();

            Map<Integer, List<QueryFrame>> fnQuery = (Map<Integer, List<QueryFrame>>) timerCache.get(version);
            for (Iterator<Integer> iter = fnQuery.keySet().iterator(); iter.hasNext(); ) {
                int fnCode = iter.next();

                List<QueryFrame> frameList = fnQuery.get(fnCode);
                if (frameList.size() < 1) {
                    continue;
                }

                for (QueryFrame frame : frameList) {
                    String qKey = frame.getSite() + ":" + frame.getCode() + ":" + frame.getStart();
                    int frequency = frame.getPointUnits().get(0).getFrequency();
                    if (onTime(deviceId, qKey, frequency)) {
                        SendMsg msg = toSendMsg(deviceId, frame);
                        SenderTask.send(msg);
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
        DeviceInfo deviceInfo = (DeviceInfo) deviceCache.get(deviceId);
        String version = deviceInfo.getSoftVersion();

        Map<Integer, List<QueryFrame>> fnQuery = (Map<Integer, List<QueryFrame>>) timerCache.get(version);
        if (MapUtils.isNotEmpty(fnQuery) && fnQuery.containsKey(fnCode)) {
            List<QueryFrame> frameList = fnQuery.get(fnCode);
            for (QueryFrame frame : frameList) {
                SendMsg msg = toSendMsg(deviceId, frame);
                SenderTask.send(msg);
            }
        }
    }


    private SendMsg toSendMsg(String deviceId, QueryFrame queryFrame) {
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
     * 是否到下发时间
     *
     * @param qKey
     * @param interval
     * @return
     */
    private boolean onTime(String deviceId, String qKey, int interval) {
        if (sendCache.containsKey(deviceId)) {
            MsgMemory msgMemory = (MsgMemory) sendCache.get(deviceId);
            SendMsg msg = msgMemory.getMsgMap().get(qKey);
            if (msg == null) {

                return true;
            }

            if (System.currentTimeMillis() - msg.getDatetime() < interval * 1000) {

                return false;
            }
        }

        return true;
    }
}
