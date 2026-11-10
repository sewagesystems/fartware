package fartware.client.gui;

import fartware.client.FartwareClient;
import fartware.client.font.FontManager;
import fartware.client.module.Module;
import fartware.client.setting.Setting;
import fartware.client.util.KeyUtil;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public class ModuleButton {
	public static final int ROW_HEIGHT = Layout.ROW_HEIGHT;

	private final Module module;
	private boolean expanded;
	private boolean binding;
	private SettingRow.Drag activeDrag;

	private int x;
	private int y;
	private int width;

	public ModuleButton(Module module) {
		this.module = module;
	}

	public boolean isBinding() {
		return binding;
	}

	public void stopBinding() {
		binding = false;
	}

	/** Handles a key press while in bind mode. */
	public void bindKey(int keyCode) {
		boolean clear = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE;
		module.setKey(clear ? KeyUtil.NONE : keyCode);
		binding = false;
	}

	public void stopDragging() {
		activeDrag = null;
	}

	public int height() {
		int height = ROW_HEIGHT;
		if (expanded) {
			height += SettingRow.HEIGHT;
			for (Setting<?> setting : module.getVisibleSettings()) {
				height += SettingRow.heightOf(setting);
			}
		}
		return height;
	}

	public void render(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY) {
		this.x = x;
		this.y = y;
		this.width = width;
		FontManager font = FartwareClient.fonts();

		boolean hovered = inside(mouseX, mouseY, x, y, width, ROW_HEIGHT);
		graphics.fill(x, y, x + width, y + ROW_HEIGHT, hovered ? Theme.ROW_HOVER : Theme.ROW_BG);
		if (module.isEnabled()) {
			BreatheShader.fill(graphics, x, y, x + width, y + ROW_HEIGHT, 0.22f);
		}

		String label = binding ? "Press a key..." : module.getName();
		int textY = SettingRow.textY(y, ROW_HEIGHT);
		if (module.isEnabled()) {
			font.drawAccent(graphics, label, x + Layout.PAD, textY, false);
		} else {
			font.drawString(graphics, label, x + Layout.PAD, textY, Theme.TEXT, false);
		}
		if (!module.getSettings().isEmpty()) {
			String indicator = expanded ? "-" : "+";
			font.drawString(graphics, indicator, x + width - Layout.PAD - font.width(indicator), textY, Theme.TEXT_DIM, false);
		}

		if (!expanded) {
			return;
		}

		int rowY = y + ROW_HEIGHT;
		SettingRow.renderText(graphics, "Bind", binding ? "..." : KeyUtil.name(module.getKey()), x, rowY, width);
		rowY += SettingRow.HEIGHT;
		for (Setting<?> setting : module.getVisibleSettings()) {
			SettingRow.render(graphics, setting, x, rowY, width);
			rowY += SettingRow.heightOf(setting);
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (inside(mouseX, mouseY, x, y, width, ROW_HEIGHT)) {
			switch (button) {
				case GLFW.GLFW_MOUSE_BUTTON_LEFT -> module.toggle();
				case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> expanded = !expanded;
				case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> binding = true;
				default -> {
					return false;
				}
			}
			return true;
		}
		if (!expanded) {
			return false;
		}

		int rowY = y + ROW_HEIGHT;
		if (inside(mouseX, mouseY, x, rowY, width, SettingRow.HEIGHT)) {
			binding = button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
			return true;
		}
		rowY += SettingRow.HEIGHT;

		for (Setting<?> setting : module.getVisibleSettings()) {
			if (inside(mouseX, mouseY, x, rowY, width, SettingRow.heightOf(setting))) {
				SettingRow.Drag drag = SettingRow.click(setting, mouseX, mouseY, button, x, rowY, width);
				if (drag != null) {
					activeDrag = drag;
				}
				return true;
			}
			rowY += SettingRow.heightOf(setting);
		}
		return false;
	}

	public void mouseDragged(double mouseX, double mouseY) {
		if (activeDrag != null) {
			activeDrag.drag(mouseX, mouseY);
		}
	}

	private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
