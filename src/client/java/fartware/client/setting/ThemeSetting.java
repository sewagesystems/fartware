package fartware.client.setting;

import fartware.client.gui.ThemeManager;

import java.util.List;

/** Picks the colour theme. The selection itself lives in {@link ThemeManager}. */
public class ThemeSetting extends ModeSetting {
	public ThemeSetting() {
		super("Theme", "Ocean");
	}

	@Override
	protected List<String> modes() {
		return ThemeManager.THEMES.stream().map(ThemeManager.ColorTheme::name).toList();
	}

	@Override
	public String get() {
		return ThemeManager.current().name();
	}

	@Override
	public void set(String name) {
		ThemeManager.select(name);
	}

	@Override
	public String display() {
		return get() + " (" + ThemeManager.THEMES.size() + ")";
	}
}
