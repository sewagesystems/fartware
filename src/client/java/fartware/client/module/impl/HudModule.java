package fartware.client.module.impl;

import fartware.client.FartwareClient;
import fartware.client.font.FontManager;
import fartware.client.gui.BlurShader;
import fartware.client.gui.BreatheShader;
import fartware.client.gui.Layout;
import fartware.client.gui.Theme;
import fartware.client.gui.ThemeManager;
import fartware.client.module.Category;
import fartware.client.module.Module;
import fartware.client.setting.BooleanSetting;
import fartware.client.setting.NumberSetting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class HudModule extends Module {
	private final BooleanSetting watermark = add(new BooleanSetting("Watermark", true));
	private final BooleanSetting arrayList = add(new BooleanSetting("ArrayList", true));
	private final BooleanSetting coordinates = add(new BooleanSetting("Coordinates", true));
	private final BooleanSetting fps = add(new BooleanSetting("FPS", true));
	private final BooleanSetting bps = add(new BooleanSetting("BPS", true));
	private final NumberSetting listBlur = add(new NumberSetting("ListBlur", 4, 0, 10, 1));

	private static final int BAR_WIDTH = 2;
	/** How much the blurred array list background is darkened. */
	private static final float LIST_DIM = 0.55f;

	private final long startMillis = System.currentTimeMillis();
	private double blocksPerSecond;

	public HudModule() {
		super("HUD", "Draws the watermark, enabled modules and other info on screen.", Category.RENDER);
		setVisible(false);
		setEnabled(true);
	}

	@Override
	public void onTick() {
		if (mc.player == null) {
			return;
		}
		double speed = Math.hypot(mc.player.getX() - mc.player.xo, mc.player.getZ() - mc.player.zo) * 20;
		blocksPerSecond += (speed - blocksPerSecond) * 0.3;
	}

	@Override
	public void onRender2D(GuiGraphics graphics, DeltaTracker deltaTracker) {
		if (mc.options.hideGui || mc.player == null) {
			return;
		}
		FontManager font = FartwareClient.fonts();
		int margin = Layout.HUD_MARGIN;
		int lineStep = font.height();

		// Top text is aligned by the top of its capitals, bottom text by its baseline,
		// so the visible gap to the screen edge is the same whichever font is used.
		if (watermark.get()) {
			long seconds = (System.currentTimeMillis() - startMillis) / 1000;
			String time = String.format(Locale.ROOT, " (%02d:%02d)", seconds / 60 % 100, seconds % 60);
			String name = "fartware";
			int y = font.topAligned(margin);
			font.drawAccent(graphics, name, margin, y, true);
			font.drawString(graphics, time, margin + font.width(name), y, Theme.TEXT_DIM, true);
		}

		int baseline = graphics.guiHeight() - margin;
		if (bps.get()) {
			String value = String.format(Locale.ROOT, "%.1f", blocksPerSecond);
			labelled(graphics, font, "BPS: ", value, graphics.guiWidth() - margin, font.baselineAligned(baseline), true);
		}
		if (coordinates.get()) {
			String value = String.format(Locale.ROOT, "%.0f %.0f %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
			labelled(graphics, font, "XYZ: ", value, margin, font.baselineAligned(baseline), false);
			baseline -= lineStep;
		}
		if (fps.get()) {
			labelled(graphics, font, "FPS: ", String.valueOf(mc.getFps()), margin, font.baselineAligned(baseline), false);
		}

		if (arrayList.get()) {
			renderArrayList(graphics, font);
		}
	}

	/** Draws "Label: value" with a coloured label. When rightAligned, x is the right edge. */
	private void labelled(GuiGraphics graphics, FontManager font, String label, String value, int x, int y, boolean rightAligned) {
		int start = rightAligned ? x - font.width(label) - font.width(value) : x;
		font.drawAccent(graphics, label, start, y, true);
		font.drawString(graphics, value, start + font.width(label), y, Theme.TEXT, true);
	}

	private void renderArrayList(GuiGraphics graphics, FontManager font) {
		List<Entry> entries = FartwareClient.modules().getAll().stream()
			.filter(module -> module.isEnabled() && module.isVisible())
			.map(module -> new Entry(module.getName(), module.getSuffix() == null ? "" : module.getSuffix()))
			.sorted(Comparator.comparingInt((Entry entry) -> entry.width(font)).reversed())
			.toList();

		// Sized from the capital letters rather than the font's line box, so the rows are the same
		// height with every font instead of loose with a tall font and cramped with the vanilla one.
		int rowHeight = font.capitalHeight() + Layout.LIST_ROW_PADDING * 2;
		// The list is flush with the top right corner of the screen.
		int right = graphics.guiWidth();
		int y = 0;

		// Equal padding either side of the text, with the accent bar on the screen edge.
		int[] lefts = new int[entries.size()];
		for (int i = 0; i < lefts.length; i++) {
			lefts[i] = right - BAR_WIDTH - Layout.INNER_GAP - entries.get(i).width(font) - Layout.INNER_GAP;
		}

		// One blurred copy of the frame serves every row's background.
		if (!entries.isEmpty()) {
			BlurShader.capture(graphics, listBlur.floatValue());
		}

		float radius = Layout.CORNER_RADIUS;
		for (int i = 0; i < entries.size(); i++) {
			Entry entry = entries.get(i);
			int left = lefts[i];
			int textY = font.centerY(y, rowHeight);

			// Rows join without gaps, and the top row meets the top of the screen without one, so no top
			// corner is rounded. Only a row's bottom left is, wherever the row below is narrower (or there is none).
			boolean lastOrNarrowerBelow = i == entries.size() - 1 || lefts[i + 1] > left;
			float bottomLeft = lastOrNarrowerBelow ? radius : 0f;

			// The background is just the blurred, dimmed scene behind the row, with no colour shader on it.
			BlurShader.fillFrosted(graphics, left, y, right, y + rowHeight, LIST_DIM, 0f, 0f, 0f, bottomLeft);
			BreatheShader.fill(graphics, right - BAR_WIDTH, y, right, y + rowHeight);
			font.drawAccent(graphics, entry.name(), left + Layout.INNER_GAP, textY, false);
			if (!entry.mode().isEmpty()) {
				font.drawString(graphics, entry.mode(), left + Layout.INNER_GAP + font.width(entry.name()) + Layout.LIST_MODE_GAP,
					textY, Theme.TEXT_DIM, false);
			}
			y += rowHeight;
		}
	}

	/** A module's name, and its mode (shown in grey after the name) if it has one. */
	private record Entry(String name, String mode) {
		int width(FontManager font) {
			int width = font.width(name);
			return mode.isEmpty() ? width : width + Layout.LIST_MODE_GAP + font.width(mode);
		}
	}
}
