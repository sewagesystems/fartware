package fartware.client.font;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import fartware.Fartware;
import fartware.client.gui.BreatheShader;
import fartware.client.gui.ThemeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A TTF/OTF font rasterized with Java AWT into glyph atlas textures.
 * One atlas is kept per effective scale (GUI scale times any scale applied to the pose stack),
 * so text stays sharp in the HUD and in a scaled click GUI at the same time.
 * All methods that touch textures must be called on the render thread.
 */
public class CustomFont {
	private static final int FIRST_CHAR = 32;
	private static final int LAST_CHAR = 383;
	private static final int PADDING = 2;
	private static final int MAX_ATLAS_WIDTH = 1024;
	private static final int MAX_ATLASES = 6;
	private static final char FORMAT_CHAR = '\u00a7';
	private static final AtomicInteger COUNTER = new AtomicInteger();

	private final java.awt.Font base;
	private final float size;
	private final int fontId = COUNTER.incrementAndGet();
	private final Map<Integer, Atlas> atlases = new LinkedHashMap<>();

	private record Glyph(float u0, float v0, float u1, float v1, float advance, int cellWidth) {
	}

	private static final class Atlas {
		final double scale;
		final ResourceLocation id;
		final Glyph[] glyphs = new Glyph[LAST_CHAR - FIRST_CHAR + 1];
		DynamicTexture texture;
		int cellHeight;
		/** Pixels from the top of the line to the baseline. */
		int ascent;
		/** Height of a capital letter in pixels. */
		float capHeight;

		Atlas(double scale, ResourceLocation id) {
			this.scale = scale;
			this.id = id;
		}
	}

	public CustomFont(java.awt.Font base, float size) {
		this.base = base;
		this.size = size;
	}

	public float getSize() {
		return size;
	}

	public void close() {
		for (Atlas atlas : atlases.values()) {
			release(atlas);
		}
		atlases.clear();
	}

	private static void release(Atlas atlas) {
		Minecraft.getInstance().getTextureManager().release(atlas.id);
	}

	/** Returns the atlas for the given pixels-per-GUI-unit scale, building it if needed. Scales are rounded to half steps. */
	private Atlas atlas(double scale) {
		int key = Math.max(1, (int) Math.round(scale * 2));
		Atlas atlas = atlases.get(key);
		if (atlas != null) {
			return atlas;
		}

		if (atlases.size() >= MAX_ATLASES) {
			Iterator<Atlas> oldest = atlases.values().iterator();
			release(oldest.next());
			oldest.remove();
		}
		atlas = new Atlas(key / 2.0, Fartware.id("custom_font/" + fontId + "/" + key));
		build(atlas);
		atlases.put(key, atlas);
		return atlas;
	}

	private Atlas baseAtlas() {
		return atlas(Minecraft.getInstance().getWindow().getGuiScale());
	}

	private void build(Atlas atlas) {
		java.awt.Font font = base.deriveFont((float) (size * atlas.scale));
		FontRenderContext context = new FontRenderContext(null, true, true);

		BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D scratchGraphics = scratch.createGraphics();
		applyHints(scratchGraphics);
		scratchGraphics.setFont(font);
		FontMetrics metrics = scratchGraphics.getFontMetrics();
		int ascent = metrics.getAscent();
		atlas.ascent = ascent;
		atlas.capHeight = (float) font.createGlyphVector(context, "H").getVisualBounds().getHeight();
		atlas.cellHeight = ascent + metrics.getDescent() + PADDING * 2;
		scratchGraphics.dispose();

		int count = atlas.glyphs.length;
		float[] advances = new float[count];
		int[] cellWidths = new int[count];
		int[] xs = new int[count];
		int[] ys = new int[count];
		int cursorX = 0;
		int cursorY = 0;
		for (int i = 0; i < count; i++) {
			char c = (char) (FIRST_CHAR + i);
			advances[i] = (float) font.getStringBounds(String.valueOf(c), context).getWidth();
			cellWidths[i] = (int) Math.ceil(advances[i]) + PADDING * 2;
			if (cursorX + cellWidths[i] > MAX_ATLAS_WIDTH) {
				cursorX = 0;
				cursorY += atlas.cellHeight;
			}
			xs[i] = cursorX;
			ys[i] = cursorY;
			cursorX += cellWidths[i];
		}
		int atlasWidth = MAX_ATLAS_WIDTH;
		int atlasHeight = cursorY + atlas.cellHeight;

		BufferedImage image = new BufferedImage(atlasWidth, atlasHeight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		applyHints(graphics);
		graphics.setFont(font);
		graphics.setColor(Color.WHITE);
		for (int i = 0; i < count; i++) {
			char c = (char) (FIRST_CHAR + i);
			if (c != ' ' && font.canDisplay(c)) {
				graphics.drawString(String.valueOf(c), xs[i] + PADDING, ys[i] + PADDING + ascent);
			}
			atlas.glyphs[i] = new Glyph(
				(float) xs[i] / atlasWidth, (float) ys[i] / atlasHeight,
				(float) (xs[i] + cellWidths[i]) / atlasWidth, (float) (ys[i] + atlas.cellHeight) / atlasHeight,
				advances[i], cellWidths[i]
			);
		}
		graphics.dispose();

		NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, atlasWidth, atlasHeight, false);
		for (int y = 0; y < atlasHeight; y++) {
			for (int x = 0; x < atlasWidth; x++) {
				// White glyphs, coverage in alpha: the colour channels are identical in ARGB and ABGR.
				nativeImage.setPixelRGBA(x, y, (image.getRGB(x, y) & 0xFF000000) | 0x00FFFFFF);
			}
		}

		atlas.texture = new DynamicTexture(nativeImage);
		Minecraft.getInstance().getTextureManager().register(atlas.id, atlas.texture);
		atlas.texture.setFilter(true, false);
	}

	private static void applyHints(Graphics2D graphics) {
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
	}

	private static Glyph glyph(Atlas atlas, char c) {
		if (c < FIRST_CHAR || c > LAST_CHAR) {
			c = '?';
		}
		return atlas.glyphs[c - FIRST_CHAR];
	}

	/** Width of the text in GUI units, ignoring formatting codes. */
	public int width(String text) {
		Atlas atlas = baseAtlas();
		float width = 0;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == FORMAT_CHAR && i + 1 < text.length()) {
				i++;
				continue;
			}
			width += glyph(atlas, c).advance();
		}
		// Rounded, not ceiled, so centred and right-aligned text isn't biased towards the left.
		return (int) Math.round(width / atlas.scale);
	}

	/** Height of one line in GUI units. */
	public int height() {
		Atlas atlas = baseAtlas();
		return (int) Math.ceil((atlas.cellHeight - PADDING * 2) / atlas.scale);
	}

	/** Distance in GUI units from the top of the line to the baseline. */
	public float ascent() {
		Atlas atlas = baseAtlas();
		return (float) (atlas.ascent / atlas.scale);
	}

	/** Height of a capital letter in GUI units. */
	public float capHeight() {
		Atlas atlas = baseAtlas();
		return (float) (atlas.capHeight / atlas.scale);
	}

	public void draw(GuiGraphics graphics, String text, float x, float y, int color, boolean shadow) {
		if (shadow) {
			int shadowColor = (color & 0xFF000000) | ((color & 0xFCFCFC) >> 2);
			drawInternal(graphics, text, x + 1, y + 1, shadowColor, true, false);
		}
		drawInternal(graphics, text, x, y, color, false, false);
	}

	/** Draws text coloured by the breathing theme shader. Formatting codes are skipped, not applied. */
	public void drawShaded(GuiGraphics graphics, String text, float x, float y, boolean shadow) {
		if (shadow) {
			drawInternal(graphics, text, x + 1, y + 1, 0xB0000000, true, false);
		}
		drawInternal(graphics, text, x, y, 0xFFFFFFFF, true, true);
	}

	private void drawInternal(GuiGraphics graphics, String text, float x, float y, int baseColor, boolean flat, boolean shaded) {
		ShaderInstance textShader = shaded ? BreatheShader.prepareText() : null;
		if (shaded && textShader == null) {
			baseColor = ThemeManager.accent();
		}
		if ((baseColor & 0xFC000000) == 0) {
			baseColor |= 0xFF000000;
		}

		graphics.flush();
		Matrix4f matrix = graphics.pose().last().pose();
		double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
		float poseScale = Math.abs(matrix.m00());
		Atlas atlas = atlas(guiScale * (poseScale > 0.01f ? poseScale : 1f));
		float scale = (float) atlas.scale;
		float padding = PADDING / scale;
		float cellHeight = atlas.cellHeight / scale;

		if (textShader != null) {
			ShaderInstance shader = textShader;
			RenderSystem.setShader(() -> shader);
		} else {
			RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		}
		RenderSystem.setShaderTexture(0, atlas.id);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		boolean any = false;
		int color = baseColor;
		float cursor = x;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == FORMAT_CHAR && i + 1 < text.length()) {
				ChatFormatting formatting = ChatFormatting.getByCode(text.charAt(i + 1));
				i++;
				if (formatting == ChatFormatting.RESET) {
					color = baseColor;
				} else if (!flat && formatting != null && formatting.getColor() != null) {
					color = (baseColor & 0xFF000000) | formatting.getColor();
				}
				continue;
			}

			Glyph glyph = glyph(atlas, c);
			// Every atlas cell has PADDING pixels around the glyph, so the quad starts that far
			// up and left of the pen position to put the text exactly at (x, y).
			float x0 = cursor - padding;
			float x1 = x0 + glyph.cellWidth() / scale;
			float y0 = y - padding;
			float y1 = y0 + cellHeight;
			buffer.addVertex(matrix, x0, y0, 0f).setUv(glyph.u0(), glyph.v0()).setColor(color);
			buffer.addVertex(matrix, x0, y1, 0f).setUv(glyph.u0(), glyph.v1()).setColor(color);
			buffer.addVertex(matrix, x1, y1, 0f).setUv(glyph.u1(), glyph.v1()).setColor(color);
			buffer.addVertex(matrix, x1, y0, 0f).setUv(glyph.u1(), glyph.v0()).setColor(color);
			any = true;
			cursor += glyph.advance() / scale;
		}

		if (any) {
			BufferUploader.drawWithShader(buffer.buildOrThrow());
		}
	}
}
