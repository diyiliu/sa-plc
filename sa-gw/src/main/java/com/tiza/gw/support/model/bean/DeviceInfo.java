package com.tiza.gw.support.model.bean;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;


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
    private int id;

    private String dtuId;

    @Column(name = "plcVersionId")
    private String softVersion;
}
