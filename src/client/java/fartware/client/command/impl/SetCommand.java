package fartware.client.command.impl;

import fartware.client.FartwareClient;
import fartware.client.command.Command;
import fartware.client.module.Module;
import fartware.client.setting.BooleanSetting;
import fartware.client.setting.Setting;
import fartware.client.util.ChatUtil;

public class SetCommand extends Command {
	public SetCommand() {
		super("set", "Shows or changes module settings.", "<module> [setting] [value]", "s");
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

		if (args.length == 1) {
			if (module.getVisibleSettings().isEmpty()) {
				ChatUtil.info(module.getName() + " has no settings.");
				return;
			}
			ChatUtil.info(module.getName() + " settings:");
			for (Setting<?> setting : module.getVisibleSettings()) {
				ChatUtil.info(" " + setting.getName() + " = " + setting.display() + " \u00a77(" + setting.hint() + ")");
			}
			return;
		}

		Setting<?> setting = module.findSetting(args[1]);
		if (setting == null) {
			ChatUtil.error("Unknown setting '" + args[1] + "' for " + module.getName() + ".");
			return;
		}

		if (args.length == 2) {
			if (setting instanceof BooleanSetting bool) {
				bool.toggle();
			} else {
				ChatUtil.info(setting.getName() + " = " + setting.display() + " \u00a77(" + setting.hint() + ")");
				return;
			}
		} else if (!setting.parse(args[2])) {
			ChatUtil.error("Invalid value '" + args[2] + "'. Expected: " + setting.hint() + ".");
			return;
		}
		ChatUtil.info(module.getName() + " " + setting.getName() + " set to " + setting.display() + ".");
		FartwareClient.config().save();
	}
}
