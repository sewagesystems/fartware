package fartware.client.gui;

import fartware.client.FartwareClient;
import fartware.client.font.FontManager;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/** Lists every colour theme with a gradient preview and the animation speed slider. */
public class ThemePanel extends Panel {
	private SettingRow.Drag speedDrag;

	public ThemePanel(int x, int y) {
		super("Themes", x, y);
	}

	@Override
	protected void renderBody(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
		FontManager font = FartwareClient.fonts();
		List<ThemeManager.ColorTheme> themes = ThemeManager.THEMES;
		int rowY = y;
		for (int i = 0; i < themes.size(); i++) {
			ThemeManager.ColorTheme theme = themes.get(i);
			boolean selected = i == ThemeManager.currentIndex();
			boolean hovered = inside(mouseX, mouseY, rowY, Layout.ROW_HEIGHT);
			graphics.fill(x, rowY, x + WIDTH, rowY + Layout.ROW_HEIGHT, hovered ? Theme.ROW_HOVER : Theme.ROW_BG);
			int textY = SettingRow.textY(rowY, Layout.ROW_HEIGHT);
			if (selected) {
				font.drawAccent(graphics, theme.name(), x + Layout.PAD, textY, false);
			} else {
				font.drawString(graphics, theme.name(), x + Layout.PAD, textY, Theme.TEXT, false);
			}

			int swatchY = rowY + (Layout.ROW_HEIGHT - Layout.SWATCH_HEIGHT) / 2;
			Draw.gradientH(graphics, x + WIDTH / 2, swatchY, x + WIDTH - Layout.PAD, swatchY + Layout.SWATCH_HEIGHT, theme.colors());
			rowY += Layout.ROW_HEIGHT;
		}
		SettingRow.render(graphics, ThemeManager.SPEED, "Speed", x, rowY, WIDTH);
	}

	@Override
	protected boolean bodyClicked(double mouseX, double mouseY, int button) {
		int rowHeight = Layout.ROW_HEIGHT;
		int rowY = y + HEADER_HEIGHT;
		for (int i = 0; i < ThemeManager.THEMES.size(); i++) {
			if (inside(mouseX, mouseY, rowY, rowHeight)) {
				ThemeManager.select(i);
				return true;
			}
			rowY += rowHeight;
		}
		if (inside(mouseX, mouseY, rowY, SettingRow.heightOf(ThemeManager.SPEED))) {
			speedDrag = SettingRow.click(ThemeManager.SPEED, mouseX, mouseY, button, x, rowY, WIDTH);
			return true;
		}
		return false;
	}

	@Override
	public void mouseDragged(double mouseX, double mouseY) {
		if (speedDrag != null) {
			speedDrag.drag(mouseX, mouseY);
		}
	}

	@Override
	public void mouseReleased() {
		speedDrag = null;
	}

	private boolean inside(double mouseX, double mouseY, int rowY, int rowHeight) {
		return mouseX >= x && mouseX < x + WIDTH && mouseY >= rowY && mouseY < rowY + rowHeight;
	}
}
