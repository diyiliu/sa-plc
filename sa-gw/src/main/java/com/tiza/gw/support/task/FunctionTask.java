package com.tiza.gw.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.tiza.gw.support.dao.jpa.PointInfoJpa;
import com.tiza.gw.support.model.PointUnit;
import com.tiza.gw.support.dao.dto.PointInfo;
import com.tiza.gw.support.model.QueryFrame;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Description: FunctionSetTask
 * Author: DIYILIU
 * Update: 2018-02-06 09:15
 */

@Slf4j
@Service
public class FunctionTask implements ITask {

    @Resource
    private PointInfoJpa pointInfoJpa;

    @Resource
    private ICache readFnCacheProvider;

    @Resource
    private ICache writeFnCacheProvider;

    @Resource
    private ICache timerCacheProvider;


    @Scheduled(fixedRate = 15 * 60 * 1000, initialDelay = 5 * 1000)
    public void execute() {
        log.info("刷新功能集列表...");
        try {
            List<PointInfo> infoList = pointInfoJpa.findAll(Sort.by(new String[]{"versionId", "siteId", "address", "position"}));

            refresh(infoList, readFnCacheProvider, writeFnCacheProvider);
        } catch (Exception e) {
            e.printStackTrace();
        }


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

    public void refresh(List<PointInfo> infoList, ICache readFnCache, ICache writeFnCache) {
        // 按软件版本号分组
        Map<String, List<PointInfo>> versionMap = infoList.stream().collect(Collectors.groupingBy(PointInfo::getVersionId));

        Set oldReadKeys = readFnCache.getKeys();
        Set oldWriteKeys = writeFnCache.getKeys();

        Set tempReadKeys = new HashSet();
        Set tempWriteKeys = new HashSet();
        for (Iterator<String> iterator = versionMap.keySet().iterator(); iterator.hasNext(); ) {
            String version = iterator.next();
            List<PointInfo> pointInfoList = versionMap.get(version);

            // 构造功能集
            List<PointUnit> pointUnitList = buildUnit(pointInfoList);

            List<PointUnit> readUnitList = pointUnitList.stream()
                    .filter(unit -> (1 == unit.getReadWrite() || 3 == unit.getReadWrite())).collect(Collectors.toList());

            List<PointUnit> writeUnitList = pointUnitList.stream()
                    .filter(unit -> (2 == unit.getReadWrite() || 3 == unit.getReadWrite())).collect(Collectors.toList());

            if (readUnitList.size() > 0) {
                readFnCache.put(version, readUnitList);
                tempReadKeys.add(version);
            }

            if (pointUnitList.size() > 0) {
                writeFnCache.put(version, writeUnitList);
                tempWriteKeys.add(version);
            }
        }

        // 删除冗余缓存
        Collection subReadKeys = CollectionUtils.subtract(oldReadKeys, tempReadKeys);
        for (Iterator iterator = subReadKeys.iterator(); iterator.hasNext(); ) {
            String key = (String) iterator.next();
            readFnCache.remove(key);
        }

        Collection subWriteKeys = CollectionUtils.subtract(oldWriteKeys, tempWriteKeys);
        for (Iterator iterator = subWriteKeys.iterator(); iterator.hasNext(); ) {
            String key = (String) iterator.next();
            writeFnCache.remove(key);
        }
    }

    public List<PointUnit> buildUnit(List<PointInfo> infoList) {
        Map<Integer, List<PointInfo>> typeMap = infoList.stream().collect(Collectors.groupingBy(PointInfo::getPointType));

        List<PointUnit> pointUnits = new ArrayList();
        for (Iterator<Integer> iterator = typeMap.keySet().iterator(); iterator.hasNext(); ) {
            int pointType = iterator.next();
            List<PointInfo> list = typeMap.get(pointType);

            // 数字量 按位偏移(bit)
            if (5 == pointType) {
                Map<Integer, List<PointInfo>> unitMap = list.stream().collect(Collectors.groupingBy(PointInfo::getReadFunction));
                for (Iterator<Integer> iter = unitMap.keySet().iterator(); iter.hasNext(); ) {
                    int fnCode = iter.next();
                    List<PointInfo> pList = unitMap.get(fnCode);

                    PointUnit pointUnit = new PointUnit();
                    renderFirst(pointUnit, pList);
                    String[] tags = new String[pList.size()];
                    PointInfo[] pointInfos = new PointInfo[pList.size()];
                    for (int i = 0; i < pList.size(); i++) {

                        PointInfo point = pList.get(i);
                        tags[i] = point.getTag();
                        pointInfos[i] = point;
                    }
                    pointUnit.setTags(tags);
                    pointUnit.setPoints(pointInfos);
                    pointUnits.add(pointUnit);
                }
            }

            // bit类型
            if (1 == pointType) {
                Map<Integer, List<PointInfo>> unitMap = list.stream().collect(Collectors.groupingBy(PointInfo::getAddress));

                // 按地址去重
                List<Integer> addressList = list.stream().map(PointInfo::getAddress).distinct().collect(Collectors.toList());
                for (Integer address : addressList) {
                    List<PointInfo> pList = unitMap.get(address);

                    PointUnit pointUnit = new PointUnit();
                    renderFirst(pointUnit, pList);

                    String[] tags = new String[pList.size()];
                    PointInfo[] points = new PointInfo[pList.size()];
                    for (int i = 0; i < pList.size(); i++) {

                        PointInfo point = pList.get(i);
                        tags[i] = point.getTag();
                        points[i] = point;
                    }
                    pointUnit.setTags(tags);
                    pointUnit.setPoints(points);

                    pointUnits.add(pointUnit);
                }
            }

            // 3:word;4:dword
            if (3 == pointType || 4 == pointType) {
                for (PointInfo pointInfo : list) {
                    PointUnit pointUnit = new PointUnit();
                    pointUnit.setType(pointType);

                    pointUnit.setReadWrite(pointInfo.getReadWrite());
                    pointUnit.setReadFunction(pointInfo.getReadFunction());
                    pointUnit.setWriteFunction(pointInfo.getWriteFunction());
                    pointUnit.setFrequency(pointInfo.getFrequency());
                    pointUnit.setSiteId(pointInfo.getSiteId());
                    pointUnit.setAddress(pointInfo.getAddress());
                    pointUnit.setTags(new String[]{pointInfo.getTag()});
                    pointUnit.setPoints(new PointInfo[]{pointInfo});

                    pointUnits.add(pointUnit);
                }
            }
        }

        return pointUnits;
    }


    /**
     * 依据队列第一个点
     *
     * @param pointUnit
     * @param list
     */
    private void renderFirst(PointUnit pointUnit, List<PointInfo> list) {
        PointInfo first = list.get(0);

        pointUnit.setType(first.getPointType());
        pointUnit.setReadWrite(first.getReadWrite());
        pointUnit.setReadFunction(first.getReadFunction());
        pointUnit.setWriteFunction(first.getWriteFunction());
        pointUnit.setSiteId(first.getSiteId());
        pointUnit.setAddress(first.getAddress());
        pointUnit.setFrequency(first.getFrequency());
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
