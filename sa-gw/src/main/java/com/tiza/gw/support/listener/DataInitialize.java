package com.tiza.gw.support.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * Description: DataInitialize
 * Author: DIYILIU
 * Update: 2018-05-28 10:20
 */

@Slf4j
public class DataInitialize implements ApplicationListener {

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ApplicationReadyEvent){



        }
    }
}
