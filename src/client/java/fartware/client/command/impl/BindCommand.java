package fartware.client.command.impl;

import fartware.client.FartwareClient;
import fartware.client.command.Command;
import fartware.client.module.Module;
import fartware.client.util.ChatUtil;
import fartware.client.util.KeyUtil;

public class BindCommand extends Command {
	public BindCommand() {
		super("bind", "Binds a module to a key (use 'none' to clear).", "<module> <key>", "b");
	}

	@Override
	public void execute(String[] args) {
		if (args.length < 1 || args.length > 2) {
			sendUsage();
			return;
		}
		Module module = FartwareClient.modules().get(args[0]);
		if (module == null) {
			ChatUtil.error("Unknown module '" + args[0] + "'.");
			return;
		}
		if (args.length == 1) {
			ChatUtil.info(module.getName() + " is bound to " + KeyUtil.name(module.getKey()) + ".");
			return;
		}
		if (!KeyUtil.isValid(args[1])) {
			ChatUtil.error("Unknown key '" + args[1] + "'. Examples: r, f6, rshift, lctrl, none.");
			return;
		}
		module.setKey(KeyUtil.parse(args[1]));
		ChatUtil.info("Bound " + module.getName() + " to " + KeyUtil.name(module.getKey()) + ".");
		FartwareClient.config().save();
	}
}
