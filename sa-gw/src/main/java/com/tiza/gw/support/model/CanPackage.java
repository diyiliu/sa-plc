package com.tiza.gw.support.model;

import java.util.List;

/**
 * Description: CanPackage
 * Author: DIYILIU
 * Update: 2016-04-21 16:44
 */

public class CanPackage {

    private String packageId;
    private int length;
    private List<NodeItem> itemList;

    private int period;

    public CanPackage() {
    }

    public CanPackage(String packageId, int length) {
        this.packageId = packageId;
        this.length = length;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public List<NodeItem> getItemList() {
        return itemList;
    }

    public void setItemList(List<NodeItem> itemList) {
        this.itemList = itemList;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }
}
