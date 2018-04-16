package com.tiza.gw.support.controller;

import com.diyiliu.plugin.cache.ICache;
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
    private ICache deviceCacheProvider;

    @Resource
    private ICache functionSetCacheProvider;


    @PostMapping("/setup")
    public String setup(@Param("key") String key, @Param("value") String value,
                        @Param("deviceId") String deviceId, HttpServletResponse response) {



        return "";
    }
}
