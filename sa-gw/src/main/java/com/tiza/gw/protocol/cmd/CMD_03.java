package com.tiza.gw.protocol.cmd;

import com.diyiliu.plugin.model.Header;
import com.tiza.gw.protocol.DtuDataProcess;
import com.tiza.gw.support.model.DtuHeader;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: CMD_03
 * Author: DIYILIU
 * Update: 2018-01-30 09:54
 */

@Service
public class CMD_03 extends DtuDataProcess {

    public CMD_03() {
        this.cmd = 0x03;
    }

    @Override
    public void parse(byte[] content, Header header) {
        DtuHeader dtuHeader = (DtuHeader) header;
        String[] items = new String[]{
                "MainCurrent", "PowerSupplyVoltage", "LubricatingOilPressure", "CoolingWaterPressure", "FirstLevelPressure",
                "SecondLevelPressure", "ThirdLevelPressure", "FourthLevelPressure", "FirstLevelTemperature", "SecondLevelTemperature",
                "ThirdLevelTemperature", "FourthLevelTemperature", "FirstWaterTemperature", "SecondWaterTemperature", "ThirdWaterTemperature",
                "FourthWaterTemperature", "LubricatingOilTemperature", "InletWaterTemperature", "InstrumentPressure", "FirstBushingTemperature",
                "SecondBushingTemperature", "ThirdBushingTemperature", "FourthBushingTemperature", "FifthBushingTemperature", "ShaftExtendTemperature",
                "ShaftEndTemperature", "FirstStatorTemperature", "SecondStatorTemperature", "ThirdStatorTemperature", "WorkingFrequency"
        };

        Map paramValues = parseFloat(content, items);
        updateStatus(dtuHeader, paramValues);
    }


    private Map parseFloat(byte[] bytes, String[] items) {
        Map map = new HashMap();
        int count = bytes.length / 4;

        ByteBuf byteBuf = Unpooled.copiedBuffer(bytes);
        for (int i = 0; i < count; i++) {
            int data = byteBuf.readInt();
            float f = Float.intBitsToFloat(data);

            map.put(items[i], f);
        }

        return map;
    }
}
