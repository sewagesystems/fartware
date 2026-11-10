package fartware.client.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class NumberSetting extends Setting<Double> {
	private final double min;
	private final double max;
	private final double step;

	public NumberSetting(String name, double defaultValue, double min, double max, double step) {
		super(name, defaultValue);
		this.min = min;
		this.max = max;
		this.step = step;
		set(defaultValue);
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public float floatValue() {
		return value.floatValue();
	}

	public int intValue() {
		return value.intValue();
	}

	@Override
	public void set(Double newValue) {
		double snapped = Math.round((newValue - min) / step) * step + min;
		snapped = Math.round(snapped * 10000.0) / 10000.0;
		this.value = Math.max(min, Math.min(max, snapped));
	}

	/** Sets the value from a 0..1 fraction of the range (used by the slider). */
	public void setFraction(double fraction) {
		set(min + Math.max(0, Math.min(1, fraction)) * (max - min));
	}

	public double getFraction() {
		return (value - min) / (max - min);
	}

	@Override
	public String display() {
		return new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT)).format(value);
	}

	@Override
	public String hint() {
		return "number from " + min + " to " + max;
	}

	@Override
	public boolean parse(String input) {
		try {
			double parsed = Double.parseDouble(input);
			if (parsed < min || parsed > max) {
				return false;
			}
			set(parsed);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement element) {
		set(element.getAsDouble());
	}
}
