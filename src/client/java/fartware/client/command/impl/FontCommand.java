package fartware.client.command.impl;

import fartware.client.FartwareClient;
import fartware.client.command.Command;
import fartware.client.font.FontManager;
import fartware.client.util.ChatUtil;

import java.util.Arrays;
import java.util.List;

public class FontCommand extends Command {
	public FontCommand() {
		super("font", "Manages the font.", "<list|folder|set|size|vanilla|reset>", "fonts");
	}

	@Override
	public void execute(String[] args) {
		FontManager fonts = FartwareClient.fonts();
		String action = args.length == 0 ? "list" : args[0].toLowerCase();

		switch (action) {
			case "list" -> list(fonts);
			case "folder", "open" -> {
				fonts.openDirectory();
				ChatUtil.info("Opened " + fonts.getDirectory());
			}
			case "set" -> {
				if (args.length < 2) {
					sendUsage();
					return;
				}
				String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
				String selected = fonts.select(name);
				if (selected == null) {
					ChatUtil.error("Could not load font '" + name + "'. Use .font list to see available fonts.");
					return;
				}
				ChatUtil.info("Font set to " + selected + ".");
				FartwareClient.config().save();
			}
			case "size" -> {
				if (args.length != 2) {
					sendUsage();
					return;
				}
				try {
					fonts.setSize(Float.parseFloat(args[1]));
					ChatUtil.info("Font size set to " + fonts.getSize() + ".");
					FartwareClient.config().save();
				} catch (NumberFormatException e) {
					ChatUtil.error("Size must be a number from " + FontManager.MIN_SIZE + " to " + FontManager.MAX_SIZE + ".");
				}
			}
			case "vanilla" -> {
				fonts.useVanilla();
				ChatUtil.info("Using the Minecraft font.");
				FartwareClient.config().save();
			}
			case "reset" -> {
				fonts.resetToDefault();
				ChatUtil.info("Font reset to " + FontManager.DEFAULT_FONT + ".");
				FartwareClient.config().save();
			}
			default -> sendUsage();
		}
	}

	private void list(FontManager fonts) {
		List<String> available = fonts.list();
		ChatUtil.info("Current: " + (fonts.isCustom() ? fonts.getCurrentName() + " (size " + fonts.getSize() + ")" : FontManager.VANILLA));
		ChatUtil.info("Available: " + String.join(", ", available) + (available.isEmpty() ? "" : ", " + FontManager.VANILLA));
		ChatUtil.info("Folder: " + fonts.getDirectory() + " (.font folder opens it)");
	}
}
