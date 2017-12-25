package org.mockserver.proxy.socks;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socks.*;
import org.mockserver.mock.action.ActionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockserver.exception.ExceptionHandler.shouldIgnoreException;
import static org.mockserver.proxy.Proxy.PROXYING;
import static org.mockserver.socket.KeyAndCertificateFactory.addSubjectAlternativeName;
import static org.mockserver.unification.PortUnificationHandler.disableSslDownstream;
import static org.mockserver.unification.PortUnificationHandler.enabledSslDownstream;

public class SocksProxyHandler extends SimpleChannelInboundHandler<SocksRequest> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public SocksProxyHandler() {
        super(false);
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, SocksRequest socksRequest) {
        switch (socksRequest.requestType()) {

            case INIT:

                ctx.pipeline().addFirst(new SocksCmdRequestDecoder());
                ctx.write(new SocksInitResponse(SocksAuthScheme.NO_AUTH));
                break;

            case AUTH:

                ctx.pipeline().addFirst(new SocksCmdRequestDecoder());
                ctx.write(new SocksAuthResponse(SocksAuthStatus.SUCCESS));
                break;

            case CMD:

                SocksCmdRequest req = (SocksCmdRequest) socksRequest;
                if (req.cmdType() == SocksCmdType.CONNECT) {

                    Channel channel = ctx.channel();
                    channel.attr(PROXYING).set(Boolean.TRUE);
                    if (String.valueOf(req.port()).endsWith("80")) {
                        disableSslDownstream(channel);
                    } else if (String.valueOf(req.port()).endsWith("443")) {
                        enabledSslDownstream(channel);
                    }

                    // add Subject Alternative Name for SSL certificate
                    addSubjectAlternativeName(req.host());

                    ctx.pipeline().addAfter(getClass().getSimpleName() + "#0", SocksConnectHandler.class.getSimpleName() + "#0", new SocksConnectHandler(req.host(), req.port()));
                    ctx.pipeline().remove(this);
                    ctx.fireChannelRead(socksRequest);

                } else {

                    ctx.close();

                }
                break;

            case UNKNOWN:

                ctx.close();
                break;

        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (!shouldIgnoreException(cause)) {
            logger.warn("Exception caught by SOCKS proxy handler -> closing pipeline " + ctx.channel(), cause);
        }
        ctx.close();
    }
}
