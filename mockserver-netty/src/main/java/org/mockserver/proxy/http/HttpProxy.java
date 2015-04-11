package org.mockserver.proxy.http;

import com.google.common.util.concurrent.SettableFuture;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.filters.LogFilter;
import org.mockserver.proxy.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class should not be constructed directly instead use HttpProxyBuilder to build and configure this class
 *
 * @see org.mockserver.proxy.ProxyBuilder
 *
 * @author jamesdbloom
 */
public class HttpProxy implements Proxy {

    private static final Logger logger = LoggerFactory.getLogger(HttpProxy.class);
    // proxy
    private final LogFilter logFilter = new LogFilter();
    private final SettableFuture<String> hasStarted;
    // netty
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private Channel channel;

    /**
     * Start the instance using the ports provided
     *
     * @param port the http port to use
     */
    public HttpProxy(final Integer port) {
        if (port == null) {
            throw new IllegalArgumentException("You must specify a port");
        }

        hasStarted = SettableFuture.create();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    channel = new ServerBootstrap()
                            .group(bossGroup, workerGroup)
                            .option(ChannelOption.SO_BACKLOG, 1024)
                            .channel(NioServerSocketChannel.class)
                            .childOption(ChannelOption.AUTO_READ, true)
                            .childHandler(new HttpProxyUnificationHandler())
                            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                            .childAttr(HTTP_PROXY, HttpProxy.this)
                            .childAttr(REMOTE_SOCKET, new InetSocketAddress(port))
                            .childAttr(LOG_FILTER, logFilter)
                            .bind(port)
                            .sync()
                            .channel();

                    logger.info("MockServer proxy started on port: {}", ((InetSocketAddress) channel.localAddress()).getPort());

                    proxyStarted(port);
                    hasStarted.set("STARTED");

                    channel.closeFuture().sync();
                } catch (InterruptedException ie) {
                    logger.error("MockServer proxy receive InterruptedException", ie);
                } finally {
                    bossGroup.shutdownGracefully(0, 1, TimeUnit.MILLISECONDS);
                    workerGroup.shutdownGracefully(0, 1, TimeUnit.MILLISECONDS);
                }
            }
        }).start();

        try {
            hasStarted.get();
        } catch (Exception e) {
            logger.warn("Exception while waiting for MockServer proxy to complete starting up", e);
        }
    }

    public void stop() {
        try {
            proxyStopping();
            bossGroup.shutdownGracefully(0, 1, TimeUnit.MILLISECONDS);
            workerGroup.shutdownGracefully(0, 1, TimeUnit.MILLISECONDS);
            channel.close();
            // wait for socket to be released
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (Exception ie) {
            logger.trace("Exception while stopping MockServer proxy", ie);
        }
    }

    public boolean isRunning() {
        if (hasStarted.isDone()) {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                logger.trace("Exception while waiting for the proxy to confirm running status", e);
            }
            return !bossGroup.isShuttingDown() && !workerGroup.isShuttingDown();
        } else {
            return false;
        }
    }

    public Integer getPort() {
        return ((InetSocketAddress) channel.localAddress()).getPort();
    }

    private static ProxySelector previousProxySelector;

    private static ProxySelector createProxySelector(final String host, final int port) {
        return new ProxySelector() {
            @Override
            public List<java.net.Proxy> select(URI uri) {
                return Arrays.asList(
                        new java.net.Proxy(java.net.Proxy.Type.SOCKS, new InetSocketAddress(host, port)),
                        new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(host, port))
                );
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                logger.error("Connection could not be established to proxy at socket [" + sa + "]", ioe);
            }
        };
    }

    protected void proxyStarted(Integer port) {
        ConfigurationProperties.proxyPort(port);
        System.setProperty("proxySet", "true");
//        System.setProperty("socksProxyHost", "127.0.0.1");
//        System.setProperty("socksProxyPort", port.toString());
//        System.setProperty("socksProxyVersion", "5");
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", port.toString());
//        System.setProperty("https.proxyHost", "127.0.0.1");
//        System.setProperty("https.proxyPort", port.toString());
//        previousProxySelector = ProxySelector.getDefault();
//        ProxySelector.setDefault(createProxySelector("127.0.0.1", port));
//        System.setProperty("socksProxySet", "true");
    }

    protected void proxyStopping() {
        System.clearProperty("proxySet");
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
//        ProxySelector.setDefault(previousProxySelector);
    }
}
