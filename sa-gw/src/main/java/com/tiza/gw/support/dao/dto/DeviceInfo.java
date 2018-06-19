package com.tiza.gw.support.dao.dto;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;


/**
 * Description: DeviceInfo
 * Author: DIYILIU
 * Update: 2018-01-30 11:13
 */

@Data
@Entity
@Table(name = "equipment")
public class DeviceInfo {

    @Id
    private Long id;

    private String dtuId;

    /** 出厂日期 */
    private Date factoryDate;

    @Column(name = "PlcVersionId")
    private String softVersion;

    private Long maintainPolicyId;

    @Transient
    private Double workHours;
}
