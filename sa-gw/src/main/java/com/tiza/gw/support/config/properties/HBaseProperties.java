package com.tiza.gw.support.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Description: HBaseProperties
 * Author: DIYILIU
 * Update: 2018-05-08 13:53
 */

@Data
@ConfigurationProperties("hbase")
public class HBaseProperties {

    private String zookeeperQuorum;

    private String zookeeperPort;
}
