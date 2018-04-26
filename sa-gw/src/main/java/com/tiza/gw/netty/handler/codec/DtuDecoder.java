package com.tiza.gw.netty.handler.codec;

import com.diyiliu.plugin.cache.ICache;
import com.diyiliu.plugin.util.CommonUtil;
import com.diyiliu.plugin.util.SpringUtil;
import com.tiza.gw.support.client.KafkaClient;
import com.tiza.gw.support.config.Constant;
import com.tiza.gw.support.model.MsgMemory;
import com.tiza.gw.support.model.SendMsg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Description: DtuDecoder
 * Author: DIYILIU
 * Update: 2018-01-26 10:41
 */

@Slf4j
public class DtuDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 4) {

            return;
        }

        // 绑定数据
        Attribute attribute = ctx.channel().attr(AttributeKey.valueOf(Constant.NETTY_DEVICE_ID));
        // 设备缓存
        ICache deviceCache = SpringUtil.getBean("deviceCacheProvider");

        KafkaClient kafkaClient = SpringUtil.getBean("kafkaClient");

        in.markReaderIndex();
        byte b1 = in.readByte();
        byte b2 = in.readByte();
        byte b3 = in.readByte();

        String deviceId;
        if (b1 == b2 && b1 == b3) {
            byte[] content = new byte[in.readableBytes()];
            in.readBytes(content);

            deviceId = new String(content);
            byte[] bytes = Unpooled.copiedBuffer(new byte[]{b1, b2, b2}, content).array();

            // 写入kafka
            kafkaClient.toKafka(deviceId, bytes, 1);

            if (0x40 == b1) {
                register(deviceId, attribute, ctx);
            } else if (0x24 == b1) {
                // 未注册重新注册
                if (attribute.get() == null) {
                    register(deviceId, attribute, ctx);
                }
            }
        } else {
            deviceId = (String) attribute.get();
            if (deviceId == null) {
                log.error("设备未注册, 断开连接!");
                ctx.close();
                return;
            }
            in.resetReaderIndex();

            // 读取 地址(byte) + 功能码(byte)
            in.readShort();

            // 判断上行数据类型
            ICache sendCache = SpringUtil.getBean("sendCacheProvider");
            if (!sendCache.containsKey(deviceId)) {
                log.error("数据异常, 找不到下行数据与之对应。");
                ctx.close();
                return;
            }

            MsgMemory msgMemory = (MsgMemory) sendCache.get(deviceId);
            SendMsg sendMsg = msgMemory.getCurrent();

            // 指令类型
            int type = sendMsg.getType();
            byte[] content = null;
            switch (type) {
                case 0: // 查询应答
                    int length = in.readUnsignedByte();
                    if (in.readableBytes() < length + 2) {

                        in.resetReaderIndex();
                        return;
                    }
                    in.resetReaderIndex();

                    content = new byte[3 + length];
                    in.readBytes(content);
                    break;

                case 1: // 设置应答
                    if (in.readableBytes() < 6) {
                        in.resetReaderIndex();
                        return;
                    }

                    in.resetReaderIndex();
                    content = new byte[6];
                    in.readBytes(content);
                    break;
                default:
                    log.error("未知数据!");
                    ctx.close();
            }

            // CRC校验码
            byte crc0 = in.readByte();
            byte crc1 = in.readByte();

            byte[] bytes = Unpooled.copiedBuffer(content, new byte[]{crc0, crc1}).array();

            // 写入kafka
            kafkaClient.toKafka(deviceId, bytes, 1);

            byte[] checkCRC = CommonUtil.checkCRC(content);
            if (crc0 != checkCRC[0] || crc1 != checkCRC[1]) {
                log.error("CRC校验码错误, 断开连接!");
                ctx.close();
                return;
            }


            if (type == 1){
                sendMsg.setResult(1);

                return;
            }

            if (deviceCache.containsKey(deviceId)) {
                out.add(Unpooled.copiedBuffer(content));
            } else {
                log.warn("设备[{}]不存在!", deviceId);
            }
        }
    }

    /**
     * 设备注册
     *
     * @param deviceId
     * @param attribute
     * @param context
     */
    private void register(String deviceId, Attribute attribute, ChannelHandlerContext context) {
        log.info("设备[{}]注册...", deviceId);
        attribute.set(deviceId);

        ICache online = SpringUtil.getBean("onlineCacheProvider");
        online.put(deviceId, context);
    }
}
