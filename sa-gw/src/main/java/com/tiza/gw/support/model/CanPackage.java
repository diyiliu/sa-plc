package com.tiza.gw.support.model;

import lombok.Data;

import java.util.List;

/**
 * Description: CanPackage
 * Author: DIYILIU
 * Update: 2016-04-21 16:44
 */

@Data
public class CanPackage {

    // 读功能码
    private String readFunction;
    // 写功能码
    private String writeFunction;

    private Integer address;

    private Integer offset;

    private Integer length;

    private List<NodeItem> itemList;

    private Integer period;
}
