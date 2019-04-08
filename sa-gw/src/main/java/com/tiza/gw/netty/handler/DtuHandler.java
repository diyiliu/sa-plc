package com.tiza.gw.netty.handler;

import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.SpringUtil;
import com.tiza.gw.protocol.DtuDataProcess;
import com.tiza.gw.support.config.Constant;
import com.tiza.gw.support.model.DtuHeader;
import com.tiza.gw.support.task.SenderTask;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Description: DtuHandler
 * Author: DIYILIU
 * Update: 2018-01-26 10:39
 */

@Slf4j
public class DtuHandler extends ChannelInboundHandlerAdapter {

    private Attribute attribute;

    private ExecutorService executorService = Executors.newCachedThreadPool();

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

                        DtuDataProcess dataProcess = SpringUtil.getBean("dtuDataProcess");
                        dataProcess.offline(deviceId);
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

        executorService.execute(()->{
            DtuHeader dtuHeader = new DtuHeader();
            dtuHeader.setDeviceId(deviceId);
            dtuHeader.setAddress(address);
            dtuHeader.setCode(code);
            dtuHeader.setContent(bytes);

            dataProcess.parse(bytes, dtuHeader);
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("服务器异常...{}", cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        attribute = ctx.channel().attr(AttributeKey.valueOf(Constant.NETTY_DEVICE_ID));
        SenderTask senderTask = SpringUtil.getBean("senderTask");

        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;

            if (IdleState.READER_IDLE == event.state()) {
                //logger.warn("读超时...[{}]", key);

            } else if (IdleState.WRITER_IDLE == event.state()) {
                //logger.warn("写超时...");

            } else if (IdleState.ALL_IDLE == event.state()) {
                String deviceId = (String) attribute.get();

                if (StringUtils.isNotEmpty(deviceId)){
                    byte[] bytes = senderTask.fetchData(deviceId);
                    if (bytes != null && bytes.length > 0){

                        ctx.writeAndFlush(Unpooled.copiedBuffer(bytes));
                    }
                }
            }
        }

    }
}
