package com.tiza.gw.support.model;

import com.diyiliu.plugin.model.Header;
import lombok.Data;

/**
 * Description: DtuHeader
 * Author: DIYILIU
 * Update: 2018-01-30 10:12
 */

@Data
public class DtuHeader extends Header {

    private String deviceId;

    private int address;

    private int code;

    private byte[] content = new byte[0];
}
