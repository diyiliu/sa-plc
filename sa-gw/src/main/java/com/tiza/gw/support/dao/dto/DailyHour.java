package com.tiza.gw.support.dao.dto;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * Description: DailyHour
 * Author: DIYILIU
 * Update: 2018-05-08 14:44
 */

@Data
@Entity
@IdClass(DailyHourKey.class)
@Table(name = "daily_work_hour")
public class DailyHour {

    @Id
    private Long equipmentId;

    @Id
    private Date day;

    @Column(name = "hours")
    private Long hour;
}

@Data
class DailyHourKey implements Serializable{

    private Long equipmentId;

    private Date day;
}
