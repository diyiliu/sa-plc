package com.tiza.gw.support.aop;

import com.diyiliu.plugin.cache.ICache;
import com.tiza.gw.support.dao.dto.*;
import com.tiza.gw.support.dao.jpa.DeviceInfoJpa;
import com.tiza.gw.support.dao.jpa.FaultInfoJpa;
import com.tiza.gw.support.model.AlarmGroup;
import com.tiza.gw.support.model.AlarmItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.*;

/**
 * Description: DataProcessAspect
 * Author: DIYILIU
 * Update: 2018-07-10 09:49
 */

@Slf4j
@Aspect
@Component
public class DataProcessAspect {

    @Resource
    private DeviceInfoJpa deviceInfoJpa;

    @Resource
    private FaultInfoJpa faultInfoJpa;

    @Resource
    private ICache alarmCacheProvider;

    @After("execution(* com.tiza.gw.protocol.DtuDataProcess.updateDetail(..))")
    public void doAfter(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();

        long equipId = (long) args[0];
        DeviceInfo deviceInfo = deviceInfoJpa.findById(equipId);
        List<AlarmInfo> alarmInfoList = deviceInfo.getAlarmInfoList();
        if (CollectionUtils.isEmpty(alarmInfoList)) {

            return;
        }

        List<DetailInfo> detailInfoList = (List<DetailInfo>) args[1];
        for (DetailInfo detailInfo : detailInfoList) {

            long pointId = detailInfo.getPointId();
            String value = detailInfo.getValue();

            for (AlarmInfo alarmInfo : alarmInfoList) {

                List<Long> pointIds = alarmInfo.getPointIds();
                if (pointIds.contains(pointId)) {
                    int i = pointIds.indexOf(pointId);
                    AlarmDetail alarmDetail = alarmInfo.getAlarmDetails().get(i);
                    boolean flag = executeScript(alarmDetail.getExpression(), value);

                    dealAlarm(deviceInfo, alarmInfo, alarmDetail, flag);
                }
            }
        }
    }

    private void dealAlarm(DeviceInfo deviceInfo, AlarmInfo alarmInfo, AlarmDetail alarmDetail, boolean flag) {
        long equipId = deviceInfo.getId();
        String key = equipId + ":" + alarmInfo.getId();

        long detailId = alarmDetail.getId();
        if (alarmCacheProvider.containsKey(key)) {
            AlarmGroup alarmGroup = (AlarmGroup) alarmCacheProvider.get(key);
            Map<Long, AlarmItem> itemMap = alarmGroup.getItemMap();

            // 是否报警
            if (flag) {
                if (!itemMap.containsKey(detailId)) {

                    AlarmItem item = new AlarmItem();
                    item.setId(detailId);
                    item.setDuration(alarmDetail.getDuration());
                    item.setStartTime(System.currentTimeMillis());

                    itemMap.put(detailId, item);
                }

                // 报警处理
                updateAlarm(deviceInfo, alarmInfo, alarmGroup, 1);
            }
            // 解除报警
            else {
                if (itemMap.containsKey(detailId) && alarmGroup.getStartTime() != null) {

                    updateAlarm(deviceInfo, alarmInfo, alarmGroup, 0);
                }
            }

        } else {
            if (flag) {
                AlarmItem item = new AlarmItem();
                item.setId(detailId);
                item.setDuration(alarmDetail.getDuration());
                item.setStartTime(System.currentTimeMillis());

                AlarmGroup alarmGroup = new AlarmGroup();
                alarmGroup.setItemMap(new HashMap() {
                    {
                        this.put(detailId, item);
                    }
                });

                alarmCacheProvider.put(key, alarmGroup);
            }
        }
    }

    private void updateAlarm(DeviceInfo deviceInfo, AlarmInfo alarmInfo, AlarmGroup alarmGroup, int result) {

        // 产生报警
        if (result == 1) {
            Map<Long, AlarmItem> itemMap = alarmGroup.getItemMap();

            boolean alarm = true;
            Set<Long> set = itemMap.keySet();
            for (Iterator<Long> iterator = set.iterator(); iterator.hasNext(); ) {
                long key = iterator.next();
                AlarmItem item = itemMap.get(key);
                if (System.currentTimeMillis() - item.getStartTime() > item.getDuration() * 1000) {
                    item.setStatus(1);
                } else {
                    alarm = false;
                }
            }

            if (alarm && alarmInfo.getAlarmDetails().size() == itemMap.size()) {

                FaultInfo faultInfo = new FaultInfo();
                faultInfo.setEquipId(deviceInfo.getId());
                faultInfo.setStartTime(new Date());
                faultInfo.setAlarmType(2);
                faultInfo.setAlarmPolicyId(alarmInfo.getId());

                faultInfo = faultInfoJpa.save(faultInfo);
                if (faultInfo != null) {
                    alarmGroup.setStartTime(new Date());
                    alarmGroup.setId(faultInfo.getId());
                }
            }

            return;
        }
        // 解除报警
        if (result == 0 && alarmGroup.getId() != null) {
            long equipId = deviceInfo.getId();
            String key = equipId + ":" + alarmInfo.getId();

            long fId = alarmGroup.getId();

            FaultInfo faultInfo = faultInfoJpa.findById(fId);
            faultInfo.setEndTime(new Date());
            faultInfo = faultInfoJpa.save(faultInfo);
            if (faultInfo != null) {
                alarmCacheProvider.remove(key);
            }
        }
    }


    private boolean executeScript(String script, String value) {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");

        SimpleBindings bindings = new SimpleBindings();
        bindings.put("$value", value);


        try {
            return (boolean) engine.eval(script, bindings);
        } catch (ScriptException e) {
            e.printStackTrace();
            log.error("解析表达式错误[{}, {}]", script, e.getMessage());
        }

        return false;
    }

}
