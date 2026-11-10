package fartware.client.command.impl;

import fartware.client.FartwareClient;
import fartware.client.command.Command;
import fartware.client.util.ChatUtil;
import net.minecraft.ChatFormatting;

public class HelpCommand extends Command {
	public HelpCommand() {
		super("help", "Lists all commands.", "[command]", "h", "?");
	}

	@Override
	public void execute(String[] args) {
		if (args.length > 0) {
			Command command = FartwareClient.commands().find(args[0]);
			if (command == null) {
				ChatUtil.error("Unknown command '" + args[0] + "'.");
				return;
			}
			ChatUtil.info(command.getUsage() + " - " + command.getDescription());
			return;
		}

		ChatUtil.info("Commands:");
		for (Command command : FartwareClient.commands().getAll()) {
			ChatUtil.plain(" " + command.getUsage() + " \u00a77- " + command.getDescription(), ChatFormatting.WHITE);
		}
	}
}
