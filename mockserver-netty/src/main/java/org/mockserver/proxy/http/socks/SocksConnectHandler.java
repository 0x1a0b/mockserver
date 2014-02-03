package org.mockserver.proxy.http.socks;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socks.SocksAddressType;
import io.netty.handler.codec.socks.SocksCmdRequest;
import io.netty.handler.codec.socks.SocksCmdResponse;
import io.netty.handler.codec.socks.SocksCmdStatus;
import org.mockserver.proxy.http.relay.RelayConnectHandler;

import java.net.InetSocketAddress;

@ChannelHandler.Sharable
public final class SocksConnectHandler extends RelayConnectHandler<SocksCmdRequest> {

    public SocksConnectHandler(InetSocketAddress connectSocket, boolean secure) {
        super(connectSocket, secure);
    }

    protected void removeCodecSupport(ChannelHandlerContext ctx) {
        ctx.pipeline().remove(this);
    }

    protected Object successResponse(Object request) {
        if (request != null && request instanceof SocksCmdRequest) {
            return new SocksCmdResponse(SocksCmdStatus.SUCCESS, ((SocksCmdRequest) request).addressType());
        } else {
            return new SocksCmdResponse(SocksCmdStatus.SUCCESS, SocksAddressType.UNKNOWN);
        }
    }

    protected Object failureResponse(Object request) {
        if (request != null && request instanceof SocksCmdRequest) {
            return new SocksCmdResponse(SocksCmdStatus.FAILURE, ((SocksCmdRequest) request).addressType());
        } else {
            return new SocksCmdResponse(SocksCmdStatus.FAILURE, SocksAddressType.UNKNOWN);
        }
    }
}
