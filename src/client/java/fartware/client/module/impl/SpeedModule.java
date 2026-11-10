package fartware.client.module.impl;

import fartware.client.module.Category;
import fartware.client.module.Module;
import fartware.client.setting.BooleanSetting;
import fartware.client.setting.ModeSetting;
import fartware.client.setting.NumberSetting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Moves faster than normal.
 * <p>
 * Vanilla mode: strafing and airstrafing. Every tick the horizontal velocity is set straight from the movement keys,
 * so you change direction instantly on the ground and in the air instead of building up speed slowly.
 * The Speed setting is a multiplier of normal sprinting speed, and Jump hops automatically while moving.
 */
public class SpeedModule extends Module {
	/** Blocks per tick when sprinting on flat ground. */
	private static final double SPRINT_SPEED = 0.2806;
	/** Vanilla's friction multiplier in the air and the factor in its ground acceleration formula. */
	private static final float AIR_FRICTION = 0.91f;
	private static final float GROUND_ACCELERATION = 0.21600002f;

	private final ModeSetting mode = add(new ModeSetting("Mode", "Vanilla", "Vanilla"));
	private final BooleanSetting jump = add(new BooleanSetting("Jump", true), () -> mode.is("Vanilla"));
	private final NumberSetting speed = add(new NumberSetting("Speed", 1.2, 0.5, 5, 0.1), () -> mode.is("Vanilla"));

	public SpeedModule() {
		super("Speed", "Moves faster, with instant strafing on the ground and in the air.", Category.MOVEMENT);
	}

	@Override
	public void onTick() {
		LocalPlayer player = mc.player;
		if (player == null || mc.screen != null || !canMove(player)) {
			return;
		}
		if (mode.is("Vanilla")) {
			vanilla(player);
		}
	}

	private boolean canMove(LocalPlayer player) {
		return !player.isInWater() && !player.isInLava() && !player.onClimbable() && !player.isFallFlying()
			&& !player.isPassenger() && !player.getAbilities().flying && !player.isShiftKeyDown();
	}

	private void vanilla(LocalPlayer player) {
		// The keys are read directly rather than from player.input, which only updates later in the tick,
		// so a direction change takes effect on the same tick.
		float forward = (mc.options.keyUp.isDown() ? 1f : 0f) - (mc.options.keyDown.isDown() ? 1f : 0f);
		float left = (mc.options.keyLeft.isDown() ? 1f : 0f) - (mc.options.keyRight.isDown() ? 1f : 0f);
		if (forward == 0f && left == 0f) {
			return;
		}

		if (jump.get() && player.onGround()) {
			player.jumpFromGround();
		}

		double yaw = Math.toRadians(player.getYRot());
		double x = -Math.sin(yaw) * forward + Math.cos(yaw) * left;
		double z = Math.cos(yaw) * forward + Math.sin(yaw) * left;
		double length = Math.sqrt(x * x + z * z);
		x /= length;
		z /= length;

		// Vanilla adds its own acceleration on top of the velocity later this tick, so leave that out of the
		// target to end up moving exactly as fast as the setting says.
		double velocity = Math.max(0.0, targetSpeed(player) - vanillaAcceleration(player));
		Vec3 motion = player.getDeltaMovement();
		player.setDeltaMovement(x * velocity, motion.y, z * velocity);
	}

	private double targetSpeed(LocalPlayer player) {
		double base = SPRINT_SPEED * speed.get();
		MobEffectInstance effect = player.getEffect(MobEffects.MOVEMENT_SPEED);
		if (effect != null) {
			base *= 1.0 + 0.2 * (effect.getAmplifier() + 1);
		}
		return base;
	}

	/** How much speed vanilla movement adds this tick while a movement key is held. */
	private double vanillaAcceleration(LocalPlayer player) {
		if (!player.onGround()) {
			return player.isSprinting() ? 0.026 : 0.02;
		}
		float friction = player.level().getBlockState(player.blockPosition().below()).getBlock().getFriction() * AIR_FRICTION;
		return player.getSpeed() * (GROUND_ACCELERATION / (friction * friction * friction));
	}
}
