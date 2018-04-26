package model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * 如果java对象属性同时添加了get和set方法，注解不能定义在属性的定义上，
 * 只需在get或者set方法上定义一个即可，否则jaxb会报错！
 * <p>
 * Description: model.TestPoint
 * Author: DIYILIU
 * Update: 2018-04-20 13:38
 */

public class TestPoint {

    private String id;

    private String function;

    private String tag;

    private String field;

    public String getId() {
        return id;
    }

    @XmlAttribute(name = "siteId")
    public void setId(String id) {
        this.id = id;
    }

    public String getFunction() {
        return function;
    }

    @XmlAttribute(name = "readFunction")
    public void setFunction(String function) {
        this.function = function;
    }

    public String getTag() {
        return tag;
    }

    @XmlAttribute
    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getField() {
        return field;
    }

    @XmlElement
    public void setField(String field) {
        this.field = field;
    }
}
