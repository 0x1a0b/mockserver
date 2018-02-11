package org.mockserver.lifecycle;

import com.google.common.util.concurrent.SettableFuture;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.logging.MockServerLogger;
import org.mockserver.mock.HttpStateHandler;
import org.mockserver.scheduler.Scheduler;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author jamesdbloom
 */
public abstract class LifeCycle<T extends LifeCycle> {

    static {
        new MockServerLogger();
    }

    protected final MockServerLogger mockServerLogger;
    protected EventLoopGroup bossGroup = new NioEventLoopGroup(ConfigurationProperties.nioEventLoopThreadCount());
    protected EventLoopGroup workerGroup = new NioEventLoopGroup(ConfigurationProperties.nioEventLoopThreadCount());
    protected ServerBootstrap serverBootstrap;
    protected HttpStateHandler httpStateHandler;
    private List<Future<Channel>> channelOpenedFutures = new ArrayList<>();
    //    private SettableFuture<String> stopping = SettableFuture.create();
    private Scheduler scheduler = new Scheduler();

    protected LifeCycle() {
        this.httpStateHandler = new HttpStateHandler(scheduler);
        this.mockServerLogger = httpStateHandler.getMockServerLogger();
    }

    public void stop() {
        scheduler.shutdown();

        // Shut down all event loops to terminate all threads.
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();

        // Wait until all threads are terminated.
        bossGroup.terminationFuture().syncUninterruptibly();
        workerGroup.terminationFuture().syncUninterruptibly();
    }

    public boolean isRunning() {
        return !bossGroup.isShuttingDown() || !workerGroup.isShuttingDown();
    }

    public List<Integer> getLocalPorts() {
        List<Integer> ports = new ArrayList<>();
        for (Future<Channel> channelOpened : channelOpenedFutures) {
            try {
                ports.add(((InetSocketAddress) channelOpened.get(3, TimeUnit.SECONDS).localAddress()).getPort());
            } catch (Exception e) {
                mockServerLogger.trace("Exception while retrieving port from channel future, ignoring port for this channel", e);
            }
        }
        return ports;
    }

    /**
     * @deprecated use getLocalPort instead of getPort
     */
    @Deprecated
    public Integer getPort() {
        return getLocalPort();
    }

    public int getLocalPort() {
        for (Future<Channel> channelOpened : channelOpenedFutures) {
            try {
                return ((InetSocketAddress) channelOpened.get(2, TimeUnit.SECONDS).localAddress()).getPort();
            } catch (Exception e) {
                mockServerLogger.trace("Exception while retrieving port from channel future, ignoring port for this channel", e);
            }
        }
        return -1;
    }

    public List<Integer> bindToPorts(final List<Integer> requestedPortBindings) {
        List<Integer> actualPortBindings = new ArrayList<>();
        for (final Integer portToBind : requestedPortBindings) {
            try {
                final SettableFuture<Channel> channelOpened = SettableFuture.create();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        channelOpenedFutures.add(channelOpened);
                        try {

                            Channel channel =
                                serverBootstrap
                                    .bind(portToBind)
                                    .addListener(new ChannelFutureListener() {
                                        @Override
                                        public void operationComplete(ChannelFuture future) {
                                            if (future.isSuccess()) {
                                                channelOpened.set(future.channel());
                                            } else {
                                                channelOpened.setException(future.cause());
                                            }
                                        }
                                    })
                                    .channel();

                            started(((InetSocketAddress) channelOpened.get().localAddress()).getPort());
                            channel.closeFuture().syncUninterruptibly();

                        } catch (Exception e) {
                            throw new RuntimeException("Exception while binding MockServer to port " + portToBind, e.getCause());
                        }
                    }
                }, "MockServer thread for port: " + portToBind).start();

                actualPortBindings.add(((InetSocketAddress) channelOpened.get().localAddress()).getPort());
            } catch (Exception e) {
                throw new RuntimeException("Exception while binding MockServer to port " + portToBind, e.getCause());
            }
        }
        return actualPortBindings;
    }

    private void started(Integer port) {
        mockServerLogger.info("MockServer started on port: {}", port);
    }

}
