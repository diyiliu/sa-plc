package com.tiza.gw.protocol;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.model.Header;
import com.diyiliu.plugin.model.IDataProcess;
import com.diyiliu.plugin.util.CommonUtil;
import com.tiza.gw.support.client.KafkaClient;
import com.tiza.gw.support.model.CanPackage;
import com.tiza.gw.support.model.DtuHeader;
import com.tiza.gw.support.model.NodeItem;
import com.tiza.gw.support.model.bean.DeviceInfo;
import com.tiza.gw.support.model.bean.FunctionInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.script.ScriptException;
import java.util.*;

/**
 * Description: DtuDataProcess
 * Author: DIYILIU
 * Update: 2018-01-30 09:45
 */

@Slf4j
@Service
public class DtuDataProcess implements IDataProcess {
    protected int cmd = 0xFF;

    @Resource
    private ICache dtuCMDCacheProvider;

    @Resource
    private ICache deviceCacheProvider;

    @Resource
    private ICache functionSetCacheProvider;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private KafkaClient kafkaClient;

    @Override
    public void init() {
        dtuCMDCacheProvider.put(this.cmd, this);
    }

    @Override
    public Header dealHeader(byte[] bytes) {

        return null;
    }

    @Override
    public void parse(byte[] content, Header header) {
        DtuHeader dtuHeader = (DtuHeader) header;
        String deviceId = dtuHeader.getDeviceId();
        if (!deviceCacheProvider.containsKey(deviceId)){

            log.warn("设备不存在[{}]!", deviceId);
            return;
        }

        DeviceInfo deviceInfo = (DeviceInfo) deviceCacheProvider.get(deviceId);
        if (!functionSetCacheProvider.containsKey(deviceInfo.getSoftVersion())){

            log.warn("未配置的功能集[{}]", deviceInfo.getSoftVersion());
            return;
        }

        String canCode = String.valueOf(dtuHeader.getCode());
        FunctionInfo functionInfo = (FunctionInfo) functionSetCacheProvider.get(deviceInfo.getSoftVersion());
        CanPackage canPackage = functionInfo.getCanPackages().get(canCode);
        if (canPackage == null){

            log.warn("未配置的功能码[{}]", canCode);
            return;
        }

        Map paramValues = parsePackage(content, canPackage.getItemList());
        updateStatus(dtuHeader, paramValues);
    }

    @Override
    public byte[] pack(Header header, Object... argus) {
        return new byte[0];
    }


    /**
     * 按字节解析
     * @param b
     * @param items
     * @return
     */
    protected Map parseByte(byte b, String[] items){
        Map map = new HashMap();
        for (int i = 0; i < items.length; i++){

            int value = (b >> i) & 0x01;
            map.put(items[i], value);
        }

        return map;
    }

    protected Map parsePackage(byte[] content, List<NodeItem> nodeItems) {
        Map packageValues = new HashMap();

        for (NodeItem item : nodeItems) {
            try {
                packageValues.put(item.getField(), parseItem(content, item));
            } catch (Exception e) {
                log.error("解析表达式错误：", e);
            }
        }

        return packageValues;
    }

    protected String parseItem(byte[] data, NodeItem item) throws ScriptException {

        String tVal;
        byte[] val = CommonUtil.byteToByte(data, item.getByteStart(), item.getByteLen(), item.getEndian());
        int tempVal = CommonUtil.byte2int(val);
        if (item.isOnlyByte()) {
            tVal = CommonUtil.parseExp(tempVal, item.getExpression(), item.getType());
        } else {
            int biteVal = CommonUtil.getBits(tempVal, item.getBitStart(), item.getBitLen());
            tVal = CommonUtil.parseExp(biteVal, item.getExpression(), item.getType());
        }

        return tVal;
    }


    public void updateStatus(DtuHeader dtuHeader, Map paramValues){
        String deviceId = dtuHeader.getDeviceId();
        if (!deviceCacheProvider.containsKey(deviceId)){

            log.warn("设备不存在[{}]!", deviceId);
            return;
        }
        DeviceInfo deviceInfo = (DeviceInfo) deviceCacheProvider.get(deviceId);

        List list = new ArrayList();
        StringBuilder sqlBuilder = new StringBuilder("UPDATE equipment_info SET ");
        paramValues.keySet().forEach(k -> {
            sqlBuilder.append(k).append("=?, ");

            Object val = paramValues.get(k);
            list.add(val);
        });

        // 最新时间
        sqlBuilder.append("lastTime").append("=?, ");
        list.add(new Date(dtuHeader.getTime()));

        log.info("更新设备[{}]状态...", deviceId);
        String sql = sqlBuilder.substring(0, sqlBuilder.length() - 2) + " WHERE equipmentId=" + deviceInfo.getId();
        jdbcTemplate.update(sql, list.toArray());

        // 写入kafka
        kafkaClient.toKafka(deviceInfo.getId(), paramValues);
    }
}
