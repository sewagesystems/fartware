package fartware.client.command.impl;

import fartware.client.FartwareClient;
import fartware.client.command.Command;
import fartware.client.util.ChatUtil;

public class ConfigCommand extends Command {
	public ConfigCommand() {
		super("config", "Saves or reloads the config file.", "<save|load>", "cfg");
	}

	@Override
	public void execute(String[] args) {
		if (args.length != 1) {
			sendUsage();
			return;
		}
		switch (args[0].toLowerCase()) {
			case "save" -> {
				FartwareClient.config().save();
				ChatUtil.info("Config saved.");
			}
			case "load" -> {
				FartwareClient.config().load();
				ChatUtil.info("Config loaded.");
			}
			default -> sendUsage();
		}
	}
}
