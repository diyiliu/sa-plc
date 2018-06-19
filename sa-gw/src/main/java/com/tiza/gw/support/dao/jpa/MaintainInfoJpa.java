package com.tiza.gw.support.dao.jpa;

import com.tiza.gw.support.dao.dto.MaintainInfo;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

/**
 * Description: MaintainInfoJpa
 * Author: DIYILIU
 * Update: 2018-06-14 09:45
 */
public interface MaintainInfoJpa extends JpaRepository<MaintainInfo, Long> {

    List<MaintainInfo> findByPolicyIdIn(Set<Long> policyIds, Sort sort);
}
