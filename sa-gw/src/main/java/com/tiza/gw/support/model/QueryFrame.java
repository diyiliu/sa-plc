package com.tiza.gw.support.model;

import lombok.Data;

/**
 * Description: QueryFrame
 * Author: DIYILIU
 * Update: 2018-01-30 09:10
 */

@Data
public class QueryFrame {

    /** 从站地址 */
    private int address;

    /** 功能码 */
    private int code;

    /** 起始地址 */
    private int start;

    /** 数据个数 */
    private int count;

    public QueryFrame(int address, int code, int start, int count) {
        this.address = address;
        this.code = code;
        this.start = start;
        this.count = count;
    }
}
