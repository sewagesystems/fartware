package fartware.client.rotation;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;

/**
 * Lets a module change which rotation the game uses without changing the camera.
 * Three rotations can be involved in a tick:
 * <ul>
 *   <li>the camera rotation: what the player sees and looks along. Silent modules leave it alone.</li>
 *   <li>the server rotation: what is sent to the server in the movement packet.</li>
 *   <li>the movement rotation: the frame the player's movement is calculated in.</li>
 * </ul>
 * The game reads all three from the player's one yaw and pitch, so this swaps them in only while they are needed:
 * the movement rotation for the player's tick, and the server rotation while the position packet is built. The hooks
 * are called from {@code LocalPlayerMixin} and {@code KeyboardInputMixin}.
 * <p>
 * Moving in a different frame than the camera's would send the player off in the wrong direction, so while the
 * movement rotation differs from the camera's the movement keys can be corrected: turned by the difference, so
 * the player still moves the way they are steering, calculated in the movement rotation.
 */
public final class RotationManager {
	private static boolean serverActive;
	private static float serverYaw;
	private static float serverPitch;

	private static boolean movementActive;
	private static float movementYaw;
	private static float movementPitch;

	private static boolean correctionActive;
	private static float intendedYaw;

	private static boolean tickSwapped;
	private static float cameraYaw;
	private static float cameraPitch;

	private static boolean sendSwapped;
	private static float beforeSendYaw;
	private static float beforeSendPitch;

	private RotationManager() {
	}

	/** Send this rotation to the server instead of the camera's. */
	public static void setServer(float yaw, float pitch) {
		serverActive = true;
		serverYaw = yaw;
		serverPitch = pitch;
	}

	/** Calculate the player's movement in this rotation instead of the camera's. */
	public static void setMovement(float yaw, float pitch) {
		movementActive = true;
		movementYaw = yaw;
		movementPitch = pitch;
	}

	/**
	 * Turn the movement keys so the player keeps moving along the given yaw (the direction they are steering relative to)
	 * even though the movement is calculated in the movement rotation.
	 */
	public static void correctInputTowards(float yaw) {
		correctionActive = true;
		intendedYaw = yaw;
	}

	public static void clear() {
		serverActive = false;
		movementActive = false;
		correctionActive = false;
	}

	public static void beforeTick(LocalPlayer player) {
		if (tickSwapped) {
			afterTick(player);
		}
		if (!movementActive) {
			return;
		}
		cameraYaw = player.getYRot();
		cameraPitch = player.getXRot();
		player.setYRot(movementYaw);
		player.setXRot(movementPitch);
		tickSwapped = true;
	}

	public static void afterTick(LocalPlayer player) {
		if (!tickSwapped) {
			return;
		}
		player.setYRot(cameraYaw);
		player.setXRot(cameraPitch);
		// Without this the camera would be interpolated from the movement rotation to the real one every frame.
		player.yRotO = cameraYaw;
		player.xRotO = cameraPitch;
		tickSwapped = false;
	}

	/** Called right after the movement keys have been turned into forward and sideways movement. */
	public static void correctInput(Input input) {
		if (!tickSwapped || !correctionActive) {
			return;
		}
		// With the movement rotation A and the intended one C, forward and left as a complex number
		// (left + i * forward) rotates by C - A to give the same direction in the other frame.
		double difference = Math.toRadians(intendedYaw - movementYaw);
		double cos = Math.cos(difference);
		double sin = Math.sin(difference);
		float forward = input.forwardImpulse;
		float left = input.leftImpulse;
		input.forwardImpulse = (float) (forward * cos + left * sin);
		input.leftImpulse = (float) (left * cos - forward * sin);
	}

	public static void beforeSend(LocalPlayer player) {
		float yaw;
		float pitch;
		if (serverActive) {
			yaw = serverYaw;
			pitch = serverPitch;
		} else if (tickSwapped) {
			// The player currently holds the movement rotation, but the server should still get the camera's.
			yaw = cameraYaw;
			pitch = cameraPitch;
		} else {
			return;
		}
		beforeSendYaw = player.getYRot();
		beforeSendPitch = player.getXRot();
		player.setYRot(yaw);
		player.setXRot(pitch);
		sendSwapped = true;
	}

	public static void afterSend(LocalPlayer player) {
		if (!sendSwapped) {
			return;
		}
		player.setYRot(beforeSendYaw);
		player.setXRot(beforeSendPitch);
		sendSwapped = false;
	}
}
