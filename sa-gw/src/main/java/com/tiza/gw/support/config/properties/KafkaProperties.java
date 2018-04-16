package com.tiza.gw.support.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Description: KafkaProperties
 * Author: DIYILIU
 * Update: 2018-04-16 15:43
 */

@Data
@ConfigurationProperties("kafka")
public class KafkaProperties {

    private String brokerList;

    private String rowTopic;

    private String dataTopic;
}
