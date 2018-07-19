package com.tiza.gw.support.config;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.tiza.gw.support.client.HBaseClient;
import com.tiza.gw.support.dao.dto.DailyHour;
import com.tiza.gw.support.dao.dto.DeviceInfo;
import com.tiza.gw.support.dao.jpa.*;
import com.tiza.gw.support.model.PointUnit;
import com.tiza.gw.support.model.QueryFrame;
import com.tiza.gw.support.task.*;
import com.tiza.gw.support.task.TimerTask;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Description: SpringQuartz
 * Author: DIYILIU
 * Update: 2018-01-30 10:41
 */

@EnableScheduling
@Configuration
public class SpringQuartz {

    @Resource
    private ICache onlineCacheProvider;

    @Resource
    private ICache deviceCacheProvider;

    @Resource
    private ICache sendCacheProvider;

    @Resource
    private ICache timerCacheProvider;

    @Resource
    private ICache readFnCacheProvider;

    @Resource
    private ICache writeFnCacheProvider;

    @Resource
    private DeviceInfoJpa deviceInfoJpa;

    @Resource
    private PointInfoJpa pointInfoJpa;

    @Resource
    private DailyHourJpa dailyHourJpa;

    @Resource
    private HBaseClient hbaseClient;

    @Resource
    private MaintainInfoJpa maintainInfoJpa;

    @Resource
    private MaintainRemindJpa maintainRemindJpa;

    @Resource
    private MaintainLogJpa maintainLogJpa;

    @Resource
    private DeviceCurrentStatusJpa deviceCurrentStatusJpa;


    @Scheduled(cron = "0 5 0 * * ?")
    //@Scheduled(fixedDelay = 60 * 1000, initialDelay = 5 * 1000)
    public void maintainTask() {
        MaintainTask mtTask = new MaintainTask();
        mtTask.setDeviceCache(deviceCacheProvider);
        mtTask.setMaintainInfoJpa(maintainInfoJpa);
        mtTask.setMaintainLogJpa(maintainLogJpa);
        mtTask.setMaintainRemindJpa(maintainRemindJpa);
        mtTask.setDeviceCurrentStatusJpa(deviceCurrentStatusJpa);

        mtTask.execute();
    }

    /**
     * 指令下发
     */
    @Scheduled(fixedDelay = 1000, initialDelay = 5 * 1000)
    public void sendTask() {
        ITask task = new SenderTask(onlineCacheProvider, sendCacheProvider);
        task.execute();
    }

    /**
     * 定时生成指令
     */
    @Scheduled(fixedDelay = 5 * 1000, initialDelay = 10 * 1000)
    public void timerTask() {
        ITask task = new TimerTask(onlineCacheProvider, deviceCacheProvider, timerCacheProvider, sendCacheProvider);
        task.execute();
    }

    /**
     * 刷新设备列表
     */
    @Scheduled(fixedDelay = 10 * 60 * 1000, initialDelay = 3 * 1000)
    public void refreshTaskDeviceInfo() {
        ITask task = new DeviceInfoTask(deviceInfoJpa, deviceCacheProvider);
        task.execute();
    }

    /**
     * 刷新功能集列表
     *
     * @return
     */
    @Scheduled(fixedDelay = 15 * 60 * 1000, initialDelay = 5 * 1000)
    public void functionSetTask() {
        ITask task = new FunctionTask(pointInfoJpa, readFnCacheProvider, writeFnCacheProvider);
        task.execute();

        Set fnSet = readFnCacheProvider.getKeys();
        Set tempKeys = new HashSet();

        fnSet.stream().forEach(e -> {
            String version = (String) e;
            List<PointUnit> readFnList = (List<PointUnit>) readFnCacheProvider.get(version);
            Map<Integer, List<PointUnit>> fnMap = readFnList.stream().collect(Collectors.groupingBy(PointUnit::getReadFunction));

            Map<Integer, List<QueryFrame>> queryMap = new HashMap();
            for (Iterator<Integer> iterator = fnMap.keySet().iterator(); iterator.hasNext(); ) {
                int fnCode = iterator.next();

                List<PointUnit> unitList = fnMap.get(fnCode);
                List<QueryFrame> queryFrames = combineUnit(unitList);
                queryMap.put(fnCode, queryFrames);
            }

            timerCacheProvider.put(version, queryMap);
            tempKeys.add(version);
        });

        // 删除过期功能集
        Collection subKeys = CollectionUtils.subtract(fnSet, tempKeys);
        for (Iterator iterator = subKeys.iterator(); iterator.hasNext(); ) {
            String key = (String) iterator.next();
            timerCacheProvider.remove(key);
        }

        /*
        // 删除过期定时消息(更新的功能集和删除的功能集)
        Collection unionKeys = CollectionUtils.union(tempKeys, subKeys);
        Set deviceSet = deviceCacheProvider.getKeys();
        for (Iterator iterator = deviceSet.iterator(); iterator.hasNext(); ) {
            String key = (String) iterator.next();
            DeviceInfo deviceInfo = (DeviceInfo) deviceCacheProvider.get(key);

            String deviceId = deviceInfo.getDtuId();
            if (unionKeys.contains(deviceInfo.getSoftVersion())) {
                if (sendCacheProvider.containsKey(deviceId)) {
                    MsgMemory msgMemory = (MsgMemory) sendCacheProvider.get(deviceId);
                    msgMemory.getMsgMap().clear();
                }
            }
        }
        */
    }

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
                double hour = max - last;

                DailyHour dailyHour = new DailyHour();
                dailyHour.setEquipId(id);
                dailyHour.setDay(new Date(startTime));
                dailyHour.setCreateTime(new Date());
                dailyHour.setHour(hour > 24 ? 0 : hour);
                dailyHour.setTotalHour(max);

                dailyHourJpa.save(dailyHour);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * 组合同功能码、同频率下发指令
     *
     * @param list
     * @return
     */
    private List<QueryFrame> combineUnit(List<PointUnit> list) {
        List<QueryFrame> queryFrames = new ArrayList();

        PointUnit last = list.get(0);

        QueryFrame queryFrame = new QueryFrame();
        queryFrame.setSite(last.getSiteId());
        queryFrame.setCode(last.getReadFunction());
        queryFrame.setStart(last.getAddress());
        queryFrame.addCount(last.getType() == 4 ? 2 : 1);
        last.setQueryFrame(queryFrame);

        List<PointUnit> pointUnits = new ArrayList();
        pointUnits.add(last);
        queryFrame.setPointUnits(pointUnits);
        queryFrames.add(queryFrame);
        for (int i = 1; i < list.size(); i++) {
            PointUnit unit = list.get(i);
            // 类型(1:bit;2:byte;3:word;4:dword;5:digital)
            int type = unit.getType();
            long frequency = unit.getFrequency();
            int address = unit.getAddress();

            boolean newQuery = true;
            if (frequency == last.getFrequency()) {
                int gap = address - last.getAddress();

                // 是否连续
                if (gap == 0 || (gap == 1 && last.getType() != 4)
                        || (gap == 2 && last.getType() == 4)) {
                    QueryFrame query = last.getQueryFrame();
                    if ((type != 4 && query.getCount().get() + 1 <= 60)
                            || (type == 4 && query.getCount().get() + 2 <= 60)) {

                        if (type == 4) {
                            query.addCount(2);
                        } else {
                            query.addCount();
                        }

                        query.getPointUnits().add(unit);
                        unit.setQueryFrame(query);
                        newQuery = false;
                    }
                }
            }

            if (newQuery) {
                QueryFrame query = new QueryFrame();
                query.setSite(unit.getSiteId());
                query.setCode(unit.getReadFunction());
                query.setStart(unit.getAddress());
                query.addCount(unit.getType() == 4 ? 2 : 1);
                unit.setQueryFrame(query);

                List<PointUnit> units = new ArrayList();
                units.add(unit);
                query.setPointUnits(units);
                queryFrames.add(query);
            }

            last = unit;
        }

        return queryFrames;
    }
}
