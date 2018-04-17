package com.tiza.gw.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.tiza.gw.support.jpa.FunctionSetJpa;
import com.tiza.gw.support.model.CanPackage;
import com.tiza.gw.support.model.bean.FunctionInfo;
import com.tiza.gw.support.model.NodeItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;

import java.util.*;

/**
 * Description: FunctionSetTask
 * Author: DIYILIU
 * Update: 2018-02-06 09:15
 */

@Slf4j
public class FunctionSetTask implements ITask {
    private ICache functionSetCache;
    private FunctionSetJpa functionSetDao;

    public FunctionSetTask(FunctionSetJpa functionSetDao, ICache functionSetCache) {
        this.functionSetDao = functionSetDao;
        this.functionSetCache = functionSetCache;
    }

    @Override
    public void execute() {
        log.info("刷新功能集列表...");
        try {
            List<FunctionInfo> infoList = functionSetDao.findAll();
            for (FunctionInfo info : infoList) {
                String functionXml = info.getFunctionXml();

                Document document = DocumentHelper.parseText(functionXml);
                Map canMap = dealCan(document);
                info.setCanPackages(canMap);
            }

            refresh(infoList, functionSetCache);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refresh(List<FunctionInfo> infoList, ICache functionCache) {
        if (infoList == null || infoList.size() < 1){
            log.warn("无功能集!");
            return;
        }

        Set oldKeys = functionCache.getKeys();
        Set tempKeys = new HashSet(infoList.size());

        for (FunctionInfo info : infoList) {
            functionCache.put(info.getId().toString(), info);
            tempKeys.add(info.getId().toString());
        }

        Collection subKeys = CollectionUtils.subtract(oldKeys, tempKeys);
        for (Iterator iterator = subKeys.iterator(); iterator.hasNext();){
            String key = (String) iterator.next();
            functionCache.remove(key);
        }
    }


    private Map<String, CanPackage> dealCan(Document document) {
        List<Node> rootPackageNodes = document.selectNodes("modbus/address");

        Map<String, CanPackage> canPackages = new HashedMap();
        for (Node node : rootPackageNodes) {
            CanPackage canPackage = dealPackage(node);

            // 读功能码
            if (StringUtils.isNotEmpty(canPackage.getReadFunction())){
                canPackages.put(canPackage.getReadFunction(), canPackage);
            }

            // 写功能码
            if (StringUtils.isNotEmpty(canPackage.getWriteFunction())){
                canPackages.put(canPackage.getWriteFunction(), canPackage);
            }
        }

        return canPackages;
    }

    private CanPackage dealPackage(Node packageNode) {
        String readFun = packageNode.valueOf("@readFunction");
        String writeFun = packageNode.valueOf("@writeFunction");

        int site = Integer.parseInt(packageNode.valueOf("@siteId"));
        int offset = Integer.parseInt(packageNode.valueOf("@offset"));
        int length = Integer.parseInt(packageNode.valueOf("@length"));

        CanPackage canPackage = new CanPackage();
        canPackage.setReadFunction(readFun);
        canPackage.setWriteFunction(writeFun);
        canPackage.setAddress(site);
        canPackage.setOffset(offset);
        canPackage.setLength(length);

        // 刷新频率
        if (StringUtils.isNotEmpty(packageNode.valueOf("@frequency"))){
            int period = Integer.parseInt(packageNode.valueOf("@frequency"));
            canPackage.setPeriod(period);
        }

        List<Node> nodeItems = packageNode.selectNodes("point");

        if (nodeItems != null && nodeItems.size() > 0) {
            List<NodeItem> itemList = new ArrayList();
            for (Node node : nodeItems) {
                NodeItem nodeItem = dealItem(node);
                if (nodeItem != null) {
                    itemList.add(nodeItem);
                }
            }
            canPackage.setItemList(itemList);
        }

        return canPackage;
    }

    private NodeItem dealItem(Node itemNode) {
        NodeItem itemBean = null;
        try {
            String tag = itemNode.selectSingleNode("tag").getText();

            String field = tag;
            Node fieldNode = itemNode.selectSingleNode("field");
            if (fieldNode != null) {
                field = fieldNode.getText();
            }

            String name = itemNode.selectSingleNode("name").getText();
            String type = itemNode.selectSingleNode("type").getText();
            String endian = itemNode.selectSingleNode("endian") == null ? "big" : itemNode.selectSingleNode("endian").getText();
            Node position = itemNode.selectSingleNode("parse/position");
            Node byteNode = position.selectSingleNode("byte");
            Node bitNode = byteNode.selectSingleNode("bit");
            String byteStart = byteNode.valueOf("@offset");
            String byteLen = byteNode.valueOf("@length");
            Node expNode = itemNode.selectSingleNode("parse/formula");

            itemBean = new NodeItem();
            if (null == bitNode) {
                itemBean.setOnlyByte(true);
            } else {
                itemBean.setOnlyByte(false);
                String bitStart = bitNode.valueOf("@offset");
                String bitLen = bitNode.valueOf("@length");
                itemBean.setBitStart(Integer.parseInt(bitStart));
                itemBean.setBitLen(Integer.parseInt(bitLen));
            }

            // 数值表达式
            if (expNode != null) {
                itemBean.setExpression(expNode.getText());
            }

            // 唯一标识
            itemBean.setTag(tag);
            // 数据库字段
            itemBean.setField(field);

            itemBean.setName(name);
            itemBean.setType(type);
            itemBean.setEndian(endian);
            itemBean.setByteStart(Integer.parseInt(byteStart));
            itemBean.setByteLen(Integer.parseInt(byteLen));
        } catch (Exception e) {
            log.error("解析功能集错误![{}]", e.getMessage());
            e.printStackTrace();
        }

        return itemBean;
    }
}
