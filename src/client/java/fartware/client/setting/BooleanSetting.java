package fartware.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BooleanSetting extends Setting<Boolean> {
	public BooleanSetting(String name, boolean defaultValue) {
		super(name, defaultValue);
	}

	public void toggle() {
		value = !value;
	}

	@Override
	public String display() {
		return value ? "ON" : "OFF";
	}

	@Override
	public String hint() {
		return "true/false";
	}

	@Override
	public boolean parse(String input) {
		switch (input.toLowerCase()) {
			case "true", "on", "yes", "1" -> value = true;
			case "false", "off", "no", "0" -> value = false;
			case "toggle" -> toggle();
			default -> {
				return false;
			}
		}
		return true;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement element) {
		value = element.getAsBoolean();
	}
}
