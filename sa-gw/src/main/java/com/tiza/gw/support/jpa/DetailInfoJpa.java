package com.tiza.gw.support.jpa;

import com.tiza.gw.support.model.bean.DetailInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Description: DeviceInfoJpa
 * Author: DIYILIU
 * Update: 2018-04-16 09:51
 */
public interface DetailInfoJpa extends JpaRepository<DetailInfo, Long> {

    List<DetailInfo> findByEquipIdAndTagIn(long equipId, String[] tags);
}
