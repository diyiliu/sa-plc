package com.tiza.gw.support.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: MsgMemory
 * Author: DIYILIU
 * Update: 2018-04-26 08:57
 */

@Data
public class MsgMemory {

    private String deviceId;

    /** 当前正在下发的消息*/
    private SendMsg current;

    /** 已经下发得定时查询消息*/
    private Map<String, SendMsg> msgMap = new HashMap();
}
