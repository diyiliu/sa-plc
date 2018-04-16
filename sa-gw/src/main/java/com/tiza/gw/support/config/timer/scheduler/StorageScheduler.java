package com.tiza.gw.support.config.timer.scheduler;

import com.tiza.gw.support.config.timer.SenderTimer;
import com.tiza.gw.support.model.QueryFrame;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

/**
 * Description: StorageScheduler
 * Author: DIYILIU
 * Update: 2018-02-28 13:26
 */

@Service
@EnableScheduling
public class StorageScheduler extends SenderTimer {

    public StorageScheduler(){
        period = 10;
        initialDay = 9;

        functionId = "3";
        queryFrame = new QueryFrame(2, 3, 0, 30 * 2);
    }
}
