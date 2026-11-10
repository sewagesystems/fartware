package fartware.client.module;

import fartware.Fartware;
import fartware.client.module.impl.AntiAfkModule;
import fartware.client.module.impl.ClickGuiModule;
import fartware.client.module.impl.EspModule;
import fartware.client.module.impl.FlightModule;
import fartware.client.module.impl.FullbrightModule;
import fartware.client.module.impl.HudModule;
import fartware.client.module.impl.KillauraModule;
import fartware.client.module.impl.SpeedModule;
import fartware.client.module.impl.SprintModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleManager {
	private final List<Module> modules = new ArrayList<>();
	private final ClickGuiModule clickGui = new ClickGuiModule();
	private final EspModule esp = new EspModule();

	public ModuleManager() {
		register(clickGui);
		register(new KillauraModule());
		register(new SprintModule());
		register(new FlightModule());
		register(new SpeedModule());
		register(new FullbrightModule());
		register(esp);
		register(new HudModule());
		register(new AntiAfkModule());
	}

	/** Add new modules in the constructor above. */
	private void register(Module module) {
		modules.add(module);
	}

	public List<Module> getAll() {
		return Collections.unmodifiableList(modules);
	}

	public List<Module> getByCategory(Category category) {
		return modules.stream().filter(module -> module.getCategory() == category).toList();
	}

	public Module get(String name) {
		for (Module module : modules) {
			if (module.getName().equalsIgnoreCase(name)) {
				return module;
			}
		}
		return null;
	}

	public ClickGuiModule clickGui() {
		return clickGui;
	}

	public EspModule esp() {
		return esp;
	}

	public void tick() {
		for (Module module : modules) {
			if (module.isEnabled()) {
				try {
					module.onTick();
				} catch (Exception e) {
					Fartware.LOGGER.error("Module {} threw during tick", module.getName(), e);
				}
			}
		}
	}

	public void postTick() {
		for (Module module : modules) {
			if (module.isEnabled()) {
				try {
					module.onPostTick();
				} catch (Exception e) {
					Fartware.LOGGER.error("Module {} threw during post tick", module.getName(), e);
				}
			}
		}
	}

	public void render2D(GuiGraphics graphics, DeltaTracker deltaTracker) {
		for (Module module : modules) {
			if (module.isEnabled()) {
				try {
					module.onRender2D(graphics, deltaTracker);
				} catch (Exception e) {
					Fartware.LOGGER.error("Module {} threw during render", module.getName(), e);
				}
			}
		}
	}

	public void disableAll() {
		for (Module module : modules) {
			module.setEnabled(false);
		}
	}
}
