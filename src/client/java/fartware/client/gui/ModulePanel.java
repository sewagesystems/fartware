package fartware.client.gui;

import fartware.client.module.Category;
import fartware.client.module.Module;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public class ModulePanel extends Panel {
	private final List<ModuleButton> buttons;

	public ModulePanel(Category category, List<Module> modules, int x, int y) {
		super(category.getDisplayName(), x, y);
		this.buttons = modules.stream().map(ModuleButton::new).toList();
	}

	@Override
	protected void renderBody(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
		int rowY = y;
		for (ModuleButton button : buttons) {
			button.render(graphics, x, rowY, WIDTH, mouseX, mouseY);
			rowY += button.height();
		}
	}

	@Override
	protected boolean bodyClicked(double mouseX, double mouseY, int button) {
		for (ModuleButton moduleButton : buttons) {
			if (moduleButton.mouseClicked(mouseX, mouseY, button)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void mouseDragged(double mouseX, double mouseY) {
		buttons.forEach(button -> button.mouseDragged(mouseX, mouseY));
	}

	@Override
	public void mouseReleased() {
		buttons.forEach(ModuleButton::stopDragging);
	}

	@Override
	public boolean keyPressed(int keyCode) {
		for (ModuleButton button : buttons) {
			if (button.isBinding()) {
				button.bindKey(keyCode);
				return true;
			}
		}
		return false;
	}

	@Override
	public void closed() {
		super.closed();
		buttons.forEach(ModuleButton::stopBinding);
	}
}
