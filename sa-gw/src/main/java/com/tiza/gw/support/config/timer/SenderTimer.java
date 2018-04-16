package com.tiza.gw.support.config.timer;

import com.diyiliu.plugin.cache.ICache;
import com.tiza.gw.support.model.QueryFrame;
import com.tiza.gw.support.task.AutoSenderTask;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.PeriodicTrigger;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * Description: SenderTimer
 * Author: DIYILIU
 * Update: 2018-02-28 11:31
 */

public class SenderTimer implements SchedulingConfigurer {

    // 功能集编号
    protected String functionId;

    // 执行频率
    protected long period = 10;

    // 延时启动
    protected long initialDay = 5;

    // 查询结构
    protected QueryFrame queryFrame;

    @Resource
    protected ICache onlineCacheProvider;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {

        taskRegistrar.addTriggerTask(new AutoSenderTask(queryFrame, onlineCacheProvider),
                (TriggerContext triggerContext) -> {
                    PeriodicTrigger trigger = new PeriodicTrigger(period, TimeUnit.SECONDS);
                    trigger.setInitialDelay(initialDay);
                    trigger.setFixedRate(true);

                    return trigger.nextExecutionTime(triggerContext);
                });
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    public String getFunctionId() {
        return functionId;
    }
}
