package fartware.client.module;

import fartware.Fartware;
import fartware.client.setting.ModeSetting;
import fartware.client.setting.Setting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

public abstract class Module {
	protected static final Minecraft mc = Minecraft.getInstance();

	private final String name;
	private final String description;
	private final Category category;
	private final List<Setting<?>> settings = new ArrayList<>();

	private int key = -1;
	private boolean enabled;
	private boolean visible = true;

	protected Module(String name, String description, Category category) {
		this.name = name;
		this.description = description;
		this.category = category;
	}

	protected <S extends Setting<?>> S add(S setting) {
		settings.add(setting);
		return setting;
	}

	/** Adds a setting that is only shown while the condition is true, e.g. while a certain mode is selected. */
	protected <S extends Setting<?>> S add(S setting, BooleanSupplier visibleWhen) {
		setting.setVisibility(visibleWhen);
		return add(setting);
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Category getCategory() {
		return category;
	}

	public List<Setting<?>> getSettings() {
		return Collections.unmodifiableList(settings);
	}

	/** The settings that currently apply, in order. */
	public List<Setting<?>> getVisibleSettings() {
		return settings.stream().filter(Setting::isVisible).toList();
	}

	public Setting<?> findSetting(String settingName) {
		for (Setting<?> setting : settings) {
			if (setting.getName().equalsIgnoreCase(settingName)) {
				return setting;
			}
		}
		return null;
	}

	public int getKey() {
		return key;
	}

	public void setKey(int key) {
		this.key = key;
	}

	public boolean isEnabled() {
		return enabled;
	}

	/** Whether the module is listed in the HUD array list. */
	public boolean isVisible() {
		return visible;
	}

	protected void setVisible(boolean visible) {
		this.visible = visible;
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	public void setEnabled(boolean enabled) {
		if (this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;
		try {
			if (enabled) {
				onEnable();
			} else {
				onDisable();
			}
		} catch (Exception e) {
			Fartware.LOGGER.error("Module {} failed to {}", name, enabled ? "enable" : "disable", e);
		}
	}

	/**
	 * Extra text shown in grey next to the module name in the array list. By default this is the value of the
	 * module's "Mode" setting, if it has one. Modules can override it to show something else.
	 */
	public String getSuffix() {
		Setting<?> mode = findSetting("Mode");
		return mode instanceof ModeSetting ? mode.display() : null;
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	/** Called at the start of every client tick while the module is enabled, before the player is ticked. */
	public void onTick() {
	}

	/**
	 * Called at the end of every client tick while the module is enabled, after the player has ticked and
	 * sent its position and rotation for the tick. Use it for actions that must reach the server after those.
	 */
	public void onPostTick() {
	}

	/** Called every HUD frame while the module is enabled. */
	public void onRender2D(GuiGraphics graphics, DeltaTracker deltaTracker) {
	}
}
