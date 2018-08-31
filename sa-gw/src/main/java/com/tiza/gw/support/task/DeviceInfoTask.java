package com.tiza.gw.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.tiza.gw.support.dao.jpa.DeviceInfoJpa;
import com.tiza.gw.support.dao.dto.DeviceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * Description: DeviceInfoTask
 * Author: DIYILIU
 * Update: 2018-01-30 11:07
 */

@Slf4j
@Service
public class DeviceInfoTask implements ITask {

    @Resource
    private ICache deviceCacheProvider;

    @Resource
    private DeviceInfoJpa deviceInfoJpa;


    @Scheduled(fixedRate = 10 * 60 * 1000, initialDelay = 3 * 1000)
    public void execute() {
        log.info("刷新设备列表...");
        List<DeviceInfo> list = deviceInfoJpa.findAll();
        refresh(list, deviceCacheProvider);
    }

    private void refresh(List<DeviceInfo> deviceList, ICache deviceCache) {
        if (deviceList == null || deviceList.size() < 1){
            log.warn("无设备!");
            return;
        }

        Set oldKeys = deviceCache.getKeys();
        Set tempKeys = new HashSet(deviceList.size());

        for (DeviceInfo device : deviceList) {
            deviceCache.put(device.getDtuId(), device);
            tempKeys.add(device.getDtuId());
        }

        Collection subKeys = CollectionUtils.subtract(oldKeys, tempKeys);
        for (Iterator iterator = subKeys.iterator(); iterator.hasNext();){
            String key = (String) iterator.next();
            deviceCache.remove(key);
        }
    }
}
