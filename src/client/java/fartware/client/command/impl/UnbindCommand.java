package fartware.client.command.impl;

import fartware.client.FartwareClient;
import fartware.client.command.Command;
import fartware.client.module.Module;
import fartware.client.util.ChatUtil;
import fartware.client.util.KeyUtil;

public class UnbindCommand extends Command {
	public UnbindCommand() {
		super("unbind", "Removes the key bind of a module.", "<module>", "ub");
	}

	@Override
	public void execute(String[] args) {
		if (args.length != 1) {
			sendUsage();
			return;
		}
		Module module = FartwareClient.modules().get(args[0]);
		if (module == null) {
			ChatUtil.error("Unknown module '" + args[0] + "'.");
			return;
		}
		module.setKey(KeyUtil.NONE);
		ChatUtil.info("Unbound " + module.getName() + ".");
		FartwareClient.config().save();
	}
}
