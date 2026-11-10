package fartware.client;

import fartware.Fartware;
import fartware.client.command.CommandManager;
import fartware.client.config.ConfigManager;
import fartware.client.font.FontManager;
import fartware.client.gui.BlurShader;
import fartware.client.gui.BreatheShader;
import fartware.client.gui.GlowShader;
import fartware.client.render.EspRenderer;
import fartware.client.render.FlatEntityRenderer;
import fartware.client.input.KeybindManager;
import fartware.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FartwareClient implements ClientModInitializer {
	private static final Queue<Runnable> TASKS = new ConcurrentLinkedQueue<>();

	private static ModuleManager modules;
	private static CommandManager commands;
	private static ConfigManager config;
	private static KeybindManager keybinds;
	private static FontManager fonts;

	public static FontManager fonts() {
		return fonts;
	}

	public static ModuleManager modules() {
		return modules;
	}

	public static CommandManager commands() {
		return commands;
	}

	public static ConfigManager config() {
		return config;
	}

	/** Runs the task at the start of the next client tick. */
	public static void runLater(Runnable task) {
		TASKS.add(task);
	}

	@Override
	public void onInitializeClient() {
		modules = new ModuleManager();
		commands = new CommandManager();
		config = new ConfigManager();
		keybinds = new KeybindManager();
		fonts = new FontManager();
		fonts.init();

		BreatheShader.register();
		BlurShader.register();
		GlowShader.register();
		FlatEntityRenderer.register();
		EspRenderer.register();

		config.load();
		config.save();

		ClientTickEvents.START_CLIENT_TICK.register(mc -> {
			Runnable task;
			while ((task = TASKS.poll()) != null) {
				task.run();
			}
			keybinds.tick(mc);
			if (mc.player != null) {
				modules.tick();
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (mc.player != null) {
				modules.postTick();
			}
		});

		HudRenderCallback.EVENT.register(modules::render2D);

		// Messages starting with the command prefix are handled locally and never reach the server.
		ClientSendMessageEvents.ALLOW_CHAT.register(message -> !commands.handle(message));

		ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> config.save());

		Fartware.LOGGER.info("fartware loaded with {} modules and {} commands", modules.getAll().size(), commands.getAll().size());
	}
}
