package com.tiza.gw.support.model.bean;

import com.tiza.gw.support.model.bean.key.DetailKey;
import lombok.Data;

import javax.persistence.*;

/**
 * Description: DetailInfo
 * Author: DIYILIU
 * Update: 2018-04-26 13:43
 */

@Data
@Entity
@IdClass(DetailKey.class)
@Table(name = "equipment_info_detail")
public class DetailInfo {

    @Id
    private Long equipId;

    @Id
    private String tag;

    private String value;

    @Column(name = "plcVersionPointId")
    private Long pointId;
}
