package com.tiza.gw.support.config;

import com.diyiliu.plugin.cache.ICache;
import com.tiza.gw.support.client.HBaseClient;
import com.tiza.gw.support.dao.dto.DailyHour;
import com.tiza.gw.support.dao.dto.DeviceInfo;
import com.tiza.gw.support.dao.jpa.DailyHourJpa;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Description: SpringQuartz
 * Author: DIYILIU
 * Update: 2018-01-30 10:41
 */

@Configuration
@EnableScheduling
public class SpringQuartz {

    @Resource
    private ICache deviceCacheProvider;

    @Resource
    private DailyHourJpa dailyHourJpa;

    @Resource
    private HBaseClient hbaseClient;

    /**
     * 当日运行时长
     */
    @Scheduled(cron = "0 30 1 * * ?")
    public void dailyRunningJob() {
        Calendar today = Calendar.getInstance();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(0);
        calendar.set(Calendar.YEAR, today.get(Calendar.YEAR));
        calendar.set(Calendar.MONTH, today.get(Calendar.MONTH));
        calendar.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 0);

        long endTime = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        long startTime = calendar.getTimeInMillis();


        final String tag = "TotalRunTime";
        Set set = deviceCacheProvider.getKeys();
        set.forEach(key -> {
            DeviceInfo deviceInfo = (DeviceInfo) deviceCacheProvider.get(key);
            Long id = deviceInfo.getId();

            double last = 0;
            List<DailyHour> lastHours = dailyHourJpa.findByEquipId(id, Sort.by(Sort.Direction.DESC, new String[]{"day", "totalHour"}));
            if (CollectionUtils.isNotEmpty(lastHours)) {
                last = lastHours.get(0).getTotalHour();
            }

            try {
                List<String> values = hbaseClient.scan(id.intValue(), tag, startTime, endTime);
                // 当日最大
                double max;
                if (CollectionUtils.isEmpty(values)) {
                    max = last;
                } else {
                    max = Double.valueOf(values.get(values.size() - 1));
                }

                // 当日工作时间
                double hour = 0;
                if (last > 0 && max > last) {
                    hour = max - last;
                }

                DailyHour dailyHour = new DailyHour();
                dailyHour.setEquipId(id);
                dailyHour.setDay(new Date(startTime));
                dailyHour.setCreateTime(new Date());
                dailyHour.setHour(hour);
                dailyHour.setTotalHour(max);

                dailyHourJpa.save(dailyHour);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
