package com.tiza.gw.protocol.cmd;

import com.diyiliu.plugin.model.Header;
import com.tiza.gw.protocol.DtuDataProcess;
import com.tiza.gw.support.model.DtuHeader;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: CMD_02
 * Author: DIYILIU
 * Update: 2018-01-30 09:54
 */

@Service
public class CMD_02 extends DtuDataProcess {

    public CMD_02() {
        this.cmd = 0x02;
    }

    @Override
    public void parse(byte[] content, Header header) {
        DtuHeader dtuHeader = (DtuHeader) header;
        byte b1 = content[0];
        byte b2 = content[1];
        byte b3 = content[2];

        String[] items1 = new String[]{"PbStartIn", "PbStopIn", "PbOilPumpIn", "PbEmergencyIn",
                "FbStartIn", "AlarmInverterIn", "FaultInverterIn", "OverLoadOilPumpIn",};
        String[] items2 = new String[]{"LowPressureIn", "LowWaterIn", "OilPressureDifferenceIn", "LoadValveControlHalfIn",
                "LoadValveControlFullIn", "FirstValveControlLowStopIn", "FirstValveControlMiddleStartIn", "FirstValveControlHighAlarmIn"};
        String[] items3 = new String[]{"SecondValveControlLowStopIn", "SecondValveControlMiddleStartIn", "SecondValveControlHighAlarmIn", "ThirdValveControlLowStopIn",
                "ThirdValveControlMiddleStartIn", "ThirdValveControlHighAlarmIn", "FourthValveControlLowStopIn", "FourthValveControlMiddleStartIn"};

        Map paramValues = new HashMap();
        paramValues.putAll(parseByte(b1, items1));
        paramValues.putAll(parseByte(b2, items2));
        paramValues.putAll(parseByte(b3, items3));

        updateStatus(dtuHeader, paramValues);
    }
}
