package fartware.client.command.impl;

import fartware.client.FartwareClient;
import fartware.client.command.Command;
import fartware.client.module.Module;
import fartware.client.util.ChatUtil;

public class ToggleCommand extends Command {
	public ToggleCommand() {
		super("toggle", "Toggles a module.", "<module>", "t");
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 1) {
			sendUsage();
			return;
		}
		Module module = FartwareClient.modules().get(args[0]);
		if (module == null) {
			ChatUtil.error("Unknown module '" + args[0] + "'.");
			return;
		}
		module.toggle();
		if (module != FartwareClient.modules().clickGui()) {
			ChatUtil.info(module.getName() + " " + (module.isEnabled() ? "\u00a7aenabled" : "\u00a7cdisabled"));
		}
		FartwareClient.config().save();
	}
}
