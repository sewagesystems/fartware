package fartware.client.setting;

import com.google.gson.JsonElement;
import fartware.client.FartwareClient;
import fartware.client.font.FontManager;

import java.util.ArrayList;
import java.util.List;

/**
 * A mode setting that lists every font in the font folder (plus the vanilla font) and applies the choice immediately.
 * The font itself is persisted by the config's "font" section, not by this setting.
 */
public class FontSetting extends ModeSetting {
	public FontSetting() {
		super("Font", FontManager.VANILLA);
	}

	@Override
	protected List<String> modes() {
		List<String> modes = new ArrayList<>();
		modes.add(FontManager.VANILLA);
		modes.addAll(FartwareClient.fonts().list());
		return modes;
	}

	@Override
	public String get() {
		FontManager fonts = FartwareClient.fonts();
		return fonts.isCustom() ? fonts.getCurrentName() : FontManager.VANILLA;
	}

	@Override
	public void set(String name) {
		FartwareClient.fonts().select(name);
	}

	@Override
	public String display() {
		String name = get();
		int dot = name.lastIndexOf('.');
		return dot > 0 ? name.substring(0, dot) : name;
	}

	@Override
	public void fromJson(JsonElement element) {
	}
}
