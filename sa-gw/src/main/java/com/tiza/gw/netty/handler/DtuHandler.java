package com.tiza.gw.netty.handler;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.SpringUtil;
import com.tiza.gw.protocol.DtuDataProcess;
import com.tiza.gw.support.config.Constant;
import com.tiza.gw.support.model.DtuHeader;
import com.tiza.gw.support.dao.dto.DeviceInfo;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Description: DtuHandler
 * Author: DIYILIU
 * Update: 2018-01-26 10:39
 */

@Slf4j
public class DtuHandler extends ChannelInboundHandlerAdapter {

    private Attribute attribute;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("建立连接...");
        attribute = ctx.channel().attr(AttributeKey.valueOf(Constant.NETTY_DEVICE_ID));

        // 断开连接
        ctx.channel().closeFuture().addListener(
                (ChannelFuture future) -> {
                    String deviceId = (String) attribute.get();
                    if (StringUtils.isNotEmpty(deviceId)) {
                        log.info("设备[{}]断开连接...", deviceId);

                        ICache online = SpringUtil.getBean("onlineCacheProvider");
                        online.remove(deviceId);
                        attribute.set(null);

                        ICache deviceCache = SpringUtil.getBean("deviceCacheProvider");
                        if (deviceCache.containsKey(deviceId)) {
                            DeviceInfo deviceInfo = (DeviceInfo) deviceCache.get(deviceId);

                            // 设备离线
                            JdbcTemplate jdbcTemplate = SpringUtil.getBean("jdbcTemplate");
                            String sql = "UPDATE equipment_info SET DtuStatus = 0 WHERE EquipmentId = " + deviceInfo.getId();
                            jdbcTemplate.update(sql);
                        }
                    }
                }
        );
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        String deviceId = (String) attribute.get();

        ByteBuf byteBuf = (ByteBuf) msg;
        int address = byteBuf.readUnsignedByte();
        int code = byteBuf.readUnsignedByte();

        DtuDataProcess dataProcess = SpringUtil.getBean("dtuDataProcess");
        if (dataProcess == null) {
            log.warn("找不到指令[{}]解析器!", CommonUtil.toHex(code));

            return;
        }

        int length = byteBuf.readUnsignedByte();
        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes);

        DtuHeader dtuHeader = new DtuHeader();
        dtuHeader.setDeviceId(deviceId);
        dtuHeader.setAddress(address);
        dtuHeader.setCode(code);
        dtuHeader.setContent(bytes);

        dataProcess.parse(bytes, dtuHeader);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("服务器异常...{}", cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}
