package fartware.client.module.impl;

import fartware.client.module.Category;
import fartware.client.module.Module;
import fartware.client.rotation.RotationManager;
import fartware.client.setting.BooleanSetting;
import fartware.client.setting.NumberSetting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Turns towards the closest target in range and attacks it.
 * <ul>
 *   <li>Rotation: turns at up to RotationSpeed degrees per tick, aiming at a point that is randomly off by
 *       between RotationMin and RotationMax degrees, re-rolled every few ticks.</li>
 *   <li>Silent: the rotation is only sent to the server. The camera stays as it is.</li>
 *   <li>FixMovement (silent only): the player's movement is calculated in the aura's rotation, the one the server
 *       sees, rather than the camera's. The movement keys are turned to match, so the player still moves the way
 *       they are steering. Without Silent the camera follows the aura, so movement is based on it anyway.</li>
 *   <li>Hit delay: each hit happens DelayMin to DelayMax milliseconds after the attack cooldown is ready, rolled
 *       again after every hit. Negative values are early (the cooldown is not quite full yet), positive are late.
 *       Hits are made at the start of a tick (before that tick's rotation is sent), so the timing is rounded to the 50 ms tick.</li>
 *   <li>AutoBlock: raises a shield between hits. It is lowered just before each hit and raised again right after.</li>
 * </ul>
 * Three rotations are kept apart: the player's own (camera and mouse, without the aura), the aura's, and what is
 * applied. The swapping that lets the server and the movement see different rotations is in {@link RotationManager}.
 */
public class KillauraModule extends Module {
	/** Targets are turned towards this much beyond the attack range, so the aim is ready when they step in. */
	private static final double AIM_EXTRA_RANGE = 1.0;
	/** How close the aura rotation has to get back to the player's own before it lets go. */
	private static final float RELEASE_THRESHOLD = 0.5f;

	private final NumberSetting range = add(new NumberSetting("Range", 3.0, 2, 6, 0.1));
	private final BooleanSetting players = add(new BooleanSetting("Players", true));
	private final BooleanSetting mobs = add(new BooleanSetting("Mobs", false));
	private final BooleanSetting invisible = add(new BooleanSetting("Invisible", false));
	private final BooleanSetting walls = add(new BooleanSetting("Walls", false));
	private final BooleanSetting autoBlock = add(new BooleanSetting("AutoBlock", false));
	private final NumberSetting rotationSpeed = add(new NumberSetting("RotationSpeed", 45, 5, 180, 5));
	private final NumberSetting rotationMin = add(new NumberSetting("RotationMin", 0.3, 0, 10, 0.1));
	private final NumberSetting rotationMax = add(new NumberSetting("RotationMax", 1.5, 0, 10, 0.1));
	private final BooleanSetting silent = add(new BooleanSetting("Silent", true));
	// Without Silent the camera itself follows the aura, so movement already is based on its rotation.
	private final BooleanSetting fixMovement = add(new BooleanSetting("FixMovement", false), silent::get);
	private final NumberSetting delayMin = add(new NumberSetting("DelayMin", -25, -150, 150, 5));
	private final NumberSetting delayMax = add(new NumberSetting("DelayMax", 25, -150, 150, 5));

	private final Random random = new Random();

	private LivingEntity target;
	private boolean blocking;
	private float yawOffset;
	private float pitchOffset;
	private int offsetTicksLeft;
	private long readySince;
	private double hitDelayMs;

	/** True while the aura is controlling a rotation (turning to a target, or turning back to the player's own). */
	private boolean engaged;
	private float auraYaw;
	private float auraPitch;
	/** The rotation the player would have without the aura. Only tracked separately from the camera when not silent. */
	private float ownYaw;
	private float ownPitch;

	public KillauraModule() {
		super("Killaura", "Aims at and attacks nearby targets.", Category.COMBAT);
	}

	@Override
	protected void onEnable() {
		target = null;
		engaged = false;
		readySince = 0;
		offsetTicksLeft = 0;
		rollHitDelay();
	}

	@Override
	protected void onDisable() {
		target = null;
		releaseRotation();
		if (mc.player != null) {
			stopBlocking(mc.player);
		}
	}

	/**
	 * Start of the tick, before the player ticks and sends its packets: pick a target, attack it, then turn towards it.
	 * The attack goes out before this tick's rotation does, so the server still has last tick's rotation when it lands.
	 */
	@Override
	public void onTick() {
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null || mc.gameMode == null || mc.screen != null || !player.isAlive()) {
			target = null;
			releaseRotation();
			if (player != null) {
				stopBlocking(player);
			}
			return;
		}
		trackOwnRotation(player);
		target = findTarget(player);
		attack(player);
		updateAuraRotation(player);
		applyRotation(player);
	}

	/** Attacks once the delay has passed and the rotation the server currently has points at the target. */
	private void attack(LocalPlayer player) {
		if (target == null || !isValid(player, target, range.get())) {
			readySince = 0;
			stopBlocking(player);
			return;
		}

		if (hitDelayElapsed(player) && isLookingAt(player, target)) {
			stopBlocking(player);
			mc.gameMode.attack(player, target);
			player.swing(InteractionHand.MAIN_HAND);
			readySince = 0;
			rollHitDelay();
		}
		startBlocking(player);
	}

	private LivingEntity findTarget(LocalPlayer player) {
		LivingEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (!(entity instanceof LivingEntity living) || !isValid(player, living, range.get() + AIM_EXTRA_RANGE)) {
				continue;
			}
			double distance = distanceTo(player, living);
			if (distance < bestDistance) {
				best = living;
				bestDistance = distance;
			}
		}
		return best;
	}

	private boolean isValid(LocalPlayer player, LivingEntity entity, double maxDistance) {
		if (entity == player || !entity.isAlive() || entity instanceof ArmorStand || entity.isSpectator()) {
			return false;
		}
		boolean allowed = (entity instanceof Player && players.get()) || (entity instanceof Mob && mobs.get());
		if (!allowed || (entity.isInvisible() && !invisible.get())) {
			return false;
		}
		return distanceTo(player, entity) <= maxDistance && (walls.get() || player.hasLineOfSight(entity));
	}

	/** Distance from the player's eyes to the closest point of the entity's hitbox, which is how reach is measured. */
	private double distanceTo(LocalPlayer player, LivingEntity entity) {
		return player.getEyePosition().distanceTo(closestPoint(entity.getBoundingBox(), player.getEyePosition()));
	}

	private Vec3 closestPoint(AABB box, Vec3 from) {
		return new Vec3(
			Mth.clamp(from.x, box.minX, box.maxX),
			Mth.clamp(from.y, box.minY, box.maxY),
			Mth.clamp(from.z, box.minZ, box.maxZ));
	}

	/**
	 * Works out the rotation the player has without the aura. When silent the camera is that rotation. Otherwise the
	 * camera was set to the aura's rotation last tick, so whatever it has moved since is the player's own turning.
	 */
	private void trackOwnRotation(LocalPlayer player) {
		if (silent.get() || !engaged) {
			ownYaw = player.getYRot();
			ownPitch = player.getXRot();
		} else {
			ownYaw += Mth.wrapDegrees(player.getYRot() - auraYaw);
			ownPitch = Mth.clamp(ownPitch + (player.getXRot() - auraPitch), -90f, 90f);
		}
	}

	private void updateAuraRotation(LocalPlayer player) {
		float desiredYaw;
		float desiredPitch;
		if (target != null) {
			Vec3 eye = player.getEyePosition();
			AABB box = target.getBoundingBox();
			Vec3 aim = closestPoint(box, eye);
			if (aim.distanceToSqr(eye) < 1.0E-6) {
				aim = box.getCenter();
			}
			double dx = aim.x - eye.x;
			double dy = aim.y - eye.y;
			double dz = aim.z - eye.z;
			updateOffsets();
			desiredYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0) + yawOffset;
			desiredPitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))) + pitchOffset, -90f, 90f);

			if (!engaged) {
				engaged = true;
				auraYaw = ownYaw;
				auraPitch = ownPitch;
			}
		} else if (engaged) {
			// No target any more: turn back to where the player is really looking.
			desiredYaw = ownYaw;
			desiredPitch = ownPitch;
		} else {
			return;
		}

		float maxStep = rotationSpeed.floatValue();
		float yawStep = Mth.clamp(Mth.wrapDegrees(desiredYaw - auraYaw), -maxStep, maxStep);
		float pitchStep = Mth.clamp(desiredPitch - auraPitch, -maxStep, maxStep);
		auraYaw += snapToMouseSteps(yawStep);
		auraPitch = Mth.clamp(auraPitch + snapToMouseSteps(pitchStep), -90f, 90f);

		if (target == null && Math.abs(Mth.wrapDegrees(ownYaw - auraYaw)) < RELEASE_THRESHOLD
			&& Math.abs(ownPitch - auraPitch) < RELEASE_THRESHOLD) {
			engaged = false;
		}
	}

	/** Hands the aura rotation to the game: to the server only when silent, otherwise to the camera. */
	private void applyRotation(LocalPlayer player) {
		RotationManager.clear();
		if (!engaged) {
			if (!silent.get() && player.getYRot() != ownYaw) {
				player.setYRot(ownYaw);
				player.setXRot(ownPitch);
			}
			return;
		}
		if (silent.get()) {
			RotationManager.setServer(auraYaw, auraPitch);
			if (fixMovement.get()) {
				RotationManager.setMovement(auraYaw, auraPitch);
				// When silent the camera is the player's own rotation, which is the direction they steer by.
				RotationManager.correctInputTowards(ownYaw);
			}
		} else {
			player.setYRot(auraYaw);
			player.setXRot(auraPitch);
		}
	}

	/** Gives the camera back to the player and stops changing what the server and the movement see. */
	private void releaseRotation() {
		RotationManager.clear();
		if (engaged && !silent.get() && mc.player != null) {
			mc.player.setYRot(ownYaw);
			mc.player.setXRot(ownPitch);
		}
		engaged = false;
	}

	/** Rounds a rotation to a multiple of the smallest step the mouse can make at the current sensitivity. */
	private float snapToMouseSteps(float degrees) {
		double sensitivity = mc.options.sensitivity().get() * 0.6 + 0.2;
		double step = sensitivity * sensitivity * sensitivity * 8.0 * 0.15;
		return (float) (Math.round(degrees / step) * step);
	}

	/** Picks a new random aim error every few ticks rather than every tick, so the aim drifts instead of shaking. */
	private void updateOffsets() {
		if (--offsetTicksLeft > 0) {
			return;
		}
		offsetTicksLeft = 3 + random.nextInt(6);
		yawOffset = randomError();
		pitchOffset = randomError();
	}

	private float randomError() {
		double low = Math.min(rotationMin.get(), rotationMax.get());
		double high = Math.max(rotationMin.get(), rotationMax.get());
		double size = low + random.nextDouble() * (high - low);
		return (float) (random.nextBoolean() ? size : -size);
	}

	/**
	 * Whether enough time has passed since the attack cooldown was ready (or, for a negative delay, whether
	 * little enough of the cooldown is left).
	 */
	private boolean hitDelayElapsed(LocalPlayer player) {
		float cooldownTicks = player.getCurrentItemAttackStrengthDelay();
		float charge = player.getAttackStrengthScale(0f);
		long now = System.currentTimeMillis();
		if (charge >= 1f) {
			if (readySince == 0) {
				readySince = now;
			}
		} else {
			readySince = 0;
		}
		double msSinceReady = readySince != 0 ? now - readySince : -(1.0 - charge) * cooldownTicks * 50.0;
		return msSinceReady >= hitDelayMs;
	}

	private void rollHitDelay() {
		double low = Math.min(delayMin.get(), delayMax.get());
		double high = Math.max(delayMin.get(), delayMax.get());
		hitDelayMs = low + random.nextDouble() * (high - low);
	}

	/**
	 * Whether the direction the server thinks the player is looking passes through the target's hitbox.
	 * This runs before this tick's rotation is applied, so that is last tick's aura rotation (or the camera's,
	 * if the aura isn't engaged), which is what the server was last sent.
	 */
	private boolean isLookingAt(LocalPlayer player, LivingEntity entity) {
		float yaw = engaged ? auraYaw : player.getYRot();
		float pitch = engaged ? auraPitch : player.getXRot();
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(Vec3.directionFromRotation(pitch, yaw).scale(range.get() + 0.5));
		return entity.getBoundingBox().inflate(entity.getPickRadius()).clip(eye, end).isPresent();
	}

	private InteractionHand shieldHand(LocalPlayer player) {
		if (player.getOffhandItem().getUseAnimation() == UseAnim.BLOCK) {
			return InteractionHand.OFF_HAND;
		}
		return player.getMainHandItem().getUseAnimation() == UseAnim.BLOCK ? InteractionHand.MAIN_HAND : null;
	}

	private void startBlocking(LocalPlayer player) {
		if (!autoBlock.get() || blocking || player.isUsingItem()) {
			return;
		}
		InteractionHand hand = shieldHand(player);
		if (hand != null && !player.getCooldowns().isOnCooldown(player.getItemInHand(hand).getItem())) {
			mc.gameMode.useItem(player, hand);
			blocking = player.isUsingItem();
		}
	}

	private void stopBlocking(LocalPlayer player) {
		if (blocking && mc.gameMode != null) {
			if (player.isUsingItem()) {
				mc.gameMode.releaseUsingItem(player);
			}
			blocking = false;
		}
	}
}
