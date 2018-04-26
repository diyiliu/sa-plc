package com.tiza.gw.support.model;

import com.tiza.gw.support.model.bean.DetailInfo;
import lombok.Data;
import org.omg.CORBA.Object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description: StoreGroup
 * Author: DIYILIU
 * Update: 2018-04-26 09:45
 */

@Data
public class StoreGroup {
    private Map<String, Object> summary = new HashMap();

    private List<DetailInfo> detailList = new ArrayList();
}
