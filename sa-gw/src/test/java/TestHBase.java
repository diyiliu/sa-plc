import com.diyiliu.plugin.util.DateUtil;
import com.diyiliu.plugin.util.JacksonUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.QualifierFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Test;

import java.util.*;

/**
 * Description: TestHBase
 * Author: DIYILIU
 * Update: 2018-05-08 09:33
 */
public class TestHBase {


    @Test
    public void test() throws Exception {
        String TABLE_NAME = "tcloud:shangair_metrics";
        String CF_DEFAULT = "1";

        Configuration config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.quorum", "192.168.1.161,192.168.1.162,192.168.1.163");
        config.set("hbase.zookeeper.property.clientPort", "2181");
        config.set("hbase.zookeeper.session.timeout", "180000");

        Calendar today = Calendar.getInstance();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(0);
        calendar.set(Calendar.YEAR, today.get(Calendar.YEAR));
        calendar.set(Calendar.MONTH, today.get(Calendar.MONTH));
        calendar.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH));

        long endTime = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_MONTH, -10);
        long startTime = calendar.getTimeInMillis();

        final String tag = "TotalRunTime";
        byte[] startRow = Bytes.add(Bytes.toBytes(1), Bytes.toBytes(startTime));
        byte[] stopRow = Bytes.add(Bytes.toBytes(1), Bytes.toBytes(endTime));
        try (Connection connection = ConnectionFactory.createConnection(config)) {

            TableName tableName = TableName.valueOf(TABLE_NAME);
            Table table = connection.getTable(tableName);

            byte[] family = Bytes.toBytes(CF_DEFAULT);
            Scan scan = new Scan(startRow, stopRow);
            scan.addFamily(family);

            byte[] tagBytes = Bytes.toBytes(tag);
            FilterList qualifierFilters = new FilterList(FilterList.Operator.MUST_PASS_ONE);
            qualifierFilters.addFilter( new QualifierFilter(CompareFilter.CompareOp.EQUAL, new BinaryComparator(tagBytes)));
            scan.setFilter(qualifierFilters);

            ResultScanner rs = table.getScanner(scan);
            List<Map<String, Object>> list = new ArrayList();
            try {
                for (Result r = rs.next(); r != null; r = rs.next()) {
                    byte[] bytes = r.getRow();
                    int id = Bytes.toInt(bytes);
                    long timestamp = Bytes.toLong(bytes, 4);

                    Map map = new HashMap();
                    map.put("equipId", id);
                    map.put("datetime", DateUtil.dateToString(new Date(timestamp)));
                    if (r.containsNonEmptyColumn(family, tagBytes)) {
                        String value = Bytes.toString(r.getValue(family, tagBytes));
                        String name = new String(tagBytes);

                        map.put(name, value);
                    }

                    list.add(map);
                }

                System.out.println(JacksonUtil.toJson(list));
            } finally {
                rs.close();
            }
        }
    }
}
