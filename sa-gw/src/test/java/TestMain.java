import com.diyiliu.plugin.util.CommonUtil;
import com.tiza.gw.support.model.TopicMsg;
import org.junit.Test;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Description: TestMain
 * Author: DIYILIU
 * Update: 2018-04-26 09:49
 */
public class TestMain {

    @Test
    public void test() {
        byte[] bytes = new byte[]{1, 1};
        StringBuilder strb = new StringBuilder();
        for (int i = bytes.length - 1; i > 0; i--) {
            byte b = bytes[i];

            strb.append(CommonUtil.byte2BinaryStr(b));
        }

        System.out.println(strb);
    }

    @Test
    public void test2() {
        String str = "41D8";

        byte[] bytes = CommonUtil.hexStringToBytes(str);

        int i = CommonUtil.byte2int(bytes);
        System.out.println(Float.intBitsToFloat(i));
    }

    @Test
    public void test3() {
        byte b = 5;

        String str = CommonUtil.byte2BinaryStr(b);
        System.out.println(str);


        System.out.println(CommonUtil.binaryStr2Byte(str));
    }

    @Test
    public void test4() {
        String str = "0000000010000000";

        byte[] bytes = CommonUtil.binaryStr2Bytes(str);

        System.out.println(CommonUtil.byte2int(bytes));
    }


    @Test
    public void test5() {
        long l = 863703034045281l;
        byte[] bytes = CommonUtil.long2Bytes(l, 7);

        System.out.println(CommonUtil.bytesToStr(bytes));
    }


    @Test
    public void test6() {

        String str = "4.153E-42";

        System.out.println(Double.parseDouble(str));

        BigDecimal bigDecimal = new BigDecimal(str);

        System.out.println(String.format("%.100000f", bigDecimal));

    }

    @Test
    public void test7() {
        List<TopicMsg> list = new ArrayList();

        TopicMsg tm1 = new TopicMsg();
        tm1.setTopic("1");

        TopicMsg tm2 = new TopicMsg();
        tm2.setTopic("2");

        TopicMsg tm3 = new TopicMsg();
        tm3.setTopic("3");

        list.add(tm1);
        list.add(tm2);
        list.add(tm3);

        list.removeIf(i -> i.getTopic().equals("1"));

        System.out.println(list.size());
    }


    @Test
    public void test8() throws Exception {

        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");

        SimpleBindings bindings = new SimpleBindings();
        bindings.put("$value", "1");

        Object o = engine.eval("$value > 10 && $value > 10 ;", bindings);
        System.out.println(o.equals(true));
    }

}
