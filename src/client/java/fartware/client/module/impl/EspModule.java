package fartware.client.module.impl;

import fartware.client.FartwareClient;
import fartware.client.font.FontManager;
import fartware.client.gui.BreatheShader;
import fartware.client.gui.Draw;
import fartware.client.module.Category;
import fartware.client.module.Module;
import fartware.client.render.EspRenderer;
import fartware.client.setting.BooleanSetting;
import fartware.client.setting.ColorSetting;
import fartware.client.setting.ModeSetting;
import fartware.client.setting.NumberSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Highlights other players. The Mode setting picks how, and only the settings that belong to the
 * selected mode are shown:
 * <ul>
 *   <li>2D: a box with a health bar on its left and the name above it, drawn on the HUD.</li>
 *   <li>Glow: a glow in the theme colours around the player model (not the vanilla glowing effect).</li>
 *   <li>Chams: the player model drawn in a flat colour, through walls. Static uses one colour; Visible uses
 *       one colour where the player can be seen and another where they are hidden, so a half hidden player shows both.</li>
 * </ul>
 * The world rendering for Glow and Chams lives in {@link EspRenderer}.
 */
public class EspModule extends Module {
	private static final int OUTLINE = 0xFF000000;
	private static final int HEALTH_FULL = 0xFF3BFF5A;
	private static final int HEALTH_EMPTY = 0xFFFF3B3B;

	private final ModeSetting mode = add(new ModeSetting("Mode", "2D", "2D", "Glow", "Chams"));
	private final BooleanSetting invisible = add(new BooleanSetting("Invisible", false));

	private final BooleanSetting healthBar = add(new BooleanSetting("HealthBar", true), this::is2D);
	private final BooleanSetting names = add(new BooleanSetting("Names", true), this::is2D);

	private final NumberSetting glowSize = add(new NumberSetting("GlowSize", 3, 1, 10, 1), this::isGlow);
	private final NumberSetting glowStrength = add(new NumberSetting("GlowStrength", 0.8, 0.1, 1, 0.1), this::isGlow);
	private final BooleanSetting glowWalls = add(new BooleanSetting("Walls", true), this::isGlow);

	private final ModeSetting chamsStyle = add(new ModeSetting("Style", "Visible", "Static", "Visible"), this::isChams);
	private final ColorSetting staticColor = add(new ColorSetting("Color", ColorSetting.THEME), this::chamsStatic);
	private final ColorSetting visibleColor = add(new ColorSetting("VisibleColor", "Green"), this::chamsVisibility);
	private final ColorSetting hiddenColor = add(new ColorSetting("HiddenColor", "Red"), this::chamsVisibility);

	public EspModule() {
		super("ESP", "Highlights other players with a 2D box, a glow or chams.", Category.RENDER);
	}

	public boolean is2D() {
		return mode.is("2D");
	}

	public boolean isGlow() {
		return mode.is("Glow");
	}

	public boolean isChams() {
		return mode.is("Chams");
	}

	/** Chams with one colour. */
	public boolean chamsStatic() {
		return isChams() && chamsStyle.is("Static");
	}

	/** Chams with a visible and a hidden colour. */
	public boolean chamsVisibility() {
		return isChams() && chamsStyle.is("Visible");
	}

	public float glowSize() {
		return glowSize.floatValue();
	}

	public float glowStrength() {
		return glowStrength.floatValue();
	}

	public boolean glowThroughWalls() {
		return glowWalls.get();
	}

	public int staticColor() {
		return staticColor.argb();
	}

	public int visibleColor() {
		return visibleColor.argb();
	}

	public int hiddenColor() {
		return hiddenColor.argb();
	}

	/** The players this module highlights. */
	public List<AbstractClientPlayer> targets() {
		List<AbstractClientPlayer> targets = new ArrayList<>();
		if (mc.level == null) {
			return targets;
		}
		for (AbstractClientPlayer player : mc.level.players()) {
			if (player == mc.player || player.isSpectator() || (player.isInvisible() && !invisible.get())) {
				continue;
			}
			targets.add(player);
		}
		return targets;
	}

	@Override
	public void onRender2D(GuiGraphics graphics, DeltaTracker deltaTracker) {
		if (!is2D() || mc.options.hideGui || mc.level == null) {
			return;
		}
		float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
		for (AbstractClientPlayer player : targets()) {
			float[] bounds = EspRenderer.project(player, partialTick, graphics.guiWidth(), graphics.guiHeight());
			if (bounds == null) {
				continue;
			}
			int left = Math.round(bounds[0]);
			int top = Math.round(bounds[1]);
			int right = Math.round(bounds[2]);
			int bottom = Math.round(bounds[3]);
			if (right - left < 3 || bottom - top < 3) {
				continue;
			}

			drawBox(graphics, left, top, right, bottom);
			if (healthBar.get()) {
				drawHealthBar(graphics, player, left, top, bottom);
			}
			if (names.get()) {
				FontManager font = FartwareClient.fonts();
				// Aligned with the left edge of the box, just above its outline.
				font.drawString(graphics, player.getName().getString(), left, font.baselineAligned(top - 3), 0xFFFFFFFF, true);
			}
		}
	}

	/** A one pixel box in the theme colours with a black outline on both sides so it stays readable on any background. */
	private void drawBox(GuiGraphics graphics, int left, int top, int right, int bottom) {
		ring(graphics, left - 1, top - 1, right + 1, bottom + 1, OUTLINE);
		ring(graphics, left, top, right, bottom, 0);
		ring(graphics, left + 1, top + 1, right - 1, bottom - 1, OUTLINE);
	}

	/** One pixel thick rectangle outline. A colour of 0 draws it with the theme shader instead. */
	private void ring(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
		edge(graphics, left, top, right, top + 1, color);
		edge(graphics, left, bottom - 1, right, bottom, color);
		edge(graphics, left, top + 1, left + 1, bottom - 1, color);
		edge(graphics, right - 1, top + 1, right, bottom - 1, color);
	}

	private void edge(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
		if (color == 0) {
			BreatheShader.fill(graphics, x0, y0, x1, y1);
		} else {
			graphics.fill(x0, y0, x1, y1, color);
		}
	}

	/** A two pixel wide bar on the left side of the box that drains from the top as health drops. */
	private void drawHealthBar(GuiGraphics graphics, AbstractClientPlayer player, int left, int top, int bottom) {
		float fraction = Math.max(0f, Math.min(1f, player.getHealth() / player.getMaxHealth()));
		int barRight = left - 4;
		int barLeft = barRight - 2;

		graphics.fill(barLeft - 1, top - 1, barRight + 1, bottom + 1, OUTLINE);
		graphics.fill(barLeft, top, barRight, bottom, 0xFF202020);
		int filledTop = bottom - Math.round((bottom - top) * fraction);
		graphics.fill(barLeft, filledTop, barRight, bottom, Draw.lerp(HEALTH_EMPTY, HEALTH_FULL, fraction));
	}
}
