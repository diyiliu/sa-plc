package com.tiza.gw.support.dao.jpa;

import com.tiza.gw.support.dao.dto.FaultInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Description: FaultInfoJpa
 * Author: DIYILIU
 * Update: 2018-05-09 17:09
 */
public interface FaultInfoJpa extends JpaRepository<FaultInfo, Long> {

    FaultInfo findById(long id);

    @Query("select f from FaultInfo f where endTime is null or endTime < startTime")
    List<FaultInfo> findByEndTimeIsNullOrEndTimeBeforeStartTime();
}
