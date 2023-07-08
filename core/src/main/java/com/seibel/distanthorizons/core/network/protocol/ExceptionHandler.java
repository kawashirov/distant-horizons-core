package com.seibel.distanthorizons.core.network.protocol;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.Logger;

public class ExceptionHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Exception caught on channel", cause);
        ctx.close();
    }
}
