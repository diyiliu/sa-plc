package com.tiza.gw.netty.handler;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.SpringUtil;
import com.tiza.gw.protocol.DtuDataProcess;
import com.tiza.gw.support.config.Constant;
import com.tiza.gw.support.model.DtuHeader;
import com.tiza.gw.support.model.SendMsg;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

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

        // 判断是否收到指令应答
        ICache sendMsgCache = SpringUtil.getBean("sendMsgCacheProvider");
        if (sendMsgCache.containsKey(deviceId)) {
            SendMsg sendMsg = (SendMsg) sendMsgCache.get(deviceId);

            synchronized (sendMsg) {
                if (code == sendMsg.getCmd()) {
                    sendMsgCache.remove(deviceId);
                }
            }
        }

        ICache cmdCacheProvider = SpringUtil.getBean("dtuCMDCacheProvider");
        DtuDataProcess dataProcess = (DtuDataProcess) cmdCacheProvider.get(0xFF);
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
        dtuHeader.setTime(System.currentTimeMillis());

        dataProcess.parse(bytes, dtuHeader);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("服务器异常...{}", cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}
