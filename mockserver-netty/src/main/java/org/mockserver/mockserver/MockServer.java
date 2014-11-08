package org.mockserver.mockserver;

import com.google.common.util.concurrent.SettableFuture;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.mockserver.mock.MockServerMatcher;
import org.mockserver.filters.LogFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * An HTTP server that sends back the content of the received HTTP request
 * in a pretty plaintext form.
 */
public class MockServer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    // mockserver
    private final LogFilter logFilter = new LogFilter();
    private final MockServerMatcher mockServerMatcher = new MockServerMatcher();
    private SettableFuture<String> hasStarted;
    // netty
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    /**
     * Start the instance using the ports provided
     *
     * @param port the http port to use
     * @param securePort the secure https port to use
     */
    public Thread start(final Integer port, final Integer securePort) {
        if (port == null && securePort == null) {
            throw new IllegalStateException("You must specify a port or a secure port");
        }

        hasStarted = SettableFuture.create();
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        Thread mockServerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    logger.info("MockServer starting up"
                                    + (port != null ? " serverPort " + port : "")
                                    + (securePort != null ? " secureServerPort " + securePort : "")
                    );

                    Channel httpChannel = null;
                    if (port != null) {
                        httpChannel = new ServerBootstrap()
                                .option(ChannelOption.SO_BACKLOG, 1024)
                                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                                .group(bossGroup, workerGroup)
                                .channel(NioServerSocketChannel.class)
                                .childHandler(new MockServerInitializer(mockServerMatcher, logFilter, MockServer.this, false))
                                .bind(port)
                                .sync()
                                .channel();
                    }

                    Channel httpsChannel = null;
                    if (securePort != null) {
                        httpsChannel = new ServerBootstrap()
                                .option(ChannelOption.SO_BACKLOG, 1024)
                                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                                .group(bossGroup, workerGroup)
                                .channel(NioServerSocketChannel.class)
                                .childHandler(new MockServerInitializer(mockServerMatcher, logFilter, MockServer.this, true))
                                .bind(securePort)
                                .sync()
                                .channel();
                    }

                    hasStarted.set("STARTED");

                    if (httpChannel != null) {
                        httpChannel.closeFuture().sync();
                    }
                    if (httpsChannel != null) {
                        httpsChannel.closeFuture().sync();
                    }
                } catch (InterruptedException ie) {
                    logger.error("MockServer receive InterruptedException", ie);
                } finally {
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                }
            }
        });
        mockServerThread.start();

        try {
            // wait for proxy to start all channels
            hasStarted.get();
        } catch (Exception e) {
            logger.debug("Exception while waiting for proxy to complete starting up", e);
        }

        return mockServerThread;
    }

    public void stop() {
        try {
            workerGroup.shutdownGracefully(1, 3, TimeUnit.SECONDS);
            bossGroup.shutdownGracefully(1, 3, TimeUnit.SECONDS);
            // wait for shutdown
            TimeUnit.SECONDS.sleep(3);
        } catch (Exception ie) {
            logger.trace("Exception while waiting for MockServer to stop", ie);
        }
    }

    public boolean isRunning() {
        if (hasStarted.isDone()) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return !bossGroup.isShuttingDown() && !workerGroup.isShuttingDown();
        } else {
            return false;
        }
    }
}
