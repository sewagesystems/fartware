package fartware.client.command.impl;

import fartware.client.FartwareClient;
import fartware.client.command.Command;
import fartware.client.module.Module;
import fartware.client.util.ChatUtil;
import fartware.client.util.KeyUtil;
import net.minecraft.ChatFormatting;

public class BindsCommand extends Command {
	public BindsCommand() {
		super("binds", "Lists all key binds.", "");
	}

	@Override
	public void execute(String[] args) {
		boolean any = false;
		for (Module module : FartwareClient.modules().getAll()) {
			if (module.getKey() < 0) {
				continue;
			}
			if (!any) {
				ChatUtil.info("Key binds:");
				any = true;
			}
			ChatUtil.plain(" " + module.getName() + " \u00a77- \u00a7f" + KeyUtil.name(module.getKey()), ChatFormatting.WHITE);
		}
		if (!any) {
			ChatUtil.info("No modules are bound.");
		}
	}
}
