package com.tiza.gw.support.client;

import lombok.Data;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.QualifierFilter;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Description: HBaseClient
 * Author: DIYILIU
 * Update: 2018-05-08 13:55
 */

@Data
public class HBaseClient {
    private final String METRICS_TABLE_NAME = "tcloud:shangair_metrics";
    private final String METRICS_COLUMN_FAMILY = "1";

    private Configuration config;

    /**
     * 查询HBase 历史数据
     *
     * @param id
     * @param tag
     * @param startTime
     * @param endTime
     * @return
     * @throws IOException
     */
    public List<String> scan(int id, String tag, long startTime, long endTime) throws IOException {
        List<String> list = new ArrayList();

        byte[] startRow = Bytes.add(Bytes.toBytes(id), Bytes.toBytes(startTime));
        byte[] stopRow = Bytes.add(Bytes.toBytes(id), Bytes.toBytes(endTime));
        try (Connection connection = ConnectionFactory.createConnection(config)) {
            TableName tableName = TableName.valueOf(METRICS_TABLE_NAME);
            Table table = connection.getTable(tableName);

            byte[] family = Bytes.toBytes(METRICS_COLUMN_FAMILY);
            Scan scan = new Scan(startRow, stopRow);
            scan.addFamily(family);

            byte[] tagBytes = Bytes.toBytes(tag);
            FilterList qualifierFilters = new FilterList(FilterList.Operator.MUST_PASS_ONE);
            qualifierFilters.addFilter(new QualifierFilter(CompareFilter.CompareOp.EQUAL, new BinaryComparator(tagBytes)));
            scan.setFilter(qualifierFilters);

            ResultScanner rs = table.getScanner(scan);
            System.out.println(rs.next());
            try {
                for (Result r = rs.next(); r != null; r = rs.next()) {
                    // rowKey = id + time
                    byte[] bytes = r.getRow();
                    int equipId = Bytes.toInt(bytes);
                    long timestamp = Bytes.toLong(bytes, 4);

                    if (r.containsNonEmptyColumn(family, tagBytes)) {
                        String value = Bytes.toString(r.getValue(family, tagBytes));
                        list.add(value);
                    }
                }
            } finally {
                rs.close();
            }
        }

        return list;
    }
}
