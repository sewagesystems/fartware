package fartware.client.gui;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import fartware.Fartware;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/**
 * Blur effects built on a separable Gaussian shader (assets/fartware/shaders/core/blur.*).
 * <ul>
 *   <li>{@link #apply} blurs everything already drawn to the screen, as a background behind the click GUI.</li>
 *   <li>{@link #capture} keeps a blurred copy of the frame, and {@link #fillFrosted} then draws rounded
 *       rectangles of that copy (frost.*), dimmed, for frosted glass panels such as the array list background.</li>
 * </ul>
 * The frame is shrunk to half size into two scratch targets, blurred for a few rounds and drawn back.
 */
public final class BlurShader {
	private static final int BACKGROUND_ROUNDS = 2;
	private static final int CAPTURE_ROUNDS = 1;

	private static ShaderInstance shader;
	private static ShaderInstance frostShader;
	private static RenderTarget targetA;
	private static RenderTarget targetB;
	private static boolean captured;

	private BlurShader() {
	}

	public static void register() {
		CoreShaderRegistrationCallback.EVENT.register(context -> {
			context.register(Fartware.id("blur"), DefaultVertexFormat.POSITION, loaded -> shader = loaded);
			context.register(Fartware.id("frost"), DefaultVertexFormat.POSITION, loaded -> frostShader = loaded);
		});
	}

	/**
	 * Blurs the whole screen in place.
	 *
	 * @param strength   blur spread, 0 disables the effect
	 * @param brightness 1.0 leaves the colours alone, lower values darken the blurred background
	 */
	public static void apply(GuiGraphics graphics, float strength, float brightness) {
		if (shader == null || strength <= 0f) {
			return;
		}
		graphics.flush();
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		blurFrame(main, strength, BACKGROUND_ROUNDS);

		// A zero direction makes the shader copy the blurred image back onto the screen.
		pass(targetB.getColorTextureId(), targetB.width, targetB.height, main, 0f, 0f, 0f, brightness);
		restoreState();
	}

	/**
	 * Blurs any render target (the screen is not changed) and returns the id of the texture holding the result,
	 * which stays valid until the next blur. Returns -1 if the blur isn't available.
	 */
	public static int blurTarget(RenderTarget source, float strength) {
		if (shader == null || strength <= 0f) {
			return -1;
		}
		blurFrame(source, strength, BACKGROUND_ROUNDS);
		restoreState();
		return targetB.getColorTextureId();
	}

	/**
	 * Takes a blurred copy of the current frame for {@link #fillFrosted} to draw from, without changing the screen.
	 *
	 * @return false if the blur isn't available (the shader failed to load or the strength is 0)
	 */
	public static boolean capture(GuiGraphics graphics, float strength) {
		captured = false;
		if (shader == null || frostShader == null || strength <= 0f) {
			return false;
		}
		graphics.flush();
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		blurFrame(main, strength, CAPTURE_ROUNDS);

		main.bindWrite(true);
		restoreState();
		captured = true;
		return true;
	}

	/**
	 * Draws a rounded rectangle showing the blurred frame from the last {@link #capture}, darkened by brightness.
	 * Falls back to a plain translucent black fill if there is no capture. Radii are in GUI units, in the
	 * order top left, top right, bottom right, bottom left.
	 */
	public static void fillFrosted(GuiGraphics graphics, int x0, int y0, int x1, int y1, float brightness,
								   float topLeft, float topRight, float bottomRight, float bottomLeft) {
		if (!captured || frostShader == null) {
			BreatheShader.fillSolid(graphics, x0, y0, x1, y1, 0x90000000, topLeft, topRight, bottomRight, bottomLeft);
			return;
		}
		graphics.flush();
		BreatheShader.setRect(frostShader, graphics, x0, y0, x1, y1, topLeft, topRight, bottomRight, bottomLeft);
		frostShader.safeGetUniform("Brightness").set(brightness);

		RenderSystem.setShader(() -> frostShader);
		RenderSystem.setShaderTexture(0, targetB.getColorTextureId());
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		Matrix4f matrix = graphics.pose().last().pose();
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
		buffer.addVertex(matrix, x0, y0, 0f);
		buffer.addVertex(matrix, x0, y1, 0f);
		buffer.addVertex(matrix, x1, y1, 0f);
		buffer.addVertex(matrix, x1, y0, 0f);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	/** Blurs the main target into the scratch targets. The result is left in targetB. */
	private static void blurFrame(RenderTarget main, float strength, int rounds) {
		int width = Math.max(1, main.width / 2);
		int height = Math.max(1, main.height / 2);
		ensureTargets(width, height);

		RenderSystem.disableBlend();
		RenderSystem.disableDepthTest();
		main.setFilterMode(GL11.GL_LINEAR);

		float radius = strength * 0.5f;
		// Shrink to half size while blurring horizontally, then blur vertically.
		pass(main.getColorTextureId(), main.width, main.height, targetA, 1f, 0f, radius, 1f);
		pass(targetA.getColorTextureId(), width, height, targetB, 0f, 1f, radius, 1f);
		for (int i = 1; i < rounds; i++) {
			pass(targetB.getColorTextureId(), width, height, targetA, 1f, 0f, radius, 1f);
			pass(targetA.getColorTextureId(), width, height, targetB, 0f, 1f, radius, 1f);
		}

		main.setFilterMode(GL11.GL_NEAREST);
	}

	private static void restoreState() {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
	}

	private static void ensureTargets(int width, int height) {
		if (targetA == null) {
			targetA = new TextureTarget(width, height, false, false);
			targetB = new TextureTarget(width, height, false, false);
		} else if (targetA.width != width || targetA.height != height) {
			targetA.resize(width, height, false);
			targetB.resize(width, height, false);
		}
		targetA.setFilterMode(GL11.GL_LINEAR);
		targetB.setFilterMode(GL11.GL_LINEAR);
	}

	private static void pass(int sourceTexture, int sourceWidth, int sourceHeight, RenderTarget destination,
							 float directionX, float directionY, float radius, float brightness) {
		destination.bindWrite(true);

		shader.safeGetUniform("InSize").set((float) sourceWidth, (float) sourceHeight);
		shader.safeGetUniform("Direction").set(directionX, directionY);
		shader.safeGetUniform("Radius").set(radius);
		shader.safeGetUniform("Brightness").set(brightness);

		RenderSystem.setShader(() -> shader);
		RenderSystem.setShaderTexture(0, sourceTexture);

		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
		buffer.addVertex(-1f, -1f, 0f);
		buffer.addVertex(1f, -1f, 0f);
		buffer.addVertex(1f, 1f, 0f);
		buffer.addVertex(-1f, 1f, 0f);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}
}
