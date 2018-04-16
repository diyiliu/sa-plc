package com.tiza.gw.support.model;

import lombok.Data;

/**
 * Description: NodeItem
 * Author: DIYILIU
 * Update: 2016-04-21 11:28
 */

@Data
public class NodeItem {

    private String nameKey;
    private String name;
    private String type;
    private String endian;
    private String position;
    private int byteStart;
    private int byteLen;
    private int bitStart;
    private int bitLen;
    private String field;
    private String expression;
    //是否只去字节
    private boolean isOnlyByte;
}
