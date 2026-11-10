package fartware.client.module.impl;

import fartware.client.module.Category;
import fartware.client.module.Module;
import fartware.client.setting.NumberSetting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;

public class FlightModule extends Module {
	private static final float VANILLA_FLY_SPEED = 0.05f;

	private final NumberSetting speed = add(new NumberSetting("Speed", 2, 1, 10, 0.5));

	public FlightModule() {
		super("Flight", "Lets you fly like in creative mode.", Category.MOVEMENT);
	}

	@Override
	public String getSuffix() {
		return speed.display();
	}

	@Override
	public void onTick() {
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}
		Abilities abilities = player.getAbilities();
		abilities.mayfly = true;
		abilities.flying = true;
		abilities.setFlyingSpeed(speed.floatValue() * VANILLA_FLY_SPEED);
	}

	@Override
	protected void onDisable() {
		LocalPlayer player = mc.player;
		if (player == null) {
			return;
		}
		Abilities abilities = player.getAbilities();
		abilities.setFlyingSpeed(VANILLA_FLY_SPEED);
		if (!player.isCreative() && !player.isSpectator()) {
			abilities.mayfly = false;
			abilities.flying = false;
		}
	}
}
