package com.tiza.gw.support.listener;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.util.SpringUtil;
import com.tiza.gw.support.dao.dto.FaultInfo;
import com.tiza.gw.support.dao.jpa.FaultInfoJpa;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Description: DataInitialize
 * Author: DIYILIU
 * Update: 2018-05-28 10:20
 */

@Slf4j
@Component
public class DataInitialize implements ApplicationListener {

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ApplicationReadyEvent){

            synchRedis();
        }
    }

    /**
     * 同步故障缓存
     */
    private void synchRedis(){
        ICache faultCache = SpringUtil.getBean("faultCacheProvider");
        JdbcTemplate jdbcTemplate = SpringUtil.getBean("jdbcTemplate");

        
        String sql = "SELECT " +
                " f.*, c.`Level` " +
                "FROM " +
                " equipment_fault f " +
                "LEFT JOIN fault_code c ON c.id = f.FaultId " +
                "WHERE " +
                " f.FaultEndTime IS NULL " +
                "OR f.FaultEndTime < f.FaultStartTime";


        List<FaultInfo> faultInfoList = jdbcTemplate.query(sql, (ResultSet rs, int rowNum) -> {
            FaultInfo faultInfo = new FaultInfo();
            faultInfo.setId(rs.getLong("id"));
            faultInfo.setFaultType(rs.getInt("level"));
            faultInfo.setEquipId(rs.getLong("equipId"));
            faultInfo.setTag(rs.getString("tag"));

            return faultInfo;
        });

        Map<Long, List<FaultInfo>> listMap = faultInfoList.stream().collect(Collectors.groupingBy(FaultInfo::getEquipId));
        faultCache.put(listMap);
    }
}
