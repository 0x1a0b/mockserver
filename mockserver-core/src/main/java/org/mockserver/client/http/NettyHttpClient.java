package org.mockserver.client.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.mockserver.client.serialization.ObjectMapperFactory;
import org.mockserver.mappers.URIMapper;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static org.mockserver.model.HttpRequest.request;

public class NettyHttpClient {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static void main(String[] args) throws Exception {
        NettyHttpClient nettyHttpClient = new NettyHttpClient();
        nettyHttpClient.sendRequest(request().withURL("http://www.london-squash-league.com/login;jsessionid=A2F3AC58C0EA1E6D758FD05934806B91"));
    }

    public HttpResponse sendRequest(final HttpRequest httpRequest) {
        logger.debug("Sending request: " + httpRequest);

        // determine request details
        URI uri = URIMapper.getURI(httpRequest);
        boolean secure = "https".equalsIgnoreCase(uri.getScheme());

        // configure the client
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            HttpClientInitializer channelInitializer = new HttpClientInitializer(secure);

            // make the connection attempt
            Channel channel = new Bootstrap().group(group)
                    .channel(NioSocketChannel.class)
                    .handler(channelInitializer)
                    .connect(uri.getHost(), uri.getPort()).sync().channel();

            // logging
            if (logger.isTraceEnabled()) {
                logger.trace("Proxy sending request:" + System.getProperty("line.separator") + ObjectMapperFactory.createObjectMapper().writeValueAsString(httpRequest));
            }

            // send the HTTP request
            channel.writeAndFlush(httpRequest);

            // wait for response
            HttpResponse httpResponse = channelInitializer.getResponseFuture().get();
            logger.debug("Received response: " + httpResponse);

            // shutdown client
            group.shutdownGracefully(2, 15, TimeUnit.MILLISECONDS);

            return httpResponse;

        } catch (Exception e) {
            throw new RuntimeException("Exception while sending request", e);
        } finally {
            // shut down executor threads to exit
            group.shutdownGracefully(2, 15, TimeUnit.MILLISECONDS);
        }
    }
}
