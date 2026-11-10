package fartware.client.gui;

import fartware.client.FartwareClient;
import fartware.client.font.FontManager;
import fartware.client.module.Category;
import fartware.client.module.Module;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {
	private static final int SCROLL_SPEED = 12;
	/** The blurred background is darkened slightly so the panels stand out. */
	private static final float BACKGROUND_BRIGHTNESS = 0.8f;

	/** The last panel is drawn on top. */
	private final List<Panel> panels = new ArrayList<>();
	private Panel draggedPanel;
	private int dragOffsetX;
	private int dragOffsetY;

	public ClickGuiScreen() {
		super(Component.literal("fartware"));
		int x = Layout.GAP;
		for (Category category : Category.values()) {
			List<Module> modules = FartwareClient.modules().getByCategory(category);
			if (modules.isEmpty()) {
				continue;
			}
			panels.add(new ModulePanel(category, modules, x, Layout.GAP));
			x += Panel.WIDTH + Layout.GAP;
		}
		panels.add(new ThemePanel(x, Layout.GAP));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private float scale() {
		return FartwareClient.modules().clickGui().getUiScale();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		BlurShader.apply(graphics, FartwareClient.modules().clickGui().getBlur(), BACKGROUND_BRIGHTNESS);

		float scale = scale();
		graphics.pose().pushPose();
		graphics.pose().scale(scale, scale, 1f);
		for (Panel panel : panels) {
			panel.render(graphics, (int) (mouseX / scale), (int) (mouseY / scale));
		}
		graphics.pose().popPose();

		FontManager fonts = FartwareClient.fonts();
		String hint = "LMB: toggle  |  RMB: settings  |  MMB: bind  |  Drag headers to move";
		fonts.drawString(graphics, hint, (width - fonts.width(hint)) / 2, fonts.baselineAligned(height - Layout.GAP), Theme.TEXT_DIM, true);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		double x = mouseX / scale();
		double y = mouseY / scale();
		for (int i = panels.size() - 1; i >= 0; i--) {
			Panel panel = panels.get(i);
			if (panel.headerContains(x, y)) {
				bringToFront(panel);
				if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
					draggedPanel = panel;
					dragOffsetX = (int) x - panel.getX();
					dragOffsetY = (int) y - panel.getY();
				} else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
					panel.toggleOpen();
				}
				return true;
			}
			if (panel.mouseClicked(x, y, button)) {
				bringToFront(panel);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		draggedPanel = null;
		panels.forEach(Panel::mouseReleased);
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		double x = mouseX / scale();
		double y = mouseY / scale();
		if (draggedPanel != null) {
			draggedPanel.moveTo((int) x - dragOffsetX, (int) y - dragOffsetY);
			return true;
		}
		panels.forEach(panel -> panel.mouseDragged(x, y));
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int delta = (int) (scrollY * SCROLL_SPEED);
		for (Panel panel : panels) {
			panel.moveTo(panel.getX(), panel.getY() + delta);
		}
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		for (Panel panel : panels) {
			if (panel.keyPressed(keyCode)) {
				return true;
			}
		}
		if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == FartwareClient.modules().clickGui().getKey()) {
			onClose();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void removed() {
		draggedPanel = null;
		panels.forEach(Panel::closed);
		FartwareClient.config().save();
	}

	private void bringToFront(Panel panel) {
		panels.remove(panel);
		panels.add(panel);
	}
}
