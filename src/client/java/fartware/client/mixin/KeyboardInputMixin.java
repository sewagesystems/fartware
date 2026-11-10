package fartware.client.mixin;

import fartware.client.rotation.RotationManager;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets {@link RotationManager} turn the movement keys once they have been read. */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
	@Inject(method = "tick", at = @At("RETURN"))
	private void fartware$correctInput(boolean slowDown, float slowDownFactor, CallbackInfo info) {
		RotationManager.correctInput((Input) (Object) this);
	}
}
