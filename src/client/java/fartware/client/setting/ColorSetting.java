package fartware.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import fartware.client.gui.ThemeManager;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A colour chosen with the click GUI's colour picker. It keeps hue, saturation and brightness as well as the
 * RGB value so dragging through grey or black doesn't lose the hue. It can also follow the theme colour instead.
 * <p>
 * From the .set command it accepts "theme", a hex code such as #3BE8FF, or one of the preset names.
 */
public class ColorSetting extends Setting<Integer> {
	public static final String THEME = "Theme";

	private static final Map<String, Integer> PRESETS = new LinkedHashMap<>();

	static {
		PRESETS.put("White", 0xFFFFFF);
		PRESETS.put("Red", 0xFF3B3B);
		PRESETS.put("Green", 0x3BFF5A);
		PRESETS.put("Blue", 0x3B7BFF);
		PRESETS.put("Yellow", 0xFFE23B);
		PRESETS.put("Orange", 0xFF9A3B);
		PRESETS.put("Pink", 0xFF6EC7);
		PRESETS.put("Purple", 0xB03BFF);
		PRESETS.put("Cyan", 0x3BE8FF);
	}

	private float hue;
	private float saturation;
	private float brightness;
	private boolean theme;
	private boolean expanded;

	/** @param defaultColor "Theme", a preset name or a hex code */
	public ColorSetting(String name, String defaultColor) {
		super(name, PRESETS.get("Cyan"));
		setRgb(PRESETS.get("Cyan"));
		parse(defaultColor);
	}

	/** The colour as opaque ARGB. Follows the animated theme colour while theme mode is on. */
	public int argb() {
		return theme ? ThemeManager.accent() : 0xFF000000 | value;
	}

	public boolean isTheme() {
		return theme;
	}

	public void setTheme(boolean theme) {
		this.theme = theme;
	}

	public float getHue() {
		return hue;
	}

	public float getSaturation() {
		return saturation;
	}

	public float getBrightness() {
		return brightness;
	}

	/** Whether the colour picker is open in the click GUI. This is only display state and isn't saved. */
	public boolean isExpanded() {
		return expanded;
	}

	public void toggleExpanded() {
		expanded = !expanded;
	}

	/** Picks a colour from the picker. This also turns theme mode off. All values are 0..1. */
	public void setHsb(float hue, float saturation, float brightness) {
		this.hue = clamp(hue);
		this.saturation = clamp(saturation);
		this.brightness = clamp(brightness);
		this.value = Color.HSBtoRGB(this.hue, this.saturation, this.brightness) & 0xFFFFFF;
		this.theme = false;
	}

	public void setRgb(int rgb) {
		float[] hsb = Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
		this.hue = hsb[0];
		this.saturation = hsb[1];
		this.brightness = hsb[2];
		this.value = rgb & 0xFFFFFF;
		this.theme = false;
	}

	@Override
	public void set(Integer rgb) {
		setRgb(rgb);
	}

	private static float clamp(float value) {
		return Math.max(0f, Math.min(1f, value));
	}

	@Override
	public String display() {
		return theme ? THEME : String.format(Locale.ROOT, "#%06X", value);
	}

	@Override
	public String hint() {
		return "Theme, a hex code like #3BE8FF, or " + String.join(", ", PRESETS.keySet());
	}

	@Override
	public boolean parse(String input) {
		String text = input.trim();
		if (text.equalsIgnoreCase(THEME)) {
			theme = true;
			return true;
		}
		for (Map.Entry<String, Integer> preset : PRESETS.entrySet()) {
			if (preset.getKey().equalsIgnoreCase(text)) {
				setRgb(preset.getValue());
				return true;
			}
		}
		String hex = text.startsWith("#") ? text.substring(1) : text;
		if (hex.length() == 6) {
			try {
				setRgb(Integer.parseInt(hex, 16));
				return true;
			} catch (NumberFormatException ignored) {
			}
		}
		return false;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(display());
	}

	@Override
	public void fromJson(JsonElement element) {
		parse(element.getAsString());
	}
}
