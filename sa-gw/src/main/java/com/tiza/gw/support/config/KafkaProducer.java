package com.tiza.gw.support.config;

import com.tiza.gw.support.client.KafkaClient;
import com.tiza.gw.support.config.properties.KafkaProperties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: KafkaProducer
 * Author: DIYILIU
 * Update: 2018-04-16 15:42
 */

@EnableKafka
@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaProducer {

    @Autowired
    private KafkaProperties kafkaProperties;

    /**
     * 获取工厂
     */
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBrokerList());
        props.put(ProducerConfig.RETRIES_CONFIG, 1);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 4096);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 40960);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return new DefaultKafkaProducerFactory(props);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {

        return new KafkaTemplate(producerFactory());
    }

    @Bean
    public KafkaClient kafkaClient() {
        KafkaClient kafkaClient = new KafkaClient();
        kafkaClient.setKafkaTemplate(kafkaTemplate());
        kafkaClient.setRowTopic(kafkaProperties.getRowTopic());
        kafkaClient.setDataTopic(kafkaProperties.getDataTopic());
        kafkaClient.start();

        return kafkaClient;
    }
}
