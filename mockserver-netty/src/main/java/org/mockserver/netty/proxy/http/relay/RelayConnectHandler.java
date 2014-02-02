package org.mockserver.netty.proxy.http.relay;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import org.mockserver.netty.logging.LoggingHandler;
import org.mockserver.netty.proxy.interceptor.RequestInterceptor;
import org.mockserver.netty.proxy.interceptor.ResponseInterceptor;
import org.mockserver.socket.SSLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;

@ChannelHandler.Sharable
public abstract class RelayConnectHandler<T> extends SimpleChannelInboundHandler<T> {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Bootstrap bootstrap = new Bootstrap();
    private final InetSocketAddress connectSocket;

    public RelayConnectHandler(InetSocketAddress connectSocket) {
        if (connectSocket == null) throw new IllegalArgumentException("Connect Socket can not be null");
        this.connectSocket = connectSocket;
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final T request) throws Exception {
        final Promise<Channel> promise = ctx.executor().newPromise();
        promise.addListener(
                new GenericFutureListener<Future<Channel>>() {
                    @Override
                    public void operationComplete(final Future<Channel> future) throws Exception {
                        final Channel outboundChannel = future.getNow();
                        if (future.isSuccess()) {
                            ctx.channel().writeAndFlush(successResponse(request))
                                    .addListener(new ChannelFutureListener() {
                                        @Override
                                        public void operationComplete(ChannelFuture channelFuture) throws Exception {
                                            removeCodecSupport(ctx);
                                            // downstream
                                            SSLEngine clientEngine = SSLFactory.sslContext().createSSLEngine();
                                            clientEngine.setUseClientMode(true);
                                            outboundChannel.pipeline().addLast("outbound relay ssl", new SslHandler(clientEngine));
                                            if (logger.isDebugEnabled()) {
                                                outboundChannel.pipeline().addLast("outbound relay logger", new LoggingHandler("                -->"));
                                            }
                                            outboundChannel.pipeline().addLast(new ProxyRelayHandler(ctx.channel(), 1048576, new RequestInterceptor(), "                -->"));
                                            // upstream
                                            SSLEngine serverEngine = SSLFactory.sslContext().createSSLEngine();
                                            serverEngine.setUseClientMode(false);
                                            ctx.channel().pipeline().addLast("upstream relay ssl", new SslHandler(serverEngine));
                                            if (logger.isDebugEnabled()) {
                                                ctx.channel().pipeline().addLast("upstream relay logger", new LoggingHandler("<-- "));
                                            }
                                            ctx.channel().pipeline().addLast(new ProxyRelayHandler(outboundChannel, 1048576, new ResponseInterceptor(), "<-- "));
                                        }
                                    });
                        } else {
                            failure("Failed to activate handler and retrieve channel", future.cause(), ctx, failureResponse(request));
                        }
                    }
                });

        final Channel inboundChannel = ctx.channel();
        bootstrap.group(inboundChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline channelPipeline = socketChannel.pipeline();
                        if (logger.isDebugEnabled()) {
                            channelPipeline.addLast("direct client logger", new LoggingHandler("<-->"));
                        }
                        channelPipeline.addLast(DirectClientHandler.class.getSimpleName(), new DirectClientHandler(promise));
                    }
                });

        bootstrap.connect(connectSocket).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    failure("Connection failed to 127.0.0.1:" + connectSocket, future.cause(), ctx, failureResponse(request));
                }
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        failure("Exception caught", cause, ctx, failureResponse(null));
    }

    private void failure(String message, Throwable cause, ChannelHandlerContext ctx, Object response) {
        logger.warn(message, cause);
        Channel channel = ctx.channel();
        channel.writeAndFlush(response);
        if (channel.isActive()) {
            channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    protected abstract void removeCodecSupport(ChannelHandlerContext ctx);

    protected abstract Object successResponse(Object request);

    protected abstract Object failureResponse(Object request);
}
