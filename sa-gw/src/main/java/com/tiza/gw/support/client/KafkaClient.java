package com.tiza.gw.support.client;

import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.JacksonUtil;
import com.tiza.gw.support.model.TopicMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Description: KafkaClient
 * Author: DIYILIU
 * Update: 2018-02-02 09:30
 */

@Slf4j
public class KafkaClient extends Thread {
    private KafkaTemplate kafkaTemplate;

    private static String rowTopic;

    private static String dataTopic;

    private static Queue<TopicMsg> pool = new ConcurrentLinkedQueue();

    @Override
    public void run() {

        while (true) {
            while (!pool.isEmpty()) {
                TopicMsg tm = pool.poll();

                String topic = tm.getTopic();
                String content = tm.getContent();

                // 写入kafka
                kafkaTemplate.send(topic, content);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Kafka 原始指令存入队列
     *
     * @param deviceId
     * @param bytes
     */
    public void toKafka(String deviceId, byte[] bytes, int direction) {
        log.info("[{}] 设备[{}]原始数据[{}]...", direction == 1 ? "上行" : "下行", deviceId, CommonUtil.bytesToStr(bytes));

        long time = System.currentTimeMillis();
        Map map = new HashMap();
        map.put("id", deviceId);
        map.put("timestamp", time);
        map.put("data", CommonUtil.bytesToStr(bytes));
        map.put("flow", direction);

        addPool(map, rowTopic);
    }

    /**
     * Kafka 解析数据存入队列
     *
     * @param id
     * @param list
     */
    public void toKafka(long id, List list) {
        //log.info("设备[{}]解析数据...", id);

        long time = System.currentTimeMillis();
        Map map = new HashMap();
        map.put("id", id);
        map.put("timestamp", time);
        map.put("metrics", JacksonUtil.toJson(list));

        addPool(map, dataTopic);
    }

    public void addPool(Object o, String topic){
        TopicMsg tm = new TopicMsg();
        tm.setTopic(topic);
        tm.setContent(JacksonUtil.toJson(o));
        tm.setDateTime(System.currentTimeMillis());

        pool.add(tm);
    }

    public void setKafkaTemplate(KafkaTemplate kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void setRowTopic(String rowTopic) {
        this.rowTopic = rowTopic;
    }

    public void setDataTopic(String dataTopic) {
        this.dataTopic = dataTopic;
    }
}
