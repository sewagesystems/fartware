package fartware.client.module.impl;

import fartware.client.module.Category;
import fartware.client.module.Module;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class FullbrightModule extends Module {
	private boolean applied;

	public FullbrightModule() {
		super("Fullbright", "Lets you see in the dark (client-side night vision).", Category.RENDER);
	}

	@Override
	public void onTick() {
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}
		if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
			player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, false, false));
			applied = true;
		}
	}

	@Override
	protected void onDisable() {
		if (applied && mc.player != null) {
			mc.player.removeEffect(MobEffects.NIGHT_VISION);
		}
		applied = false;
	}
}
