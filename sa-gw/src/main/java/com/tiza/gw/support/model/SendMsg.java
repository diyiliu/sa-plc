package com.tiza.gw.support.model;

import lombok.Data;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Description: SendMsg
 * Author: DIYILIU
 * Update: 2018-02-09 14:54
 */

@Data
public class SendMsg {
    private String deviceId;

    private Integer cmd;

    private byte[] bytes;

    /** 0: 查询; 1: 设置 */
    private Integer type;

    /** 处理结果 0: 下发; 1: 接收 */
    private Integer result = 0;

    /** 重复次数 */
    private AtomicInteger tryCount = new AtomicInteger(0);

    /** 指令KEY */
    private String key;

    /** 下发时间 */
    private Long datetime;

    /** 下发功能集节点 */
    private List<PointUnit> unitList;

    public int getTryCount() {
        return tryCount.getAndIncrement();
    }
}
