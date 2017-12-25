package org.mockserver.proxy.direct;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import org.mockserver.mockserver.callback.WebSocketServerHandler;
import org.mockserver.proxy.http.HttpProxyHandler;
import org.mockserver.server.netty.codec.MockServerServerCodec;
import org.mockserver.unification.PortUnificationHandler;

import static org.mockserver.mock.HttpStateHandler.STATE_HANDLER;
import static org.mockserver.proxy.Proxy.HTTP_PROXY;

/**
 * @author jamesdbloom
 */
@ChannelHandler.Sharable
public class DirectProxyUnificationHandler extends PortUnificationHandler {

    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, ChannelPipeline pipeline) {
        pipeline.addLast(new WebSocketServerHandler(ctx.channel().attr(STATE_HANDLER).get().getWebSocketClientRegistry()));
        pipeline.addLast(new MockServerServerCodec(isSslEnabledDownstream(ctx.channel())));

        pipeline.addLast(new HttpProxyHandler(
            ctx.channel().attr(HTTP_PROXY).get(),
            ctx.channel().attr(STATE_HANDLER).get()
        ));
    }

}
