package com.tiza.gw.support.controller;

import com.diyiliu.plugin.cache.ICache;
import com.tiza.gw.support.jpa.DeviceInfoJpa;
import com.tiza.gw.support.model.bean.DeviceInfo;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

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
    private DeviceInfoJpa deviceInfoJpa;


    @PostMapping("/setup")
    public String setup(@Param("key") String key, @Param("value") String value,
                        @Param("id") String id, HttpServletResponse response) {

        DeviceInfo deviceInfo = deviceInfoJpa.findById(Long.parseLong(id));

        String dtuId = deviceInfo.getDtuId();
        if (!onlineCacheProvider.containsKey(dtuId)) {

            response.setStatus(500);
            return "设备离线。";
        }

/*        String softVersion = deviceInfo.getSoftVersion();
        if (!functionSetCacheProvider.containsKey(softVersion)) {

            response.setStatus(500);
            return "未配置设备功能集。";
        }

        FunctionInfo functionInfo = (FunctionInfo) functionSetCacheProvider.get(softVersion);

        int val;
        if (value.indexOf(".") > 0) {
            val = Float.floatToIntBits(Float.parseFloat(value));
        } else {
            val = Integer.parseInt(value);
        }

        SendMsg sendMsg = toSend(key, val, functionInfo);
        if (sendMsg == null) {

            response.setStatus(500);
            return "设备功能集异常。";
        }

        sendMsg.setDeviceId(dtuId);
        if (sendMsgCacheProvider.containsKey(dtuId)) {

            response.setStatus(500);
            return "数据等待中, 请稍后重试。";
        }

        ChannelHandlerContext context = (ChannelHandlerContext) onlineCacheProvider.get(dtuId);
        context.writeAndFlush(Unpooled.copiedBuffer(sendMsg.getBytes()));
        sendMsgCacheProvider.put(dtuId, sendMsg);*/

        return "设置成功。";
    }
}
