package fartware.client.gui;

import fartware.client.FartwareClient;
import fartware.client.font.FontManager;
import fartware.client.setting.BooleanSetting;
import fartware.client.setting.ColorSetting;
import fartware.client.setting.ModeSetting;
import fartware.client.setting.NumberSetting;
import fartware.client.setting.Setting;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;

/** Drawing and input for one setting row, shared by the module buttons and the theme panel. */
public final class SettingRow {
	public static final int HEIGHT = Layout.SUB_ROW_HEIGHT;

	private static final int[] RAINBOW = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};

	/** What a mouse drag started on a row does. Called with the current mouse position while the button is held. */
	@FunctionalInterface
	public interface Drag {
		void drag(double mouseX, double mouseY);
	}

	private SettingRow() {
	}

	public static int textY(int rowY, int rowHeight) {
		return FartwareClient.fonts().centerY(rowY, rowHeight);
	}

	/**
	 * Total height of the setting's row. Number settings are taller because the slider sits on its own row
	 * below the label, and an open colour picker adds its own area below the label.
	 */
	public static int heightOf(Setting<?> setting) {
		if (setting instanceof NumberSetting) {
			return HEIGHT + Layout.SLIDER_ROW_HEIGHT;
		}
		if (setting instanceof ColorSetting color && color.isExpanded()) {
			return HEIGHT + Layout.PICKER_GAP + Layout.PICKER_SV_HEIGHT + Layout.PICKER_GAP + Layout.PICKER_HUE_HEIGHT
				+ Layout.PICKER_GAP + HEIGHT;
		}
		return HEIGHT;
	}

	public static void render(GuiGraphics graphics, Setting<?> setting, String label, int x, int y, int width) {
		FontManager font = FartwareClient.fonts();
		int textY = textY(y, HEIGHT);
		int left = x + Layout.PAD;
		int right = x + width - Layout.PAD;
		graphics.fill(x, y, x + width, y + heightOf(setting), Theme.SUB_BG);

		if (setting instanceof NumberSetting number) {
			// The label and value share the top row; the slider gets the row below.
			font.drawString(graphics, label, left, textY, Theme.TEXT, false);
			String value = number.display();
			font.drawString(graphics, value, right - font.width(value), textY, Theme.TEXT_DIM, false);

			int start = trackStart(x);
			int end = trackEnd(x, width);
			int trackTop = y + HEIGHT + (Layout.SLIDER_ROW_HEIGHT - Layout.TRACK_HEIGHT) / 2;
			int trackBottom = trackTop + Layout.TRACK_HEIGHT;
			graphics.fill(start, trackTop, end, trackBottom, Theme.TRACK);
			BreatheShader.fill(graphics, start, trackTop, start + (int) ((end - start) * number.getFraction()), trackBottom);
		} else if (setting instanceof BooleanSetting bool) {
			renderToggle(graphics, label, bool.get(), x, y, width);
		} else if (setting instanceof ColorSetting color) {
			renderColor(graphics, color, label, x, y, width);
		} else {
			font.drawString(graphics, label, left, textY, Theme.TEXT, false);
			int maxValueWidth = width - Layout.PAD * 2 - Layout.INNER_GAP - font.width(label);
			String value = clip(font, setting.display(), maxValueWidth);
			font.drawString(graphics, value, right - font.width(value), textY, Theme.TEXT_DIM, false);
		}
	}

	public static void render(GuiGraphics graphics, Setting<?> setting, int x, int y, int width) {
		render(graphics, setting, setting.getName(), x, y, width);
	}

	/** A label with a toggle box on the right. The row background is drawn by the caller. */
	private static void renderToggle(GuiGraphics graphics, String label, boolean on, int x, int y, int width) {
		FontManager font = FartwareClient.fonts();
		int textY = textY(y, HEIGHT);
		int right = x + width - Layout.PAD;
		if (on) {
			font.drawAccent(graphics, label, x + Layout.PAD, textY, false);
		} else {
			font.drawString(graphics, label, x + Layout.PAD, textY, Theme.TEXT, false);
		}
		int boxX = right - Layout.TOGGLE_SIZE;
		int boxY = y + (HEIGHT - Layout.TOGGLE_SIZE) / 2;
		graphics.fill(boxX, boxY, right, boxY + Layout.TOGGLE_SIZE, Theme.TRACK);
		if (on) {
			BreatheShader.fill(graphics, boxX + 1, boxY + 1, right - 1, boxY + Layout.TOGGLE_SIZE - 1);
		}
	}

	private static void renderColor(GuiGraphics graphics, ColorSetting color, String label, int x, int y, int width) {
		FontManager font = FartwareClient.fonts();
		int textY = textY(y, HEIGHT);
		int right = x + width - Layout.PAD;
		font.drawString(graphics, label, x + Layout.PAD, textY, Theme.TEXT, false);

		// The swatch shows the current colour; the +/- next to it shows whether the picker is open.
		int swatchLeft = right - Layout.COLOR_SWATCH_WIDTH;
		int swatchTop = y + (HEIGHT - Layout.SWATCH_HEIGHT) / 2;
		int swatchBottom = swatchTop + Layout.SWATCH_HEIGHT;
		graphics.fill(swatchLeft - 1, swatchTop - 1, right + 1, swatchBottom + 1, Theme.TRACK);
		if (color.isTheme()) {
			BreatheShader.fill(graphics, swatchLeft, swatchTop, right, swatchBottom);
		} else {
			graphics.fill(swatchLeft, swatchTop, right, swatchBottom, 0xFF000000 | color.get());
		}
		String indicator = color.isExpanded() ? "-" : "+";
		font.drawString(graphics, indicator, swatchLeft - 1 - Layout.INNER_GAP - font.width(indicator), textY, Theme.TEXT_DIM, false);

		if (color.isExpanded()) {
			renderPicker(graphics, color, x, y, width);
		}
	}

	private static int pickerLeft(int x) {
		return x + Layout.PAD;
	}

	private static int pickerRight(int x, int width) {
		return x + width - Layout.PAD;
	}

	private static int squareTop(int rowY) {
		return rowY + HEIGHT + Layout.PICKER_GAP;
	}

	private static int hueTop(int rowY) {
		return squareTop(rowY) + Layout.PICKER_SV_HEIGHT + Layout.PICKER_GAP;
	}

	private static int themeRowTop(int rowY) {
		return hueTop(rowY) + Layout.PICKER_HUE_HEIGHT + Layout.PICKER_GAP;
	}

	private static void renderPicker(GuiGraphics graphics, ColorSetting color, int x, int rowY, int width) {
		int left = pickerLeft(x);
		int right = pickerRight(x, width);

		// Saturation grows to the right and brightness upwards. Blending white, the pure hue and two blacks
		// across the corners gives exactly that.
		int squareTop = squareTop(rowY);
		int squareBottom = squareTop + Layout.PICKER_SV_HEIGHT;
		int pureHue = 0xFF000000 | (Color.HSBtoRGB(color.getHue(), 1f, 1f) & 0xFFFFFF);
		Draw.gradientQuad(graphics, left, squareTop, right, squareBottom, 0xFFFFFFFF, pureHue, 0xFF000000, 0xFF000000);

		int hueTop = hueTop(rowY);
		int hueBottom = hueTop + Layout.PICKER_HUE_HEIGHT;
		Draw.gradientH(graphics, left, hueTop, right, hueBottom, RAINBOW);

		// A small marker on each: a black box with a white one inside, and the chosen colour at the centre of the square's.
		if (!color.isTheme()) {
			int markerX = left + Math.round(color.getSaturation() * (right - left - 1));
			int markerY = squareTop + Math.round((1f - color.getBrightness()) * (Layout.PICKER_SV_HEIGHT - 1));
			graphics.fill(markerX - 2, markerY - 2, markerX + 3, markerY + 3, 0xFF000000);
			graphics.fill(markerX - 1, markerY - 1, markerX + 2, markerY + 2, 0xFFFFFFFF);
			graphics.fill(markerX, markerY, markerX + 1, markerY + 1, 0xFF000000 | color.get());

			int hueX = left + Math.round(color.getHue() * (right - left - 1));
			graphics.fill(hueX - 1, hueTop - 1, hueX + 2, hueBottom + 1, 0xFF000000);
			graphics.fill(hueX, hueTop, hueX + 1, hueBottom, 0xFFFFFFFF);
		}

		renderToggle(graphics, "Theme", color.isTheme(), x, themeRowTop(rowY), width);
	}

	/** Draws a plain "name  value" row (used for the bind row). */
	public static void renderText(GuiGraphics graphics, String label, String value, int x, int y, int width) {
		FontManager font = FartwareClient.fonts();
		int textY = textY(y, HEIGHT);
		graphics.fill(x, y, x + width, y + HEIGHT, Theme.SUB_BG);
		font.drawString(graphics, label, x + Layout.PAD, textY, Theme.TEXT, false);
		font.drawString(graphics, value, x + width - Layout.PAD - font.width(value), textY, Theme.TEXT_DIM, false);
	}

	/**
	 * Applies a click to the setting. Returns what to do while the mouse is dragged if the click started a drag
	 * (a slider, the colour square or the hue bar), otherwise null. y is the top of the setting's row.
	 */
	public static Drag click(Setting<?> setting, double mouseX, double mouseY, int button, int x, int y, int width) {
		if (setting instanceof BooleanSetting bool) {
			bool.toggle();
		} else if (setting instanceof ColorSetting color) {
			return clickColor(color, mouseX, mouseY, button, x, y, width);
		} else if (setting instanceof ModeSetting mode) {
			mode.cycle(button != GLFW.GLFW_MOUSE_BUTTON_RIGHT);
		} else if (setting instanceof NumberSetting number && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			Drag drag = (dragX, dragY) -> dragSlider(number, dragX, x, width);
			drag.drag(mouseX, mouseY);
			return drag;
		}
		return null;
	}

	private static Drag clickColor(ColorSetting color, double mouseX, double mouseY, int button, int x, int y, int width) {
		if (mouseY < y + HEIGHT) {
			color.toggleExpanded();
			return null;
		}
		if (!color.isExpanded() || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			return null;
		}

		int left = pickerLeft(x);
		int right = pickerRight(x, width);
		int squareTop = squareTop(y);
		int hueTop = hueTop(y);

		Drag drag = null;
		if (mouseX >= left && mouseX < right && mouseY >= squareTop && mouseY < squareTop + Layout.PICKER_SV_HEIGHT) {
			drag = (dragX, dragY) -> color.setHsb(color.getHue(),
				(float) ((dragX - left) / (right - left - 1)),
				1f - (float) ((dragY - squareTop) / (Layout.PICKER_SV_HEIGHT - 1)));
		} else if (mouseX >= left && mouseX < right && mouseY >= hueTop && mouseY < hueTop + Layout.PICKER_HUE_HEIGHT) {
			drag = (dragX, dragY) -> color.setHsb((float) ((dragX - left) / (right - left - 1)),
				color.getSaturation(), color.getBrightness());
		} else if (mouseY >= themeRowTop(y) && mouseY < themeRowTop(y) + HEIGHT) {
			color.setTheme(!color.isTheme());
		}
		if (drag != null) {
			drag.drag(mouseX, mouseY);
		}
		return drag;
	}

	private static int trackStart(int x) {
		return x + Layout.PAD;
	}

	private static int trackEnd(int x, int width) {
		return x + width - Layout.PAD;
	}

	private static void dragSlider(NumberSetting number, double mouseX, int x, int width) {
		int start = trackStart(x);
		int end = trackEnd(x, width);
		number.setFraction((mouseX - start) / (end - start));
	}

	private static String clip(FontManager font, String text, int maxWidth) {
		while (text.length() > 1 && font.width(text) > maxWidth) {
			text = text.substring(0, text.length() - 1);
		}
		return text;
	}
}
