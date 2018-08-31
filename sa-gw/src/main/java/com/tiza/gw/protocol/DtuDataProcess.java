package com.tiza.gw.protocol;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.model.Header;
import com.diyiliu.plugin.model.IDataProcess;
import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.JacksonUtil;
import com.diyiliu.plugin.util.SpringUtil;
import com.tiza.gw.support.client.KafkaClient;
import com.tiza.gw.support.dao.dto.*;
import com.tiza.gw.support.dao.jpa.DetailInfoJpa;
import com.tiza.gw.support.dao.jpa.FaultInfoJpa;
import com.tiza.gw.support.model.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Description: DtuDataProcess
 * Author: DIYILIU
 * Update: 2018-01-30 09:45
 */

@Slf4j
@Service
public class DtuDataProcess implements IDataProcess {

    @Resource
    private KafkaClient kafkaClient;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private DetailInfoJpa detailInfoJpa;

    @Resource
    private FaultInfoJpa faultInfoJpa;

    @Resource
    private ICache deviceCacheProvider;

    @Resource
    private ICache sendCacheProvider;

    @Resource
    private ICache faultCacheProvider;

    @Resource
    private ICache alarmCacheProvider;

    @Resource
    private ICache onlineCacheProvider;


    @Override
    public void init() {

    }

    @Override
    public Header dealHeader(byte[] bytes) {

        return null;
    }

    @Override
    public void parse(byte[] content, Header header) {
        DtuHeader dtuHeader = (DtuHeader) header;
        String deviceId = dtuHeader.getDeviceId();
        if (!deviceCacheProvider.containsKey(deviceId)) {

            log.warn("设备不存在[{}]!", deviceId);
            return;
        }
        DeviceInfo deviceInfo = (DeviceInfo) deviceCacheProvider.get(deviceId);
        long equipId = deviceInfo.getId();

        MsgMemory msgMemory = (MsgMemory) sendCacheProvider.get(deviceId);
        SendMsg sendMsg = msgMemory.getCurrent();
        if (1 == sendMsg.getResult()) {

            return;
        }
        sendMsg.setResult(1);

        // 加入历史下发缓存
        msgMemory.getMsgMap().put(sendMsg.getKey(), sendMsg);

        StoreGroup storeGroup = new StoreGroup();
        // 当前表
        Map summary = storeGroup.getSummary();
        // 字典表
        List<DetailInfo> detailList = storeGroup.getDetailList();

        boolean isOk = true;
        // 是否数字量
        boolean isDigital = false;

        List<PointUnit> unitList = sendMsg.getUnitList();
        ByteBuf buf = Unpooled.copiedBuffer(content);
        for (int i = 0; i < unitList.size(); i++) {
            PointUnit pointUnit = unitList.get(i);
            int type = pointUnit.getType();

            // 数字量单独处理
            if (5 == type) {
                isDigital = true;
                unpackUnit(content, pointUnit, summary, detailList);
                break;
            }

            // 只有dword是四个字节,其他(除数字量)均为二个字节
            int length = type == 4 ? 4 : 2;
            // 按字(两个字节)解析
            if (buf.readableBytes() >= length) {
                byte[] bytes = new byte[length];
                buf.readBytes(bytes);

                unpackUnit(bytes, pointUnit, summary, detailList);
            } else {
                log.error("字节长度不足, 数据解析异常!");
                isOk = false;
                break;
            }
        }

        // 上下行匹配
        if (isOk) {
            if (!isDigital && buf.readableBytes() > 0){
                log.info("数据解析不完整(非数字量包!)");
                return;
            }

            updateSummary(equipId, summary);
            updateDetail(deviceInfo, detailList);
        }else {
            log.error("数据异常!");
        }
    }

    @Override
    public byte[] pack(Header header, Object... argus) {
        return new byte[0];
    }


    /**
     * 设备离线
     *
     * @param deviceId
     */
    public void offline(String deviceId) {
        onlineCacheProvider.remove(deviceId);
        sendCacheProvider.remove(deviceId);
         /*
        ICache deviceCache = SpringUtil.getBean("deviceCacheProvider");
        if (deviceCache.containsKey(deviceId)) {
            DeviceInfo deviceInfo = (DeviceInfo) deviceCache.get(deviceId);
            String sql = "UPDATE equipment_info SET DtuStatus = 0 WHERE EquipmentId = " + deviceInfo.getId();
            jdbcTemplate.update(sql);
            log.warn("设备[{}]离线[{}]", deviceId, sql);
        }
        */
    }


    private void unpackUnit(byte[] bytes, PointUnit pointUnit, Map summary, List<DetailInfo> detailList) {
        int type = pointUnit.getType();
        // bit类型
        if (5 == type || 1 == type) {
            String binaryStr = CommonUtil.bytes2BinaryStr(bytes);

            int address = pointUnit.getAddress();
            int length = pointUnit.getTags().length;
            if (binaryStr.length() - length >= 0) {
                for (int i = 0; i < length; i++) {
                    PointInfo p = pointUnit.getPoints()[i];
                    String k = p.getTag();

                    int offset = (p.getAddress() - address) * 8;
                    int position = offset + p.getPosition();
                    String v = binaryStr.substring(position, position + 1);

                    // 当前表
                    if (p.getSaveType() == 1) {
                        String f = p.getField();
                        summary.put(f, v);
                    }

                    // 详情表
                    DetailInfo d = new DetailInfo();
                    d.setPointId(p.getId());
                    d.setTag(k);
                    d.setValue(v);
                    detailList.add(d);

                    // 故障点
                    if (p.getFaultId() != null && p.getFaultType() > 0) {
                        d.setFaultId(p.getFaultId());
                        d.setFaultType(p.getFaultType());
                    }
                }
            }

            return;
        }

        if (3 == type || 4 == type) {
            PointInfo p = pointUnit.getPoints()[0];
            int val = CommonUtil.byte2int(bytes);
            String v = Integer.toString(val);

            // 1:float;2:int;3:hex
            int dataType = p.getDataType();
            if (1 == dataType) {
                v = String.valueOf(Float.intBitsToFloat(val));
            } else if (3 == dataType) {
                v = String.format("%02X", val);
            }

            String k = p.getTag();
            // 当前表
            if (p.getSaveType() == 1) {
                String f = p.getField();
                summary.put(f, v);
            }

            // 详情表
            DetailInfo d = new DetailInfo();
            d.setPointId(p.getId());
            d.setTag(k);
            d.setValue(v);
            detailList.add(d);
        }
    }

    /**
     * 更新当前信息
     *
     * @param equipId
     * @param paramValues
     */
    private void updateSummary(long equipId, Map paramValues) {
        List list = new ArrayList();
        StringBuilder sqlBuilder = new StringBuilder("UPDATE equipment_info SET ");

        if (MapUtils.isNotEmpty(paramValues)) {
            for (Iterator iterator = paramValues.keySet().iterator(); iterator.hasNext(); ) {
                String key = (String) iterator.next();

                sqlBuilder.append(key).append("=?, ");
                Object val = paramValues.get(key);
                list.add(val);
            }
        }

        // 最新时间
        sqlBuilder.append("lastTime").append("=?, ");
        list.add(new Date());

        log.info("[更新] 设备[{}]状态...", equipId);
        String sql = sqlBuilder.substring(0, sqlBuilder.length() - 2) + " WHERE equipmentId=" + equipId;
        jdbcTemplate.update(sql, list.toArray());
    }

    /**
     * 更新详细信息
     *
     * @param deviceInfo
     * @param detailInfoList
     */
    private void updateDetail(DeviceInfo deviceInfo, List<DetailInfo> detailInfoList) {
        if (CollectionUtils.isNotEmpty(detailInfoList)) {
            long equipId = deviceInfo.getId();

            for (DetailInfo detailInfo : detailInfoList) {
                detailInfo.setEquipId(equipId);
                detailInfo.setLastTime(new Date());

                // 处理故障报警
                if (detailInfo.getFaultId() != null) {
                    dealFault(detailInfo);
                }
            }

            // 批量更新数据
            detailInfoJpa.saveAll(detailInfoList);

            // 写入kafka
            kafkaClient.toKafka(equipId, detailInfoList);

            // 处理自定义报警
            doAlarm(deviceInfo, detailInfoList);
        }
    }

    /**
     * 故障处理
     *
     * @param detailInfo
     */
    private void dealFault(DetailInfo detailInfo) {
        Long key = detailInfo.getEquipId();
        int value = Integer.parseInt(detailInfo.getValue());

        synchronized (key) {
            List<FaultInfo> list = (List<FaultInfo>) faultCacheProvider.get(key);

            boolean flag = false;
            FaultInfo currentFault = null;
            if (CollectionUtils.isNotEmpty(list)) {
                for (FaultInfo info : list) {
                    if (info.getTag().equals(detailInfo.getTag())) {
                        flag = true;
                        currentFault = info;
                        break;
                    }
                }
            }

            // 产生报警
            if (1 == value && !flag) {
                FaultInfo faultInfo = new FaultInfo();
                faultInfo.setEquipId(detailInfo.getEquipId());
                faultInfo.setFaultId(detailInfo.getFaultId());
                faultInfo.setPointId(detailInfo.getPointId());
                faultInfo.setTag(detailInfo.getTag());
                faultInfo.setValue(detailInfo.getValue());
                faultInfo.setStartTime(new Date());
                faultInfo.setFaultType(detailInfo.getFaultType());
                faultInfo.setAlarmType(1);

                faultInfo = faultInfoJpa.save(faultInfo);
                if (faultInfo != null) {
                    if (CollectionUtils.isEmpty(list)) {
                        list = new ArrayList();
                        list.add(faultInfo);
                        faultCacheProvider.put(key, list);
                    } else {
                        list.add(faultInfo);
                    }

                    // 更新当前状态
                    updateAlarm(list);
                }

                return;
            }

            // 解除报警
            if (0 == value && flag) {
                long id = currentFault.getId();
                FaultInfo faultInfo = faultInfoJpa.findById(id);
                faultInfo.setEndTime(new Date());

                faultInfo = faultInfoJpa.save(faultInfo);
                if (faultInfo != null) {
                    list.removeIf(f -> f.getTag().equals(detailInfo.getTag()));

                    // 更新当前状态
                    if (CollectionUtils.isEmpty(list)) {
                        String sql = "UPDATE equipment_info SET HardAlarmStatus = 0 where equipmentId = " + key;
                        jdbcTemplate.update(sql);
                    } else {
                        updateAlarm(list);
                    }
                }
            }
        }
    }


    /**
     * 更新当前报警等级
     *
     * @param list
     */
    private void updateAlarm(List<FaultInfo> list) {
        list = list.stream().sorted(Comparator.comparing(FaultInfo::getFaultType)).collect(Collectors.toList());
        FaultInfo faultInfo = list.get(0);
        String sql = "UPDATE equipment_info SET HardAlarmStatus = " + faultInfo.getFaultType() + " where equipmentId = " + faultInfo.getEquipId();
        jdbcTemplate.update(sql);

        System.out.println(JacksonUtil.toJson(list));
        System.out.println(sql);
    }


    public void doAlarm(DeviceInfo deviceInfo, List<DetailInfo> detailInfoList) {
        List<AlarmInfo> alarmInfoList = deviceInfo.getAlarmInfoList();
        if (CollectionUtils.isEmpty(alarmInfoList)) {

            return;
        }

        for (DetailInfo detailInfo : detailInfoList) {
            long pointId = detailInfo.getPointId();
            String value = detailInfo.getValue();

            for (AlarmInfo alarmInfo : alarmInfoList) {

                List<Long> pointIds = alarmInfo.getPointIds();
                if (pointIds.contains(pointId)) {
                    int i = pointIds.indexOf(pointId);
                    AlarmDetail alarmDetail = alarmInfo.getAlarmDetails().get(i);
                    boolean flag = executeScript(alarmDetail.getExpression(), value);

                    dealAlarm(deviceInfo, alarmInfo, alarmDetail, flag);
                }
            }
        }
    }

    private void dealAlarm(DeviceInfo deviceInfo, AlarmInfo alarmInfo, AlarmDetail alarmDetail, boolean flag) {
        long equipId = deviceInfo.getId();
        String key = equipId + ":" + alarmInfo.getId();

        long detailId = alarmDetail.getId();
        if (alarmCacheProvider.containsKey(key)) {
            AlarmGroup alarmGroup = (AlarmGroup) alarmCacheProvider.get(key);
            Map<Long, AlarmItem> itemMap = alarmGroup.getItemMap();

            // 是否报警
            if (flag) {
                if (!itemMap.containsKey(detailId)) {
                    AlarmItem item = new AlarmItem();
                    item.setId(detailId);
                    item.setDuration(alarmDetail.getDuration());
                    item.setStartTime(System.currentTimeMillis());

                    itemMap.put(detailId, item);
                }

                // 报警处理
                updateAlarm(deviceInfo, alarmInfo, alarmGroup, 1);
            }
            // 解除报警
            else {
                if (itemMap.containsKey(detailId) && alarmGroup.getStartTime() != null) {

                    updateAlarm(deviceInfo, alarmInfo, alarmGroup, 0);
                }
            }

        } else {
            if (flag) {
                AlarmItem item = new AlarmItem();
                item.setId(detailId);
                item.setDuration(alarmDetail.getDuration());
                item.setStartTime(System.currentTimeMillis());

                AlarmGroup alarmGroup = new AlarmGroup();
                alarmGroup.setItemMap(new HashMap() {
                    {
                        this.put(detailId, item);
                    }
                });

                alarmCacheProvider.put(key, alarmGroup);
            }
        }
    }

    private void updateAlarm(DeviceInfo deviceInfo, AlarmInfo alarmInfo, AlarmGroup alarmGroup, int result) {
        // 产生报警
        if (result == 1) {
            Map<Long, AlarmItem> itemMap = alarmGroup.getItemMap();

            boolean alarm = true;
            Set<Long> set = itemMap.keySet();
            for (Iterator<Long> iterator = set.iterator(); iterator.hasNext(); ) {
                long key = iterator.next();
                AlarmItem item = itemMap.get(key);
                if (System.currentTimeMillis() - item.getStartTime() > item.getDuration() * 1000) {
                    item.setStatus(1);
                } else {
                    alarm = false;
                }
            }

            if (alarm && alarmGroup.getStartTime() == null
                    && alarmInfo.getAlarmDetails().size() == itemMap.size()) {

                FaultInfo faultInfo = new FaultInfo();
                faultInfo.setFaultId(alarmInfo.getFaultId());
                faultInfo.setEquipId(deviceInfo.getId());
                faultInfo.setStartTime(new Date());
                faultInfo.setAlarmType(2);
                faultInfo.setAlarmPolicyId(alarmInfo.getId());

                faultInfo = faultInfoJpa.save(faultInfo);
                if (faultInfo != null) {
                    log.info("产生报警[{}, {}]", deviceInfo.getId(), faultInfo.getAlarmPolicyId());
                    alarmGroup.setStartTime(new Date());
                    alarmGroup.setId(faultInfo.getId());
                }
            }

            return;
        }
        // 解除报警
        if (result == 0 && alarmGroup.getId() != null) {
            long equipId = deviceInfo.getId();
            String key = equipId + ":" + alarmInfo.getId();

            long fId = alarmGroup.getId();

            FaultInfo faultInfo = faultInfoJpa.findById(fId);
            faultInfo.setEndTime(new Date());
            faultInfo = faultInfoJpa.save(faultInfo);
            if (faultInfo != null) {
                log.info("解除报警[{}, {}]", deviceInfo.getId(), faultInfo.getAlarmPolicyId());
                alarmCacheProvider.remove(key);
            }
        }
    }

    private boolean executeScript(String script, String value) {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");

        SimpleBindings bindings = new SimpleBindings();
        bindings.put("$value", value);
        try {
            Object object = engine.eval(script, bindings);
            if (object == null || !(object instanceof Boolean)) {
                return false;
            } else {
                return (boolean) object;
            }
        } catch (ScriptException e) {
            e.printStackTrace();
            log.error("解析表达式错误[{}, {}]", script, e.getMessage());
        }

        return false;
    }
}
