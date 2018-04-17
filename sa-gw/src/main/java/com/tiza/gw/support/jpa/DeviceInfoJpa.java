package com.tiza.gw.support.jpa;

import com.tiza.gw.support.model.bean.DeviceInfo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Description: DeviceInfoJpa
 * Author: DIYILIU
 * Update: 2018-04-16 09:51
 */
public interface DeviceInfoJpa extends JpaRepository<DeviceInfo, Long> {

    DeviceInfo findById(long id);
}
