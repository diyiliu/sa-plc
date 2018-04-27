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

            strb.append(CommonUtil.byte2BinaryStr(b));
        }

        System.out.println(strb);

    }

    @Test
    public void test2(){
        String str = "41D8";

        byte[] bytes = CommonUtil.hexStringToBytes(str);

        int i =  CommonUtil.byte2int(bytes);
        System.out.println(Float.intBitsToFloat(i));
    }

    @Test
    public void test3(){
        byte b = 5;

        String str = CommonUtil.byte2BinaryStr(b);
        System.out.println(str);


        System.out.println(CommonUtil.binaryStr2Byte(str));
    }

    @Test
    public void test4(){
        String str = "0000000010000000";

        byte[] bytes = CommonUtil.binaryStr2Bytes(str);

        System.out.println(CommonUtil.byte2int(bytes));
    }
}
