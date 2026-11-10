package fartware.client.command.impl;

import fartware.client.FartwareClient;
import fartware.client.command.Command;
import fartware.client.util.ChatUtil;

public class PanicCommand extends Command {
	public PanicCommand() {
		super("panic", "Disables every module.", "");
	}

	@Override
	public void execute(String[] args) {
		FartwareClient.modules().disableAll();
		FartwareClient.config().save();
		ChatUtil.info("All modules disabled.");
	}
}
