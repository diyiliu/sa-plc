package com.tiza.gw.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.tiza.gw.support.dao.dto.*;
import com.tiza.gw.support.dao.jpa.DeviceCurrentStatusJpa;
import com.tiza.gw.support.dao.jpa.MaintainInfoJpa;
import com.tiza.gw.support.dao.jpa.MaintainLogJpa;
import com.tiza.gw.support.dao.jpa.MaintainRemindJpa;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.data.domain.Sort;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Description: MaintainTask
 * Author: DIYILIU
 * Update: 2018-06-14 09:02
 */

@Slf4j
public class MaintainTask implements ITask {

    private MaintainInfoJpa maintainInfoJpa;

    private MaintainRemindJpa maintainRemindJpa;

    private MaintainLogJpa maintainLogJpa;

    private DeviceCurrentStatusJpa deviceCurrentStatusJpa;

    private ICache deviceCache;

    @Override
    public void execute() {
        log.info("保养提醒分析 ... ");

        Map<String, Object> deviceInfoMap = deviceCache.get(deviceCache.getKeys());
        Set<Long> policyIds = deviceInfoMap.keySet().stream().map(e -> {
            DeviceInfo deviceInfo = (DeviceInfo) deviceInfoMap.get(e);
            return deviceInfo.getMaintainPolicyId();
        }).collect(Collectors.toSet());

        List<MaintainInfo> maintainInfoList = maintainInfoJpa.findByPolicyIdIn(policyIds, Sort.by(new String[]{"isPeriod", "isMajor", "workHoursBegin"}));
        Map<Long, List<MaintainInfo>> maintainInfoMap = maintainInfoList.stream().collect(Collectors.groupingBy(MaintainInfo::getPolicyId));

        Set<String> deviceSet = deviceInfoMap.keySet();
        for (Iterator<String> iterator = deviceSet.iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            DeviceInfo deviceInfo = (DeviceInfo) deviceInfoMap.get(key);

            if (deviceInfo.getId() != 94){
                continue;
            }

            DeviceCurrentStatus currentStatus = deviceCurrentStatusJpa.findByEquipId(deviceInfo.getId());
            deviceInfo.setWorkHours(currentStatus.getTotalWorkTime());

            // 保养记录
            List<MaintainLog> maintainLogs = maintainLogJpa.findByEquipId(deviceInfo.getId(), Sort.by(Sort.Direction.DESC, "maintainTime"));
            MaintainLog lastMtLog = null;
            if (CollectionUtils.isNotEmpty(maintainLogs)) {
                lastMtLog = maintainLogs.get(0);
            }

            Long policyId = deviceInfo.getMaintainPolicyId();
            if (!maintainInfoMap.containsKey(policyId)) {
                continue;
            }

            List<MaintainInfo> maintainInfos = maintainInfoMap.get(policyId);
            List<MaintainInfo> list = maintainInfos.stream().filter(mt -> mt.getIsMajor() == 0).collect(Collectors.toList());
            // 普通保养
            for (MaintainInfo maintainInfo : list) {

                dealMaintain(maintainInfo, lastMtLog, deviceInfo);
            }

            List<MaintainInfo> majorList = maintainInfos.stream().filter(mt -> mt.getIsMajor() == 1).collect(Collectors.toList());
            // 小保养、大保养
            dealMajor(majorList, lastMtLog, deviceInfo);
        }
    }

    private void dealMaintain(MaintainInfo maintainInfo, MaintainLog maintainLog, DeviceInfo deviceInfo) {
        double workHour = deviceInfo.getWorkHours() == null ? 0 : deviceInfo.getWorkHours();
        int month = calcMonth(deviceInfo.getFactoryDate(), new Date());

        boolean isPeriod = maintainInfo.getIsPeriod() == 1 ? true : false;
        List<MaintainRemind> reminds = maintainRemindJpa.findByEquipIdAndPolicyDetailId(deviceInfo.getId(), maintainInfo.getId(), Sort.by(Sort.Direction.DESC, "workHours"));

        MaintainRemind remind = new MaintainRemind();
        remind.setEquipId(deviceInfo.getId());
        remind.setPolicyId(maintainInfo.getPolicyId());
        remind.setPolicyDetailId(maintainInfo.getId());
        remind.setWorkHours(workHour);
        remind.setBuyMonths(Double.valueOf(month));

        remind.setTimes(reminds.size() + 1);
        remind.setCreateTime(new Date());
        remind.setStatus(1);

        double mtHour = maintainInfo.getWorkHoursBegin();
        // 非周期保养
        if (!isPeriod && CollectionUtils.isEmpty(reminds)) {

            // 保养提醒
            if (mtHour > workHour) {

                maintainRemindJpa.save(remind);
            }
        }
        // 周期保养
        else {
            if (CollectionUtils.isNotEmpty(reminds)) {
                MaintainRemind lastRemind = reminds.get(0);
                if (lastRemind.getStatus() == 1) {

                    return;
                }
            }

            double hourGap = workHour;
            if (maintainLog != null) {
                hourGap = workHour - maintainLog.getWorkHour();
            }

            // 保养提醒
            if (hourGap >= mtHour) {
                if (CollectionUtils.isNotEmpty(reminds)) {
                    MaintainRemind lastRemind = reminds.get(0);

                    if (workHour - lastRemind.getWorkHours() < mtHour){

                        return;
                    }
                }

                maintainRemindJpa.save(remind);
            }
        }
    }

    private void dealMajor(List<MaintainInfo> majorList, MaintainLog maintainLog, DeviceInfo deviceInfo) {
        double workHour = deviceInfo.getWorkHours() == null ? 0 : deviceInfo.getWorkHours();
        int month = calcMonth(deviceInfo.getFactoryDate(), new Date());

        if (CollectionUtils.isEmpty(majorList)) {
            return;
        }

        for (int i = majorList.size() - 1; i >= 0; i--) {
            MaintainInfo mtInfo = majorList.get(i);

            if (workHour > mtInfo.getWorkHoursBegin() || month > mtInfo.getBuyMonthsBegin()) {
                if (maintainLog != null) {
                    double workHourGap = deviceInfo.getWorkHours() - maintainLog.getWorkHour();
                    int monthGap = calcMonth(maintainLog.getMaintainTime(), new Date());

                    if (i > 0) {
                        MaintainInfo preMtInfo = majorList.get(i - 1);
                        if ((workHourGap < mtInfo.getWorkHoursBegin() - preMtInfo.getWorkHoursBegin()) &&
                                (monthGap < mtInfo.getBuyMonthsBegin() - preMtInfo.getBuyMonthsBegin())) {

                            continue;
                        }
                    }
                }

                List<MaintainRemind> reminds = maintainRemindJpa.findByEquipIdAndPolicyId(deviceInfo.getId(), mtInfo.getPolicyId(), Sort.by(Sort.Direction.DESC, "workHours"));
                if (CollectionUtils.isNotEmpty(reminds)) {
                    MaintainRemind lastRemind = reminds.get(0);
                    if (lastRemind.getStatus() == 1) {

                        return;
                    }
                }

                MaintainRemind remind = new MaintainRemind();
                remind.setEquipId(deviceInfo.getId());
                remind.setPolicyId(mtInfo.getPolicyId());
                remind.setPolicyDetailId(mtInfo.getId());
                remind.setWorkHours(workHour);
                remind.setBuyMonths(Double.valueOf(month));

                remind.setTimes(reminds.size() + 1);
                remind.setCreateTime(new Date());
                remind.setStatus(1);

                maintainRemindJpa.save(remind);
                continue;
            }

        }
    }

    private int calcMonth(Date date1, Date date2) {
        Calendar past = Calendar.getInstance();
        past.setTime(date1);

        Calendar now = Calendar.getInstance();
        now.setTime(date2);

        int year = now.get(Calendar.YEAR) - past.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH) - past.get(Calendar.MONTH);

        return year * 12 + month;
    }

    public void setMaintainInfoJpa(MaintainInfoJpa maintainInfoJpa) {
        this.maintainInfoJpa = maintainInfoJpa;
    }

    public void setMaintainRemindJpa(MaintainRemindJpa maintainRemindJpa) {
        this.maintainRemindJpa = maintainRemindJpa;
    }

    public void setMaintainLogJpa(MaintainLogJpa maintainLogJpa) {
        this.maintainLogJpa = maintainLogJpa;
    }

    public void setDeviceCurrentStatusJpa(DeviceCurrentStatusJpa deviceCurrentStatusJpa) {
        this.deviceCurrentStatusJpa = deviceCurrentStatusJpa;
    }

    public void setDeviceCache(ICache deviceCache) {
        this.deviceCache = deviceCache;
    }
}
