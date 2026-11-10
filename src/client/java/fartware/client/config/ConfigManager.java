package fartware.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fartware.Fartware;
import fartware.client.FartwareClient;
import fartware.client.font.FontManager;
import fartware.client.module.Module;
import fartware.client.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path file = FabricLoader.getInstance().getConfigDir().resolve("fartware.json");

	public void save() {
		JsonObject modules = new JsonObject();
		for (Module module : FartwareClient.modules().getAll()) {
			JsonObject entry = new JsonObject();
			entry.addProperty("enabled", module.isEnabled());
			entry.addProperty("key", module.getKey());
			JsonObject settings = new JsonObject();
			for (Setting<?> setting : module.getSettings()) {
				settings.add(setting.getName(), setting.toJson());
			}
			entry.add("settings", settings);
			modules.add(module.getName(), entry);
		}

		JsonObject root = new JsonObject();
		root.add("modules", modules);

		JsonObject font = new JsonObject();
		font.addProperty("size", FartwareClient.fonts().getSize());
		font.addProperty("name", FartwareClient.fonts().isCustom() ? FartwareClient.fonts().getCurrentName() : FontManager.VANILLA);
		root.add("font", font);
		try {
			Files.writeString(file, GSON.toJson(root));
		} catch (IOException e) {
			Fartware.LOGGER.error("Failed to save config", e);
		}
	}

	public void load() {
		if (!Files.exists(file)) {
			return;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
			JsonObject modules = root.getAsJsonObject("modules");
			if (modules != null) {
				for (Module module : FartwareClient.modules().getAll()) {
					if (modules.has(module.getName())) {
						loadModule(module, modules.getAsJsonObject(module.getName()));
					}
				}
			}
			if (root.has("font")) {
				loadFont(root.getAsJsonObject("font"));
			}
		} catch (IOException | RuntimeException e) {
			Fartware.LOGGER.error("Failed to load config", e);
		}
	}

	private void loadFont(JsonObject font) {
		if (font.has("size")) {
			FartwareClient.fonts().setSize(font.get("size").getAsFloat());
		}
		if (font.has("name") && FartwareClient.fonts().select(font.get("name").getAsString()) == null) {
			Fartware.LOGGER.warn("Configured font {} could not be loaded, using {}", font.get("name").getAsString(), FontManager.DEFAULT_FONT);
			FartwareClient.fonts().resetToDefault();
		}
	}

	private void loadModule(Module module, JsonObject entry) {
		if (entry.has("key")) {
			module.setKey(entry.get("key").getAsInt());
		}
		if (entry.has("settings")) {
			JsonObject settings = entry.getAsJsonObject("settings");
			for (Setting<?> setting : module.getSettings()) {
				JsonElement value = settings.get(setting.getName());
				if (value == null) {
					continue;
				}
				try {
					setting.fromJson(value);
				} catch (RuntimeException e) {
					Fartware.LOGGER.warn("Ignoring invalid value for {}.{}", module.getName(), setting.getName());
				}
			}
		}
		if (entry.has("enabled")) {
			module.setEnabled(entry.get("enabled").getAsBoolean());
		}
	}
}
