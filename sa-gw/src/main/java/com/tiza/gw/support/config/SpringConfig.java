package com.tiza.gw.support.config;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.cache.ram.RamCacheProvider;
import com.diyiliu.plugin.util.SpringUtil;
import com.tiza.gw.netty.server.DtuServer;
import com.tiza.gw.protocol.DtuDataProcess;
import com.tiza.gw.support.listener.CMDInitializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

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
     * 指令初始化
     *
     * @return
     */
    @Bean
    public CMDInitializer cmdInitializer(){
        CMDInitializer cmdInitializer = new CMDInitializer();

        List<Class> protocols = new ArrayList();
        protocols.add(DtuDataProcess.class);
        cmdInitializer.setProtocols(protocols);

        return cmdInitializer;
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
     * 指令缓存
     *
     * @return
     */
    @Bean
    public ICache dtuCMDCacheProvider() {

        return new RamCacheProvider();
    }

    /**
     * 下发缓存
     *
     * @return
     */
    @Bean
    public ICache sendMsgCacheProvider() {

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
     * 功能集缓存
     *
     * @return
     */
    @Bean
    public ICache functionSetCacheProvider() {

        return new RamCacheProvider();
    }
}
