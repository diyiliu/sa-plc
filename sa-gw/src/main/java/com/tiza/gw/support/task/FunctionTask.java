package com.tiza.gw.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.tiza.gw.support.jpa.PointInfoJpa;
import com.tiza.gw.support.model.PointUnit;
import com.tiza.gw.support.model.bean.PointInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.domain.Sort;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Description: FunctionSetTask
 * Author: DIYILIU
 * Update: 2018-02-06 09:15
 */

@Slf4j
public class FunctionTask implements ITask {
    private PointInfoJpa pointInfoJpa;

    private ICache readFnCache;

    private ICache writeFnCache;

    public FunctionTask(PointInfoJpa pointInfoJpa, ICache readFnCache, ICache writeFnCache) {
        this.pointInfoJpa = pointInfoJpa;

        this.readFnCache = readFnCache;
        this.writeFnCache = writeFnCache;
    }

    @Override
    public void execute() {
        log.info("刷新功能集列表...");
        try {
            List<PointInfo> infoList = pointInfoJpa.findAll(Sort.by(new String[]{"versionId", "siteId", "address", "position"}));

            refresh(infoList, readFnCache, writeFnCache);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                    renderFirst(pointUnit, list);

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
}
