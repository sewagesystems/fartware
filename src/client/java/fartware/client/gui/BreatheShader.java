package fartware.client.gui;

import com.mojang.blaze3d.platform.Window;
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

/**
 * Fills UI rectangles with the theme colours, animated by a shader that breathes the colours
 * across the screen. It is only used for UI elements, never as a full screen overlay.
 * Rectangles can have individually rounded corners, and the same shader can also draw a plain
 * colour with those corners. The shader lives in assets/fartware/shaders/core/breathe.*
 */
public final class BreatheShader {
	private static ShaderInstance shader;
	private static ShaderInstance textShader;

	private BreatheShader() {
	}

	public static void register() {
		CoreShaderRegistrationCallback.EVENT.register(context -> {
			context.register(Fartware.id("breathe"), DefaultVertexFormat.POSITION, loaded -> shader = loaded);
			context.register(Fartware.id("breathe_text"), DefaultVertexFormat.POSITION_TEX_COLOR, loaded -> textShader = loaded);
		});
	}

	/**
	 * Returns the glyph version of the shader with its uniforms filled in, or null if it isn't loaded.
	 * The caller binds the font texture and draws glyph quads with it.
	 */
	public static ShaderInstance prepareText() {
		if (textShader == null) {
			return null;
		}
		configure(textShader, 1f);
		return textShader;
	}

	public static void fill(GuiGraphics graphics, int x0, int y0, int x1, int y1) {
		fill(graphics, x0, y0, x1, y1, 1f);
	}

	public static void fill(GuiGraphics graphics, int x0, int y0, int x1, int y1, float alpha) {
		fill(graphics, x0, y0, x1, y1, alpha, 0f, 0f, 0f, 0f);
	}

	/**
	 * Fills the rectangle with the animated theme colours at the given opacity (0..1).
	 * Corner radii are in GUI units, in the order top left, top right, bottom right, bottom left.
	 */
	public static void fill(GuiGraphics graphics, int x0, int y0, int x1, int y1, float alpha,
							float topLeft, float topRight, float bottomRight, float bottomLeft) {
		if (shader == null) {
			graphics.fill(x0, y0, x1, y1, Draw.withAlpha(ThemeManager.accent(), Math.round(alpha * 255)));
			return;
		}
		draw(graphics, x0, y0, x1, y1, false, alpha, 0, topLeft, topRight, bottomRight, bottomLeft);
	}

	/** Fills the rectangle with a plain ARGB colour, with optionally rounded corners (radii in GUI units). */
	public static void fillSolid(GuiGraphics graphics, int x0, int y0, int x1, int y1, int argb,
								 float topLeft, float topRight, float bottomRight, float bottomLeft) {
		if (shader == null) {
			graphics.fill(x0, y0, x1, y1, argb);
			return;
		}
		draw(graphics, x0, y0, x1, y1, true, 1f, argb, topLeft, topRight, bottomRight, bottomLeft);
	}

	private static void draw(GuiGraphics graphics, int x0, int y0, int x1, int y1, boolean flat, float alpha, int flatArgb,
							 float topLeft, float topRight, float bottomRight, float bottomLeft) {
		graphics.flush();
		configure(shader, alpha);
		setRect(shader, graphics, x0, y0, x1, y1, topLeft, topRight, bottomRight, bottomLeft);

		Matrix4f matrix = graphics.pose().last().pose();
		shader.safeGetUniform("Flat").set(flat ? 1f : 0f);
		shader.safeGetUniform("FlatColor").set(
			((flatArgb >> 16) & 0xFF) / 255f, ((flatArgb >> 8) & 0xFF) / 255f, (flatArgb & 0xFF) / 255f, ((flatArgb >>> 24) & 0xFF) / 255f);

		RenderSystem.setShader(() -> shader);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
		buffer.addVertex(matrix, x0, y0, 0f);
		buffer.addVertex(matrix, x0, y1, 0f);
		buffer.addVertex(matrix, x1, y1, 0f);
		buffer.addVertex(matrix, x1, y0, 0f);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
	}

	/**
	 * Fills the Rect and Radii uniforms the rounded-rectangle shaders use to find the element's edges.
	 * They work in framebuffer pixels with the origin at the bottom left, so the GUI coordinates are
	 * run through the current pose and GUI scale. Radii are in GUI units.
	 */
	static void setRect(ShaderInstance target, GuiGraphics graphics, int x0, int y0, int x1, int y1,
						float topLeft, float topRight, float bottomRight, float bottomLeft) {
		Matrix4f matrix = graphics.pose().last().pose();
		Window window = Minecraft.getInstance().getWindow();
		float guiScale = (float) window.getGuiScale();
		float scaleX = matrix.m00() * guiScale;

		float left = (matrix.m00() * x0 + matrix.m30()) * guiScale;
		float right = (matrix.m00() * x1 + matrix.m30()) * guiScale;
		float top = (matrix.m11() * y0 + matrix.m31()) * guiScale;
		float bottom = (matrix.m11() * y1 + matrix.m31()) * guiScale;
		float framebufferHeight = window.getHeight();
		target.safeGetUniform("Rect").set(left, framebufferHeight - bottom, right, framebufferHeight - top);
		target.safeGetUniform("Radii").set(topLeft * scaleX, topRight * scaleX, bottomRight * scaleX, bottomLeft * scaleX);
	}

	/** Fills the Time and Color0..2 uniforms every theme-coloured shader shares (and Alpha, where the shader has one). */
	static void configure(ShaderInstance target, float alpha) {
		int[] colors = ThemeManager.current().colors();
		int third = colors.length > 2 ? colors[2] : Draw.lerp(colors[0], colors[1], 0.5f);
		target.safeGetUniform("Time").set(ThemeManager.phase());
		target.safeGetUniform("Alpha").set(alpha);
		setColor(target, "Color0", colors[0]);
		setColor(target, "Color1", colors[1]);
		setColor(target, "Color2", third);
	}

	private static void setColor(ShaderInstance target, String uniform, int argb) {
		target.safeGetUniform(uniform).set(((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f);
	}
}
