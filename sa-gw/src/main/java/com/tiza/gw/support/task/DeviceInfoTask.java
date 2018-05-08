package com.tiza.gw.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.tiza.gw.support.dao.jpa.DeviceInfoJpa;
import com.tiza.gw.support.dao.dto.DeviceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;

/**
 * Description: DeviceInfoTask
 * Author: DIYILIU
 * Update: 2018-01-30 11:07
 */

@Slf4j
public class DeviceInfoTask implements ITask {

    private ICache deviceCache;

    private DeviceInfoJpa deviceDao;

    public DeviceInfoTask(DeviceInfoJpa deviceDao, ICache deviceCache) {
        this.deviceDao = deviceDao;
        this.deviceCache = deviceCache;
    }

    @Override
    public void execute() {
        log.info("刷新设备列表...");
        List<DeviceInfo> list = deviceDao.findAll();
        refresh(list, deviceCache);
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
