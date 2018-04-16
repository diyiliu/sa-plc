package com.tiza.gw.support.model;

/**
 * Description: QueryFrame
 * Author: DIYILIU
 * Update: 2018-01-30 09:10
 */
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

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
