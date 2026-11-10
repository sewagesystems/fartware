package fartware.client.module.impl;

import fartware.client.module.Category;
import fartware.client.module.Module;
import fartware.client.setting.NumberSetting;
import net.minecraft.client.player.LocalPlayer;

public class AntiAfkModule extends Module {
	private final NumberSetting interval = add(new NumberSetting("Interval", 15, 5, 60, 1));

	private int ticks;
	private boolean flip;

	public AntiAfkModule() {
		super("AntiAFK", "Periodically moves your camera so you don't get kicked for idling.", Category.PLAYER);
	}

	@Override
	protected void onEnable() {
		ticks = 0;
	}

	@Override
	public void onTick() {
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}
		if (++ticks >= interval.intValue() * 20) {
			ticks = 0;
			flip = !flip;
			player.setYRot(player.getYRot() + (flip ? 5f : -5f));
		}
	}
}
