package com.tiza.gw.protocol;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.model.Header;
import com.diyiliu.plugin.model.IDataProcess;
import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.JacksonUtil;
import com.tiza.gw.support.client.KafkaClient;
import com.tiza.gw.support.dao.dto.DetailInfo;
import com.tiza.gw.support.dao.dto.DeviceInfo;
import com.tiza.gw.support.dao.dto.FaultInfo;
import com.tiza.gw.support.dao.dto.PointInfo;
import com.tiza.gw.support.dao.jpa.DetailInfoJpa;
import com.tiza.gw.support.dao.jpa.FaultInfoJpa;
import com.tiza.gw.support.model.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.kafka.common.security.JaasUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
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
    private ICache deviceCacheProvider;

    @Resource
    private ICache sendCacheProvider;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private KafkaClient kafkaClient;

    @Resource
    private DetailInfoJpa detailInfoJpa;

    @Resource
    private FaultInfoJpa faultInfoJpa;

    @Resource
    private ICache faultCacheProvider;

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

        List<PointUnit> unitList = sendMsg.getUnitList();
        ByteBuf buf = Unpooled.copiedBuffer(content);
        for (int i = 0; i < unitList.size(); i++) {
            PointUnit pointUnit = unitList.get(i);
            int type = pointUnit.getType();

            // 数字量单独处理
            if (5 == type) {

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
            }
        }

        updateSummary(equipId, summary);
        updateDetail(equipId, detailList);
    }

    @Override
    public byte[] pack(Header header, Object... argus) {
        return new byte[0];
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
        if (MapUtils.isEmpty(paramValues)) {
            return;
        }

        List list = new ArrayList();
        StringBuilder sqlBuilder = new StringBuilder("UPDATE equipment_info SET ");
        for (Iterator iterator = paramValues.keySet().iterator(); iterator.hasNext(); ) {
            String key = (String) iterator.next();

            sqlBuilder.append(key).append("=?, ");
            Object val = paramValues.get(key);
            list.add(val);
        }

        // 最新时间
        sqlBuilder.append("lastTime").append("=?, ");
        list.add(new Date());

        log.info("[更新] 设备[{}]状态...", equipId);
        String sql = sqlBuilder.substring(0, sqlBuilder.length() - 2) + " WHERE equipmentId=" + equipId;
        jdbcTemplate.update(sql, list.toArray());
    }

    /**
     * 更新详细新
     *
     * @param equipId
     * @param detailInfoList
     */
    public void updateDetail(long equipId, List<DetailInfo> detailInfoList) {
        if (CollectionUtils.isNotEmpty(detailInfoList)) {
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
        }
    }

    /**
     * 故障处理
     *
     * @param detailInfo
     */
    public void dealFault(DetailInfo detailInfo) {
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
}
