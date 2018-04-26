package model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Description: TestModbus
 * Author: DIYILIU
 * Update: 2018-04-20 15:19
 */

@XmlRootElement(name = "modbus")
public class TestModbus {

    List<TestPoint> pointList;

    public List<TestPoint> getPointList() {
        return pointList;
    }

    @XmlElement(name = "point")
    public void setPointList(List<TestPoint> pointList) {
        this.pointList = pointList;
    }
}
