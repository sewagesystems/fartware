package fartware.client.util;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.Locale;
import java.util.Map;

public final class KeyUtil {
	public static final int NONE = -1;

	private static final Map<String, String> ALIASES = Map.ofEntries(
		Map.entry("rshift", "right.shift"),
		Map.entry("lshift", "left.shift"),
		Map.entry("rctrl", "right.control"),
		Map.entry("lctrl", "left.control"),
		Map.entry("rcontrol", "right.control"),
		Map.entry("lcontrol", "left.control"),
		Map.entry("ralt", "right.alt"),
		Map.entry("lalt", "left.alt"),
		Map.entry("esc", "escape"),
		Map.entry("ins", "insert"),
		Map.entry("del", "delete"),
		Map.entry("pgup", "page.up"),
		Map.entry("pgdn", "page.down"),
		Map.entry("return", "enter"),
		Map.entry("backtick", "grave.accent")
	);

	private KeyUtil() {
	}

	/** Converts user input like "r", "rshift" or "f6" to a GLFW key code. Returns {@link #NONE} for "none" or unknown input. */
	public static int parse(String input) {
		String name = input.toLowerCase(Locale.ROOT).trim().replace('_', '.').replace(' ', '.');
		if (name.isEmpty() || name.equals("none")) {
			return NONE;
		}
		name = ALIASES.getOrDefault(name, name);
		try {
			InputConstants.Key key = InputConstants.getKey("key.keyboard." + name);
			if (key.getType() == InputConstants.Type.KEYSYM) {
				return key.getValue();
			}
		} catch (RuntimeException ignored) {
		}
		return NONE;
	}

	public static boolean isValid(String input) {
		return input.equalsIgnoreCase("none") || parse(input) != NONE;
	}

	public static String name(int key) {
		if (key < 0) {
			return "NONE";
		}
		return InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString().toUpperCase(Locale.ROOT);
	}
}
