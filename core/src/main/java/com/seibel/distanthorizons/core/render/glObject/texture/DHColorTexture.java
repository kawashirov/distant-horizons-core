package com.seibel.distanthorizons.core.render.glObject.texture;

import org.joml.Vector2i;
import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL13C;
import org.lwjgl.opengl.GL43C;

import java.nio.ByteBuffer;

public class DHColorTexture
{
	private final DHInternalTextureFormat internalFormat;
	private final DHPixelFormat format;
	private final DHPixelType type;
	private int width;
	private int height;

	private boolean isValid;
	private final int texture;

	private static final ByteBuffer NULL_BUFFER = null;

	public DHColorTexture(Builder builder) {
		this.isValid = true;

		this.internalFormat = builder.internalFormat;
		this.format = builder.format;
		this.type = builder.type;

		this.width = builder.width;
		this.height = builder.height;

		this.texture = GL43C.glGenTextures();

		boolean isPixelFormatInteger = builder.internalFormat.getPixelFormat().isInteger();
		setupTexture(texture, builder.width, builder.height, !isPixelFormatInteger);

		// Clean up after ourselves
		// This is strictly defensive to ensure that other buggy code doesn't tamper with our textures
		GL43C.glBindTexture(GL43C.GL_TEXTURE_2D, 0);
	}

	private void setupTexture(int texture, int width, int height, boolean allowsLinear) {
		resizeTexture(texture, width, height);

		GL43C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MIN_FILTER, allowsLinear ? GL11C.GL_LINEAR : GL11C.GL_NEAREST);
		GL43C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_MAG_FILTER, allowsLinear ? GL11C.GL_LINEAR : GL11C.GL_NEAREST);
		GL43C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_S, GL13C.GL_CLAMP_TO_EDGE);
		GL43C.glTexParameteri(GL11C.GL_TEXTURE_2D, GL11C.GL_TEXTURE_WRAP_T, GL13C.GL_CLAMP_TO_EDGE);
	}

	private void resizeTexture(int texture, int width, int height) {
		GL43C.glBindTexture(GL43C.GL_TEXTURE_2D, texture);
		GL43C.glTexImage2D(GL11C.GL_TEXTURE_2D, 0, internalFormat.getGlFormat(), width, height, 0, format.getGlFormat(), type.getGlFormat(), NULL_BUFFER);
	}

	void resize(Vector2i textureScaleOverride) {
		this.resize(textureScaleOverride.x, textureScaleOverride.y);
	}

	// Package private, call CompositeRenderTargets#resizeIfNeeded instead.
	public void resize(int width, int height) {
		requireValid();

		this.width = width;
		this.height = height;

		resizeTexture(texture, width, height);
	}

	public DHInternalTextureFormat getInternalFormat() {
		return internalFormat;
	}

	public int getTexture() {
		requireValid();

		return texture;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void destroy() {
		requireValid();
		isValid = false;

		GL43C.glDeleteTextures(texture);
	}

	private void requireValid() {
		if (!isValid) {
			throw new IllegalStateException("Attempted to use a deleted composite render target");
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private DHInternalTextureFormat internalFormat = DHInternalTextureFormat.RGBA8;
		private int width = 0;
		private int height = 0;
		private DHPixelFormat format = DHPixelFormat.RGBA;
		private DHPixelType type = DHPixelType.UNSIGNED_BYTE;

		private Builder() {
			// No-op
		}

		public Builder setInternalFormat(DHInternalTextureFormat format) {
			this.internalFormat = format;

			return this;
		}

		public Builder setDimensions(int width, int height) {
			if (width <= 0) {
				throw new IllegalArgumentException("Width must be greater than zero");
			}

			if (height <= 0) {
				throw new IllegalArgumentException("Height must be greater than zero");
			}

			this.width = width;
			this.height = height;

			return this;
		}

		public Builder setPixelFormat(DHPixelFormat pixelFormat) {
			this.format = pixelFormat;

			return this;
		}

		public Builder setPixelType(DHPixelType pixelType) {
			this.type = pixelType;

			return this;
		}

		public DHColorTexture build() {
			return new DHColorTexture(this);
		}
	}
}
