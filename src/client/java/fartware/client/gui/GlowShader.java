package fartware.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import fartware.Fartware;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.renderer.ShaderInstance;

/**
 * Draws the glow around the players in the theme colours, from a blurred and a sharp copy of
 * their silhouette. The shader is assets/fartware/shaders/core/glow.*
 */
public final class GlowShader {
	private static ShaderInstance shader;

	private GlowShader() {
	}

	public static void register() {
		CoreShaderRegistrationCallback.EVENT.register(context ->
			context.register(Fartware.id("glow"), DefaultVertexFormat.POSITION, loaded -> shader = loaded));
	}

	public static boolean isReady() {
		return shader != null;
	}

	/** Adds the glow on top of the target's current contents. The glow is additive, so it brightens what is behind it. */
	public static void composite(RenderTarget target, int blurredTexture, int silhouetteTexture, float strength) {
		if (shader == null) {
			return;
		}
		target.bindWrite(true);
		BreatheShader.configure(shader, 1f);
		shader.safeGetUniform("Strength").set(strength);

		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

		RenderSystem.setShader(() -> shader);
		RenderSystem.setShaderTexture(0, blurredTexture);
		RenderSystem.setShaderTexture(1, silhouetteTexture);

		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
		buffer.addVertex(-1f, -1f, 0f);
		buffer.addVertex(1f, -1f, 0f);
		buffer.addVertex(1f, 1f, 0f);
		buffer.addVertex(-1f, 1f, 0f);
		BufferUploader.drawWithShader(buffer.buildOrThrow());

		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
	}
}
