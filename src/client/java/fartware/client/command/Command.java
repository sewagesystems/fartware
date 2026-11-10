package fartware.client.command;

import fartware.client.util.ChatUtil;

public abstract class Command {
	private final String name;
	private final String description;
	private final String usage;
	private final String[] aliases;

	protected Command(String name, String description, String usage, String... aliases) {
		this.name = name;
		this.description = description;
		this.usage = usage;
		this.aliases = aliases;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getUsage() {
		return CommandManager.PREFIX + name + (usage.isEmpty() ? "" : " " + usage);
	}

	public boolean matches(String input) {
		if (name.equalsIgnoreCase(input)) {
			return true;
		}
		for (String alias : aliases) {
			if (alias.equalsIgnoreCase(input)) {
				return true;
			}
		}
		return false;
	}

	protected void sendUsage() {
		ChatUtil.error("Usage: " + getUsage());
	}

	public abstract void execute(String[] args);
}
