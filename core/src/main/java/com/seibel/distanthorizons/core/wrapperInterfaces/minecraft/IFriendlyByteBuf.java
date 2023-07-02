package com.seibel.distanthorizons.core.wrapperInterfaces.minecraft;

import java.nio.charset.Charset;

/**
 * Interface that wraps the net.minecraft.network.FriendlyByteBuffer.
 */
public interface IFriendlyByteBuf {

    short readShort();

    CharSequence readCharSequence(int length, Charset charset);
}
