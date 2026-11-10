package fartware.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.List;

public class ModeSetting extends Setting<String> {
	private final List<String> modes;

	public ModeSetting(String name, String defaultValue, String... modes) {
		super(name, defaultValue);
		this.modes = List.of(modes);
	}

	/** The selectable modes. Subclasses can override this to provide a dynamic list. */
	protected List<String> modes() {
		return modes;
	}

	public boolean is(String mode) {
		return get().equalsIgnoreCase(mode);
	}

	public void cycle(boolean forward) {
		List<String> available = modes();
		if (available.isEmpty()) {
			return;
		}
		int index = available.indexOf(get());
		int next = (index + (forward ? 1 : -1) + available.size()) % available.size();
		set(available.get(next));
	}

	@Override
	public String display() {
		return get();
	}

	@Override
	public String hint() {
		return String.join(", ", modes());
	}

	@Override
	public boolean parse(String input) {
		for (String mode : modes()) {
			if (mode.equalsIgnoreCase(input)) {
				set(mode);
				return true;
			}
		}
		return false;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(get());
	}

	@Override
	public void fromJson(JsonElement element) {
		parse(element.getAsString());
	}
}
