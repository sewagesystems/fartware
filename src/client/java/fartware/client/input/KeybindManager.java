package fartware.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import fartware.client.FartwareClient;
import fartware.client.module.Module;
import fartware.client.util.KeyUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

/** Polls module hotkeys once per tick. Hotkeys only fire in-game with no screen open. */
public class KeybindManager {
	private final Map<Module, Boolean> wasDown = new HashMap<>();

	public void tick(Minecraft mc) {
		long window = mc.getWindow().getWindow();
		boolean canTrigger = mc.level != null && mc.screen == null;

		for (Module module : FartwareClient.modules().getAll()) {
			int key = module.getKey();
			if (key == KeyUtil.NONE) {
				wasDown.remove(module);
				continue;
			}

			boolean down = InputConstants.isKeyDown(window, key);
			boolean previously = wasDown.getOrDefault(module, false);
			wasDown.put(module, down);

			if (down && !previously && canTrigger) {
				module.toggle();
				if (module != FartwareClient.modules().clickGui()) {
					announce(mc, module);
					FartwareClient.config().save();
				}
			}
		}
	}

	private void announce(Minecraft mc, Module module) {
		String state = module.isEnabled() ? "\u00a7aenabled" : "\u00a7cdisabled";
		mc.gui.setOverlayMessage(Component.literal(module.getName() + " " + state), false);
	}
}
