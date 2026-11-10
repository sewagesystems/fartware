package fartware.client.module.impl;

import fartware.client.FartwareClient;
import fartware.client.gui.ClickGuiScreen;
import fartware.client.gui.ThemeManager;
import fartware.client.module.Category;
import fartware.client.module.Module;
import fartware.client.setting.FontSetting;
import fartware.client.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public class ClickGuiModule extends Module {
	private final NumberSetting uiScale = add(new NumberSetting("UiScale", 1, 0.5, 3, 0.25));
	private final NumberSetting blur = add(new NumberSetting("Blur", 4, 0, 10, 1));

	private ClickGuiScreen screen;

	public ClickGuiModule() {
		super("ClickGui", "Opens the click GUI, sets its theme and the font.", Category.MISC);
		add(ThemeManager.SETTING);
		add(ThemeManager.SPEED);
		add(new FontSetting());
		setVisible(false);
		setKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	public float getUiScale() {
		return uiScale.floatValue();
	}

	/** Strength of the background blur behind the GUI, 0 = off. */
	public float getBlur() {
		return blur.floatValue();
	}

	/** Never stays enabled: toggling just opens the GUI. */
	@Override
	public void toggle() {
		// Deferred so it also works when triggered from chat, where the chat screen closes right after the command runs.
		FartwareClient.runLater(() -> {
			if (mc.level == null || mc.screen instanceof ClickGuiScreen) {
				return;
			}
			if (screen == null) {
				screen = new ClickGuiScreen();
			}
			mc.setScreen(screen);
		});
	}

	@Override
	public void setEnabled(boolean enabled) {
		if (enabled) {
			toggle();
		}
	}
}
