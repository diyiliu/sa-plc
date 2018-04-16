package com.tiza.gw.protocol.cmd;

import com.diyiliu.plugin.model.Header;
import com.tiza.gw.protocol.DtuDataProcess;
import com.tiza.gw.support.model.DtuHeader;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: CMD_01
 * Author: DIYILIU
 * Update: 2018-01-30 09:48
 */

@Service
public class CMD_01 extends DtuDataProcess {

    public CMD_01() {
        this.cmd = 0x01;
    }

    @Override
    public void parse(byte[] content, Header header) {
        DtuHeader dtuHeader = (DtuHeader) header;
        byte b1 = content[0];
        byte b2 = content[1];

        String[] items1 = new String[]{"BypassControlOut","FaultStopOut","OilPumpRunOut","HeatingOut",
                "LoadValve1Out","LoadValve2Out","FirstPollutionOut","SecondPollutionOut"};
        String[] items2 = new String[]{"ThirdPollutionOut","FourthPollutionOut","FirstWaterInletOut","SecondWaterInletOut",
                "ThirdWaterInletOut","FourthWaterInletOut","StopValveOut","FaultOut"};

        Map paramValues = new HashMap();
        paramValues.putAll(parseByte(b1, items1));
        paramValues.putAll(parseByte(b2, items2));

        updateStatus(dtuHeader, paramValues);
    }
}
