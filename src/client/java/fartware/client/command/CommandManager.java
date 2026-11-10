package fartware.client.command;

import fartware.Fartware;
import fartware.client.command.impl.BindCommand;
import fartware.client.command.impl.BindsCommand;
import fartware.client.command.impl.ConfigCommand;
import fartware.client.command.impl.FontCommand;
import fartware.client.command.impl.HelpCommand;
import fartware.client.command.impl.ModulesCommand;
import fartware.client.command.impl.PanicCommand;
import fartware.client.command.impl.SetCommand;
import fartware.client.command.impl.ToggleCommand;
import fartware.client.command.impl.UnbindCommand;
import fartware.client.util.ChatUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandManager {
	public static final String PREFIX = ".";

	private final List<Command> commands = new ArrayList<>();

	public CommandManager() {
		register(new HelpCommand());
		register(new ToggleCommand());
		register(new ModulesCommand());
		register(new BindCommand());
		register(new UnbindCommand());
		register(new BindsCommand());
		register(new SetCommand());
		register(new ConfigCommand());
		register(new FontCommand());
		register(new PanicCommand());
	}

	/** Add new commands in the constructor above. */
	private void register(Command command) {
		commands.add(command);
	}

	public List<Command> getAll() {
		return Collections.unmodifiableList(commands);
	}

	public Command find(String name) {
		for (Command command : commands) {
			if (command.matches(name)) {
				return command;
			}
		}
		return null;
	}

	/**
	 * Handles an outgoing chat message.
	 *
	 * @return true if the message was a command and must not be sent to the server
	 */
	public boolean handle(String message) {
		if (!message.startsWith(PREFIX)) {
			return false;
		}

		String[] parts = message.substring(PREFIX.length()).trim().split("\\s+");
		String name = parts[0];
		if (name.isEmpty()) {
			name = "help";
			parts = new String[] {"help"};
		}

		Command command = find(name);
		if (command == null) {
			ChatUtil.error("Unknown command '" + name + "'. Type " + PREFIX + "help for a list of commands.");
			return true;
		}

		String[] args = new String[parts.length - 1];
		System.arraycopy(parts, 1, args, 0, args.length);
		try {
			command.execute(args);
		} catch (Exception e) {
			Fartware.LOGGER.error("Command {} failed", name, e);
			ChatUtil.error("Command failed: " + e);
		}
		return true;
	}
}
