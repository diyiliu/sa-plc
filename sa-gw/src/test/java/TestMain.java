import com.diyiliu.plugin.util.CommonUtil;
import org.junit.Test;

/**
 * Description: TestMain
 * Author: DIYILIU
 * Update: 2018-04-26 09:49
 */
public class TestMain {

    @Test
    public void test(){
        byte[] bytes = new byte[]{1, 1};
        StringBuilder strb = new StringBuilder();
        for (int i = bytes.length - 1; i > 0; i--) {
            byte b = bytes[i];

            strb.append(CommonUtil.bytes2BinaryStr(b));
        }

        System.out.println(strb);

    }
}
