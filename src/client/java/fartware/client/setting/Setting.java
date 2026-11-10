package fartware.client.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
	private final String name;
	private BooleanSupplier visibility = () -> true;
	protected T value;

	protected Setting(String name, T defaultValue) {
		this.name = name;
		this.value = defaultValue;
	}

	public String getName() {
		return name;
	}

	public T get() {
		return value;
	}

	public void set(T value) {
		this.value = value;
	}

	/**
	 * Whether the setting is currently shown in the click GUI and listed by .set.
	 * Hidden settings keep their value and are still saved, so a module can hide
	 * the settings that don't apply to its current mode.
	 */
	public boolean isVisible() {
		return visibility.getAsBoolean();
	}

	public void setVisibility(BooleanSupplier visibility) {
		this.visibility = visibility;
	}

	/** Current value as shown to the user. */
	public abstract String display();

	/** Describes the accepted values, used in error messages. */
	public abstract String hint();

	/** Parses user input (from the .set command). Returns false if the input was invalid. */
	public abstract boolean parse(String input);

	public abstract JsonElement toJson();

	public abstract void fromJson(JsonElement element);
}
