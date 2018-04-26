package com.tiza.gw.support.model.bean.key;

import lombok.Data;

import java.io.Serializable;

/**
 * Description: DetailKey
 * Author: DIYILIU
 * Update: 2018-04-26 13:45
 */

@Data
public class DetailKey implements Serializable {

    private Long equipId;

    private String tag;
}
