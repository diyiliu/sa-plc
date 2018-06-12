package com.tiza.gw.support.config;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.cache.ram.RamCacheProvider;
import com.diyiliu.plugin.util.SpringUtil;
import com.tiza.gw.netty.server.DtuServer;
import com.tiza.gw.support.client.HBaseClient;
import com.tiza.gw.support.client.RedisClient;
import com.tiza.gw.support.config.properties.HBaseProperties;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * Description: SpringConfiguration
 * Author: DIYILIU
 * Update: 2018-01-29 10:25
 */

@Configuration
@EnableConfigurationProperties(HBaseProperties.class)
public class SpringConfig {

    @Value("${dtu-server-port}")
    private Integer port;

    @Autowired
    private HBaseProperties hbaseProperties;

    @Resource
    private RedisTemplate redisTemplate;


    @Bean
    public RedisClient redisClient(){
        RedisClient redisClient = new RedisClient();
        redisClient.setRedisTemplate(redisTemplate);

        return redisClient;
    }

    @Bean
    public HBaseClient hbaseClient() {
        org.apache.hadoop.conf.Configuration config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.quorum", hbaseProperties.getZookeeperQuorum());
        config.set("hbase.zookeeper.property.clientPort", hbaseProperties.getZookeeperPort());

        HBaseClient hbaseClient = new HBaseClient();
        hbaseClient.setConfig(config);

        return hbaseClient;
    }

    @Bean
    public DtuServer dtuServer() {
        DtuServer dtuServer = new DtuServer();
        dtuServer.setPort(port);
        dtuServer.init();

        return dtuServer;
    }

    /**
     * spring 工具类
     *
     * @return
     */
    @Bean
    public SpringUtil springUtil() {

        return new SpringUtil();
    }

    /**
     * spring jdbcTemplate
     *
     * @param dataSource
     * @return
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {

        return new JdbcTemplate(dataSource);
    }


    /**
     * 故障缓存
     *
     * @return
     */
    @Bean
    public ICache faultCacheProvider() {

        return new RamCacheProvider();
    }


    /**
     * 设备注册缓存
     *
     * @return
     */
    @Bean
    public ICache onlineCacheProvider() {

        return new RamCacheProvider();
    }

    /**
     * 下发缓存
     *
     * @return
     */
    @Bean
    public ICache sendCacheProvider() {

        return new RamCacheProvider();
    }

    /**
     * 数据库设备缓存
     *
     * @return
     */
    @Bean
    public ICache deviceCacheProvider() {

        return new RamCacheProvider();
    }


    /**
     * 读功能集缓存
     *
     * @return
     */
    @Bean
    public ICache readFnCacheProvider() {

        return new RamCacheProvider();
    }

    /**
     * 写功能集缓存
     *
     * @return
     */
    @Bean
    public ICache writeFnCacheProvider() {

        return new RamCacheProvider();
    }

    /**
     * 定时任务缓存
     *
     * @return
     */
    @Bean
    public ICache timerCacheProvider() {

        return new RamCacheProvider();
    }
}
