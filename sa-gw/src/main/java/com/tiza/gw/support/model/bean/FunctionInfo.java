package com.tiza.gw.support.model.bean;

import com.tiza.gw.support.model.CanPackage;
import lombok.Data;

import javax.persistence.*;
import java.util.Map;

/**
 * Description: FunctionInfo
 * Author: DIYILIU
 * Update: 2016-04-21 11:25
 */

@Data
@Entity
@Table(name = "plc_version")
public class FunctionInfo {

    @Id
    private Long  id;

    @Column(name = "name")
    private String softName;

    @Column(name = "funcSet")
    private String functionXml;

    @Transient
    private String modelCode;

    @Transient
    private Map<String, CanPackage> canPackages;

    // can包 packageId 长度（占字节数）
    @Transient
    private int pidLength;
}
