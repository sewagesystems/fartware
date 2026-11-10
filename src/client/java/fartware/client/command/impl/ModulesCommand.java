package fartware.client.command.impl;

import fartware.client.FartwareClient;
import fartware.client.command.Command;
import fartware.client.module.Category;
import fartware.client.module.Module;
import fartware.client.util.ChatUtil;

import java.util.List;

public class ModulesCommand extends Command {
	public ModulesCommand() {
		super("modules", "Lists all modules.", "", "m");
	}

	@Override
	public void execute(String[] args) {
		ChatUtil.info("Modules (\u00a7aenabled\u00a7f / \u00a77disabled\u00a7f):");
		for (Category category : Category.values()) {
			List<Module> modules = FartwareClient.modules().getByCategory(category);
			if (modules.isEmpty()) {
				continue;
			}
			StringBuilder line = new StringBuilder(" \u00a7e" + category.getDisplayName() + "\u00a7f: ");
			for (int i = 0; i < modules.size(); i++) {
				Module module = modules.get(i);
				line.append(module.isEnabled() ? "\u00a7a" : "\u00a77").append(module.getName());
				if (i < modules.size() - 1) {
					line.append("\u00a7f, ");
				}
			}
			ChatUtil.plain(line.toString(), net.minecraft.ChatFormatting.WHITE);
		}
	}
}
