package fartware.client.mixin;

import fartware.client.rotation.RotationManager;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hooks for {@link RotationManager}: the player's tick, and the building of the position and rotation packet. */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void fartware$beforeTick(CallbackInfo info) {
		RotationManager.beforeTick((LocalPlayer) (Object) this);
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void fartware$afterTick(CallbackInfo info) {
		RotationManager.afterTick((LocalPlayer) (Object) this);
	}

	@Inject(method = "sendPosition", at = @At("HEAD"))
	private void fartware$beforeSendPosition(CallbackInfo info) {
		RotationManager.beforeSend((LocalPlayer) (Object) this);
	}

	@Inject(method = "sendPosition", at = @At("RETURN"))
	private void fartware$afterSendPosition(CallbackInfo info) {
		RotationManager.afterSend((LocalPlayer) (Object) this);
	}
}
