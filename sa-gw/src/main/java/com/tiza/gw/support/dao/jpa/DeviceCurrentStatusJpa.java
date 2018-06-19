package com.tiza.gw.support.dao.jpa;

import com.tiza.gw.support.dao.dto.DeviceCurrentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Description: DeviceCurrentStatusJpa
 * Author: DIYILIU
 * Update: 2018-06-14 16:30
 */

public interface DeviceCurrentStatusJpa extends JpaRepository<DeviceCurrentStatus, Long> {


    DeviceCurrentStatus findByEquipId(long equipId);
}
