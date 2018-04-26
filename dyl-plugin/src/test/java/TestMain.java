import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.JaxbXmlUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import model.TestModbus;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.junit.Test;
import org.springframework.util.ResourceUtils;

import java.io.File;

/**
 * Description: TestMain
 * Author: DIYILIU
 * Update: 2018-04-19 10:08
 */
public class TestMain {

    @Test
    public void test(){

        int i = 1000;

        ByteBuf buf = Unpooled.buffer(4);
        buf.writeShort(i);

        byte[] bytes = CommonUtil.long2Bytes(i, 2);
        buf.writeBytes(bytes);



        System.out.println(buf.array());
    }


    @Test
    public void test2() throws  Exception{
        File file = ResourceUtils.getFile("classpath:plc.xml");
        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(file);

        String str = document.asXML();

        System.out.println(str);

        TestModbus modbus = JaxbXmlUtil.convertToJavaBean(str, TestModbus.class);

        System.out.println(modbus.getPointList().get(0).getId());
    }
}
