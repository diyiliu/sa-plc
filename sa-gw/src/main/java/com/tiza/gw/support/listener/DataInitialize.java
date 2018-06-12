package com.tiza.gw.support.listener;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.util.SpringUtil;
import com.tiza.gw.support.dao.dto.FaultInfo;
import com.tiza.gw.support.dao.jpa.FaultInfoJpa;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Description: DataInitialize
 * Author: DIYILIU
 * Update: 2018-05-28 10:20
 */

@Slf4j
@Component
public class DataInitialize implements ApplicationListener {

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ApplicationReadyEvent){

            synchRedis();
        }
    }

    /**
     * 同步故障缓存
     */
    private void synchRedis(){
        ICache faultCache = SpringUtil.getBean("faultCacheProvider");
        FaultInfoJpa faultInfoJpa = SpringUtil.getBean("faultInfoJpa");

        List<FaultInfo> faultInfoList = faultInfoJpa.findByEndTimeIsNullOrEndTimeBeforeStartTime();
        for (FaultInfo faultInfo: faultInfoList){
            String key = faultInfo.getEquipId() + ":" + faultInfo.getTag();
            faultCache.put(key, faultInfo.getId());
        }
    }
}
