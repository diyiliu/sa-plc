package com.tiza.gw.support.controller;

import com.diyiliu.plugin.cache.ICache;
import com.tiza.gw.support.jpa.DeviceInfoJpa;
import com.tiza.gw.support.model.CanPackage;
import com.tiza.gw.support.model.NodeItem;
import com.tiza.gw.support.model.bean.DeviceInfo;
import com.tiza.gw.support.model.bean.FunctionInfo;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.Map;

/**
 * Description: SendController
 * Author: DIYILIU
 * Update: 2018-04-16 09:10
 */

@RestController
public class SendController {

    @Resource
    private ICache onlineCacheProvider;

    @Resource
    private ICache functionSetCacheProvider;


    @Resource
    private DeviceInfoJpa deviceInfoJpa;


    @RequestMapping("/setup")
    public String setup(@Param("key") String key, @Param("value") String value,
                        @Param("deviceId") String deviceId, HttpServletResponse response) {

        DeviceInfo deviceInfo = deviceInfoJpa.findById(Long.parseLong(deviceId));

        String dtuId = deviceInfo.getDtuId();
        if (!onlineCacheProvider.containsKey(dtuId)) {

            response.setStatus(500);
            return "设备离线。";
        }


        String softVersion = deviceInfo.getSoftVersion();
        if (!functionSetCacheProvider.containsKey(softVersion)) {

            response.setStatus(500);
            return "未配置设备功能集。";
        }

        FunctionInfo functionInfo = (FunctionInfo) functionSetCacheProvider.get(softVersion);


        return "";
    }

    public byte[] toSend(String key, String value, FunctionInfo functionInfo) {
        Map<String, CanPackage> packageMap = functionInfo.getCanPackages();

        NodeItem nodeItem = null;
        CanPackage canPackage = null;
        for (Iterator iterator = packageMap.keySet().iterator(); iterator.hasNext(); ) {
            String function = (String) iterator.next();

            canPackage = packageMap.get(function);
            nodeItem = canPackage.getItemList().stream().filter(item -> item.getTag().equals(key)).findFirst().get();
            if (nodeItem != null) {

                break;
            }
        }

        // 未找到节点
        if (nodeItem == null){


            return null;
        }

        int address = canPackage.getAddress();
        int offset = canPackage.getOffset();


        return null;
    }

}
