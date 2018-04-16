package com.tiza.gw.support.jpa;

import com.tiza.gw.support.model.bean.FunctionInfo;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Description: FunctionSetJpa
 * Author: DIYILIU
 * Update: 2018-04-16 09:53
 */
public interface FunctionSetJpa extends JpaRepository<FunctionInfo, Long> {

}
