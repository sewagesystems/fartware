package fartware.client.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

public final class Draw {
	private Draw() {
	}

	public static int lerp(int a, int b, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int red = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
		int green = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
		int blue = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
		return 0xFF000000 | red << 16 | green << 8 | blue;
	}

	public static int withAlpha(int rgb, int alpha) {
		return (alpha << 24) | (rgb & 0xFFFFFF);
	}

	/** Fills a rectangle with a left-to-right colour gradient. */
	public static void gradientH(GuiGraphics graphics, int x0, int y0, int x1, int y1, int left, int right) {
		VertexConsumer consumer = graphics.bufferSource().getBuffer(RenderType.gui());
		Matrix4f matrix = graphics.pose().last().pose();
		consumer.addVertex(matrix, x0, y0, 0f).setColor(left);
		consumer.addVertex(matrix, x0, y1, 0f).setColor(left);
		consumer.addVertex(matrix, x1, y1, 0f).setColor(right);
		consumer.addVertex(matrix, x1, y0, 0f).setColor(right);
		graphics.flush();
	}

	/** Fills a rectangle by blending four corner colours, which gives a smooth two dimensional gradient. */
	public static void gradientQuad(GuiGraphics graphics, int x0, int y0, int x1, int y1,
									int topLeft, int topRight, int bottomRight, int bottomLeft) {
		VertexConsumer consumer = graphics.bufferSource().getBuffer(RenderType.gui());
		Matrix4f matrix = graphics.pose().last().pose();
		consumer.addVertex(matrix, x0, y0, 0f).setColor(topLeft);
		consumer.addVertex(matrix, x0, y1, 0f).setColor(bottomLeft);
		consumer.addVertex(matrix, x1, y1, 0f).setColor(bottomRight);
		consumer.addVertex(matrix, x1, y0, 0f).setColor(topRight);
		graphics.flush();
	}

	/** Fills a rectangle with a gradient through several colours. */
	public static void gradientH(GuiGraphics graphics, int x0, int y0, int x1, int y1, int[] colors) {
		int segments = colors.length - 1;
		for (int i = 0; i < segments; i++) {
			int start = x0 + (x1 - x0) * i / segments;
			int end = x0 + (x1 - x0) * (i + 1) / segments;
			gradientH(graphics, start, y0, end, y1, colors[i], colors[i + 1]);
		}
	}
}
