package com.tiza.gw.support.config;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.cache.ram.RamCacheProvider;
import com.diyiliu.plugin.util.SpringUtil;
import com.tiza.gw.netty.server.DtuServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Description: SpringConfiguration
 * Author: DIYILIU
 * Update: 2018-01-29 10:25
 */

@Configuration
public class SpringConfig {

    @Value("${dtu-server-port}")
    private Integer port;


    @Bean
    public DtuServer dtuServer(){
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
    public JdbcTemplate jdbcTemplate(DataSource dataSource){

        return new JdbcTemplate(dataSource);
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
