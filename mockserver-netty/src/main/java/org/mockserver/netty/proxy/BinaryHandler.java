package org.mockserver.netty.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.mockserver.client.NettyHttpClient;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.log.model.LogEntry;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.model.BinaryMessage;
import org.mockserver.scheduler.Scheduler;
import org.slf4j.event.Level;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mockserver.configuration.ConfigurationProperties.maxFutureTimeout;
import static org.mockserver.exception.ExceptionHandling.closeOnFlush;
import static org.mockserver.exception.ExceptionHandling.connectionClosedException;
import static org.mockserver.log.model.LogEntry.LogMessageType.FORWARDED_REQUEST;
import static org.mockserver.log.model.LogEntry.LogMessageType.RECEIVED_REQUEST;
import static org.mockserver.mock.action.ActionHandler.getRemoteAddress;
import static org.mockserver.model.BinaryMessage.bytes;
import static org.mockserver.netty.unification.PortUnificationHandler.isSslEnabledUpstream;

/**
 * @author jamesdbloom
 */
@ChannelHandler.Sharable
public class BinaryHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private MockServerLogger mockServerLogger;
    private final Scheduler scheduler;
    private final NettyHttpClient httpClient;

    public BinaryHandler(final MockServerLogger mockServerLogger, final Scheduler scheduler, final NettyHttpClient httpClient) {
        super(false);
        this.mockServerLogger = mockServerLogger;
        this.scheduler = scheduler;
        this.httpClient = httpClient;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) {
        BinaryMessage binaryRequest = bytes(ByteBufUtil.getBytes(byteBuf));
        mockServerLogger.logEvent(
            new LogEntry()
                .setType(RECEIVED_REQUEST)
                .setLogLevel(Level.INFO)
                .setMessageFormat("received binary request:{}")
                .setArguments(ByteBufUtil.hexDump(binaryRequest.getBytes()))
        );
        final InetSocketAddress remoteAddress = getRemoteAddress(ctx);
        if (remoteAddress != null) {
            boolean synchronous = true;
            CompletableFuture<BinaryMessage> binaryResponseFuture = httpClient.sendRequest(binaryRequest, isSslEnabledUpstream(ctx.channel()), remoteAddress, ConfigurationProperties.socketConnectionTimeout());
            scheduler.submit(binaryResponseFuture, () -> {
                try {
                    BinaryMessage binaryResponse = binaryResponseFuture.get(maxFutureTimeout(), MILLISECONDS);
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setType(FORWARDED_REQUEST)
                            .setLogLevel(Level.INFO)
                            .setMessageFormat("returning binary response:{}from:{}for forwarded binary request:{}")
                            .setArguments(ByteBufUtil.hexDump(binaryResponse.getBytes()), remoteAddress, ByteBufUtil.hexDump(binaryRequest.getBytes()))
                    );
                    ctx.writeAndFlush(Unpooled.copiedBuffer(binaryResponse.getBytes()));
                } catch (Throwable throwable) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(Level.WARN)
                            .setMessageFormat("exception " + throwable.getMessage() + " sending hex{}to{}closing connection")
                            .setArguments(ByteBufUtil.hexDump(binaryRequest.getBytes()), remoteAddress)
                            .setThrowable(throwable)
                    );
                    ctx.close();
                }
            }, synchronous);
        } else {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.INFO)
                    .setMessageFormat("unknown message format{}")
                    .setArguments(ByteBufUtil.hexDump(binaryRequest.getBytes()))
            );
            ctx.writeAndFlush(Unpooled.copiedBuffer("unknown message format".getBytes(StandardCharsets.UTF_8)));
            ctx.close();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (connectionClosedException(cause)) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("exception caught by " + this.getClass() + " handler -> closing pipeline " + ctx.channel())
                    .setThrowable(cause)
            );
        }
        closeOnFlush(ctx.channel());
    }
}
