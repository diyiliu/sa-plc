package com.tiza.gw.netty.server;

import com.tiza.gw.netty.handler.DtuHandler;
import com.tiza.gw.netty.handler.codec.DtuDecoder;
import com.tiza.gw.netty.handler.codec.DtuEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Description: DtuServer
 * Author: DIYILIU
 * Update: 2018-01-26 10:38
 */

@Slf4j
@Setter
public class DtuServer extends Thread {
    private int port;

    public void init(){

        this.start();
    }

    @Override
    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();

            b.group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1000)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch)  {
                            ch.pipeline().addLast(new DtuEncoder())
                                    .addLast(new DtuDecoder())
                                    .addLast(new DtuHandler());
                        }
                    });

            ChannelFuture f = b.bind(port).sync();
            log.info("DTU网关服务器启动, 端口[{}]...", port);
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}
