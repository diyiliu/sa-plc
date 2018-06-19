package com.tiza.gw.support.dao.jpa;

import com.tiza.gw.support.dao.dto.MaintainRemind;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Description: MaintainRemindJpa
 * Author: DIYILIU
 * Update: 2018-06-14 15:13
 */
public interface MaintainRemindJpa extends JpaRepository<MaintainRemind, Long> {


    List<MaintainRemind> findByEquipIdAndPolicyDetailId(long equipId, long detailId, Sort sort);
}
