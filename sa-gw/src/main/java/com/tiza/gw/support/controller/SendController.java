package com.tiza.gw.support.controller;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.util.CommonUtil;
import com.tiza.gw.support.dao.jpa.DetailInfoJpa;
import com.tiza.gw.support.dao.jpa.DeviceInfoJpa;
import com.tiza.gw.support.model.PointUnit;
import com.tiza.gw.support.model.SendMsg;
import com.tiza.gw.support.dao.dto.DetailInfo;
import com.tiza.gw.support.dao.dto.DeviceInfo;
import com.tiza.gw.support.dao.dto.PointInfo;
import com.tiza.gw.support.task.SenderTask;
import com.tiza.gw.support.task.TimerTask;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Description: SendController
 * Author: DIYILIU
 * Update: 2018-04-16 09:10
 */

@Slf4j
@RestController
public class SendController {

    @Resource
    private ICache onlineCacheProvider;

    @Resource
    private ICache writeFnCacheProvider;

    @Resource
    private ICache deviceCacheProvider;

    @Resource
    private ICache sendCacheProvider;

    @Resource
    private ICache timerCacheProvider;

    @Resource
    private DeviceInfoJpa deviceInfoJpa;

    @Resource
    private DetailInfoJpa detailInfoJpa;

    /**
     * 参数设置
     *
     * @param key
     * @param value
     * @param equipId
     * @param rowId
     * @param response
     * @return
     */
    @PostMapping("/setup")
    public String setup(@Param("key") String key, @Param("value") String value,
                        @Param("equipId") String equipId, @Param("rowId") Long rowId, HttpServletResponse response) {
        //log.info("[{}, {}, {}, {}]", key, value, equipId, rowId);

        DeviceInfo deviceInfo = deviceInfoJpa.findById(Long.parseLong(equipId));
        String dtuId = deviceInfo.getDtuId();
        if (!onlineCacheProvider.containsKey(dtuId)) {

            response.setStatus(500);
            return "设备离线。";
        }

        String softVersion = deviceInfo.getSoftVersion();
        if (!writeFnCacheProvider.containsKey(softVersion)) {

            response.setStatus(500);
            return "未配置设备功能集。";
        }

        List<PointUnit> writeUnitList = (List<PointUnit>) writeFnCacheProvider.get(softVersion);
        PointUnit pointUnit = null;
        for (PointUnit unit : writeUnitList) {
            List<String> tagList = Arrays.asList(unit.getTags());
            if (tagList.contains(key)) {

                pointUnit = unit;
                break;
            }
        }

        if (pointUnit == null) {
            response.setStatus(500);
            return "功能集中未找到TAG[{" + key + "}]。";
        }

        // 设置值转int
        int val;
        if (value.indexOf(".") > 0) {
            val = Float.floatToIntBits(Float.parseFloat(value));
        } else {
            val = Integer.parseInt(value);
        }

        // 寄存器数量(dword类型为2, 其他均为1)
        int count;
        // 从站地址
        int side;
        // 起始地址
        int address;

        // 单点下发
        if (1 == pointUnit.getTags().length) {
            PointInfo pointInfo = pointUnit.getPoints()[0];

            int type = pointInfo.getPointType();
            // 寄存器数量
            count = type == 4 ? 2 : 1;
            side = pointInfo.getSiteId();
            address = pointInfo.getAddress();
        } else {
            String[] tags = pointUnit.getTags();
            List<DetailInfo> details = detailInfoJpa.findByEquipIdAndTagIn(deviceInfo.getId(), tags);
            if (CollectionUtils.isEmpty(details)) {

                response.setStatus(500);
                return "TA[{" + key + "}]节点异常。";
            }

            int length = tags.length;
            Map<String, String> tagValue = details.stream().collect(Collectors.toMap(DetailInfo::getTag, DetailInfo::getValue));
            StringBuilder strBuf = new StringBuilder();
            // 最小单元为字(两个字节)
            for (int i = 0; i < 16; i++) {
                if (i < length) {
                    String tag = tags[i];
                    String v = "0";
                    if (tagValue.containsKey(tag)) {
                        v = tagValue.get(tag);
                    }
                    if (key.equals(tag)) {
                        v = value;
                    }
                    strBuf.append(v);
                } else {
                    strBuf.append("0");
                }
            }
            String binaryStr = strBuf.toString();
            byte[] bytes = CommonUtil.binaryStr2Bytes(binaryStr);
            val = CommonUtil.byte2int(bytes);

            count = 1;
            side = pointUnit.getSiteId();
            address = pointUnit.getAddress();
        }
        // 下发单元
        List<PointUnit> unitList = new ArrayList();
        unitList.add(pointUnit);

        // 功能码
        int code = pointUnit.getWriteFunction();

        byte[] bytes = toBytes(side, code, address, count, val);
        SendMsg sendMsg = new SendMsg();
        sendMsg.setRowId(rowId);
        sendMsg.setDeviceId(dtuId);
        sendMsg.setCmd(code);
        sendMsg.setBytes(bytes);
        // 0: 查询; 1: 设置
        sendMsg.setType(1);
        sendMsg.setUnitList(unitList);
        sendMsg.setTags(pointUnit.getTags());
        SenderTask.send(sendMsg, true);
        //log.info("设备[{}]参数[{},{}]等待下发[{}]...", dtuId, key, value, CommonUtil.bytesToStr(bytes));

        return "设置成功。";
    }


    /**
     * 参数同步
     * @param code
     * @param equipId
     * @param response
     * @return
     */
    @PostMapping("/synchronize")
    public String synchronize(@Param("code") Integer code, @Param("equipId") String equipId, HttpServletResponse response) {
        DeviceInfo deviceInfo = deviceInfoJpa.findById(Long.parseLong(equipId));
        String dtuId = deviceInfo.getDtuId();
        if (!onlineCacheProvider.containsKey(dtuId)) {

            response.setStatus(500);
            return "设备离线。";
        }

        String softVersion = deviceInfo.getSoftVersion();
        if (!writeFnCacheProvider.containsKey(softVersion)) {

            response.setStatus(500);
            return "未配置设备功能集。";
        }

        TimerTask task = new TimerTask(deviceCacheProvider, timerCacheProvider, sendCacheProvider);
        task.synchronize(dtuId, code);

        return "设置成功";
    }


    private byte[] toBytes(int site, int code, int address, int count, int value) {
        ByteBuf buf = Unpooled.buffer(7 + count * 2);
        buf.writeByte(site);
        buf.writeByte(code);
        buf.writeShort(address);
        buf.writeShort(count);
        buf.writeByte(count * 2);
        buf.writeBytes(CommonUtil.long2Bytes(value, count * 2));

        return buf.array();
    }
}
