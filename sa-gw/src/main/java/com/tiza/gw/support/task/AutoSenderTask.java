package com.tiza.gw.support.task;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.task.ITask;
import com.diyiliu.plugin.util.SpringUtil;
import com.tiza.gw.support.model.QueryFrame;
import com.tiza.gw.support.model.SendMsg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Description: AutoSenderTask
 * Author: DIYILIU
 * Update: 2018-01-29 10:45
 */

@Slf4j
public class AutoSenderTask implements ITask, Runnable {

    public AutoSenderTask(QueryFrame queryFrame, ICache onlineCache) {
        this.queryFrame = queryFrame;
        this.onlineCache = onlineCache;
    }

    /**
     * 发送数据帧
     */
    private QueryFrame queryFrame;

    /**
     * 在线设备
     */
    private ICache onlineCache;


    @Override
    public void run() {

        execute();
    }

    @Override
    public void execute() {
        Set keys = onlineCache.getKeys();
        if (keys.size() < 1) {

            return;
        }

        ByteBuf byteBuf = Unpooled.buffer(6);
        byteBuf.writeByte(queryFrame.getAddress());
        byteBuf.writeByte(queryFrame.getCode());
        byteBuf.writeShort(queryFrame.getStart());
        byteBuf.writeShort(queryFrame.getCount());
        byte[] bytes = byteBuf.array();

        ICache sendMsgCache = SpringUtil.getBean("sendMsgCacheProvider");
        keys.forEach(e -> {
            // 如果有下发指令没回复，则不下发本次指令
            if (sendMsgCache.containsKey(e)) {
                log.warn("下行指令尚未响应，取消终端[{}]本次指令[{}]下发...", e, queryFrame.getCode());

                SendMsg sendMsg = (SendMsg) sendMsgCache.get(e);
                synchronized (sendMsg) {
                    if (sendMsg.getTime() > 2) {

                        sendMsgCache.remove(e);
                    }
                }
            } else {
                log.info("终端[{}]指令[{}]下发...", e, queryFrame.getCode());

                ChannelHandlerContext context = (ChannelHandlerContext) onlineCache.get(e);
                context.writeAndFlush(Unpooled.copiedBuffer(bytes));

                SendMsg sendMsg = new SendMsg();
                sendMsg.setDeviceId((String) e);
                sendMsg.setCmd(queryFrame.getCode());
                sendMsg.setBytes(bytes);

                sendMsgCache.put(e, sendMsg);
            }
        });
    }
}
