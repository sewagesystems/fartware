package fartware.client.gui;

import fartware.client.setting.NumberSetting;
import fartware.client.setting.ThemeSetting;

import java.util.List;

/** Holds the selected colour theme and the animated accent colour derived from it. */
public final class ThemeManager {
	public record ColorTheme(String name, int... colors) {
	}

	public static final List<ColorTheme> THEMES = List.of(
		new ColorTheme("Ocean", 0xFF00C6FF, 0xFF6A5CFF),
		new ColorTheme("Sunset", 0xFFFF4E3A, 0xFFFFC25A),
		new ColorTheme("Aurora", 0xFF00E5A0, 0xFF2F8CFF),
		new ColorTheme("Sakura", 0xFFFF9EB5, 0xFFD9A8FF),
		new ColorTheme("Neon", 0xFFFF2BD6, 0xFF2BC8FF, 0xFFA6FF2B),
		new ColorTheme("Mono", 0xFFFFFFFF, 0xFF8C8C8C)
	);

	public static final NumberSetting SPEED = new NumberSetting("ThemeSpeed", 1, 0.1, 5, 0.1);
	public static final ThemeSetting SETTING = new ThemeSetting();

	private static int index;
	private static double phase;
	private static long lastNanos = System.nanoTime();

	private ThemeManager() {
	}

	public static ColorTheme current() {
		return THEMES.get(index);
	}

	public static int currentIndex() {
		return index;
	}

	public static boolean select(String name) {
		for (int i = 0; i < THEMES.size(); i++) {
			if (THEMES.get(i).name().equalsIgnoreCase(name)) {
				index = i;
				return true;
			}
		}
		return false;
	}

	public static void select(int newIndex) {
		index = Math.floorMod(newIndex, THEMES.size());
	}

	/** Animation clock in seconds. It runs faster or slower with ThemeSpeed without ever jumping. */
	public static float phase() {
		long now = System.nanoTime();
		double delta = Math.min((now - lastNanos) / 1.0e9, 0.1);
		lastNanos = now;
		phase += delta * SPEED.get();
		return (float) phase;
	}

	/** The theme colour at the current moment. The offset shifts the position along the gradient (use it to vary colours between rows). */
	public static int accent(float offset) {
		int[] colors = current().colors();
		float position = phase() * 0.15f + offset;
		position -= (float) Math.floor(position);
		float triangle = 1f - Math.abs(2f * position - 1f);
		float scaled = triangle * (colors.length - 1);
		int i = Math.min((int) scaled, colors.length - 2);
		return Draw.lerp(colors[i], colors[i + 1], scaled - i);
	}

	public static int accent() {
		return accent(0f);
	}
}
