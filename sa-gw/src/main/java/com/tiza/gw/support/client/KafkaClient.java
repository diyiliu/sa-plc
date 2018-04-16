package com.tiza.gw.support.client;

import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.JacksonUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: KafkaClient
 * Author: DIYILIU
 * Update: 2018-02-02 09:30
 */

@Data
@Slf4j
public class KafkaClient {
    private KafkaTemplate kafkaTemplate;

    private String rowTopic;

    private String dataTopic;

    /**
     * 存入kafka原始指令
     *
     * @param deviceId
     * @param bytes
     */
    public void toKafka(String deviceId, byte[] bytes, int direction) {
        log.info("[{}] 设备[{}]原始数据[{}]写入kafka...", direction == 1 ? "上行" : "下行", deviceId, CommonUtil.bytesToStr(bytes));

        Map map = new HashMap();
        map.put("id", deviceId);
        map.put("timestamp", System.currentTimeMillis());
        map.put("data", CommonUtil.bytesToStr(bytes));
        map.put("flow", direction);

        kafkaTemplate.send(rowTopic, JacksonUtil.toJson(map));
    }

    /**
     * 存入kafka解析数据
     *
     * @param id
     * @param paramValues
     */
    public void toKafka(long id, Map paramValues) {
        log.info("设备[{}]解析数据写入kafka...", id);

        Map map = new HashMap();
        map.put("id", id);
        map.put("timestamp", System.currentTimeMillis());
        map.put("metrics", JacksonUtil.toJson(paramValues));

        kafkaTemplate.send(dataTopic, JacksonUtil.toJson(map));
    }
}
