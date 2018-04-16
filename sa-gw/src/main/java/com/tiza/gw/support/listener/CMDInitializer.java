package com.tiza.gw.support.listener;

import com.diyiliu.plugin.model.IDataProcess;
import com.diyiliu.plugin.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Description: CMDInitializer
 * Author: DIYILIU
 * Update: 2017-08-04 14:10
 */

@Slf4j
public class CMDInitializer implements ApplicationListener {
    private List<Class> protocols;

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationReadyEvent){
            log.info("协议解析初始化...");
            for (Class protocol : protocols) {
                Map parses = SpringUtil.getBeansOfType(protocol);

                for (Iterator iterator = parses.keySet().iterator(); iterator.hasNext(); ) {
                    String key = (String) iterator.next();
                    IDataProcess process = (IDataProcess) parses.get(key);
                    process.init();
                }
            }
        }
    }

    public void setProtocols(List<Class> protocols) {
        this.protocols = protocols;
    }
}
