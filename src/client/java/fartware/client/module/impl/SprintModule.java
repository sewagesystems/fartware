package fartware.client.module.impl;

import fartware.client.module.Category;
import fartware.client.module.Module;
import net.minecraft.client.player.LocalPlayer;

public class SprintModule extends Module {
	public SprintModule() {
		super("Sprint", "Automatically sprints while moving forward.", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}
		boolean hasFood = player.getFoodData().getFoodLevel() > 6 || player.getAbilities().mayfly;
		if (player.input.forwardImpulse > 0 && hasFood && !player.isSprinting() && !player.isShiftKeyDown()
			&& !player.isUsingItem() && !player.horizontalCollision) {
			player.setSprinting(true);
		}
	}
}
