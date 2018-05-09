package com.tiza.gw.support.dao.dto;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * Description: FaultInfo
 * Author: DIYILIU
 * Update: 2018-05-09 17:04
 */

@Data
@Entity
@Table(name = "equipment_fault")
public class FaultInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long faultId;

    private Long equipId;

    @Column(name = "PlcVersionPointId")
    private Long pointId;

    private String tag;

    private String value;

    @Column(name = "FaultStartTime")
    private Date startTime;

    @Column(name = "FaultEndTime")
    private Date endTime;
}
