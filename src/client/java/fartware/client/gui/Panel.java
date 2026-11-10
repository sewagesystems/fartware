package fartware.client.gui;

import fartware.client.FartwareClient;
import fartware.client.font.FontManager;
import net.minecraft.client.gui.GuiGraphics;

/** A draggable, collapsible window in the click GUI. Subclasses draw the body. */
public abstract class Panel {
	public static final int WIDTH = Layout.PANEL_WIDTH;
	public static final int HEADER_HEIGHT = Layout.HEADER_HEIGHT;

	private final String title;
	protected int x;
	protected int y;
	private boolean open = true;

	protected Panel(String title, int x, int y) {
		this.title = title;
		this.x = x;
		this.y = y;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void moveTo(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public void toggleOpen() {
		open = !open;
	}

	public boolean headerContains(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEADER_HEIGHT;
	}

	public void render(GuiGraphics graphics, int mouseX, int mouseY) {
		FontManager font = FartwareClient.fonts();
		int textY = SettingRow.textY(y, HEADER_HEIGHT - 2);

		graphics.fill(x, y, x + WIDTH, y + HEADER_HEIGHT, Theme.HEADER_BG);
		font.drawString(graphics, open ? "-" : "+", x + Layout.PAD, textY, Theme.TEXT_DIM, false);
		font.drawString(graphics, title, x + (WIDTH - font.width(title)) / 2, textY, Theme.TEXT, false);

		BreatheShader.fill(graphics, x, y + HEADER_HEIGHT - 2, x + WIDTH, y + HEADER_HEIGHT);

		if (open) {
			renderBody(graphics, x, y + HEADER_HEIGHT, mouseX, mouseY);
		}
	}

	protected abstract void renderBody(GuiGraphics graphics, int x, int y, int mouseX, int mouseY);

	protected abstract boolean bodyClicked(double mouseX, double mouseY, int button);

	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return open && bodyClicked(mouseX, mouseY, button);
	}

	public void mouseDragged(double mouseX, double mouseY) {
	}

	public void mouseReleased() {
	}

	/** Returns true if the panel consumed the key (for example while binding a module). */
	public boolean keyPressed(int keyCode) {
		return false;
	}

	/** Called when the GUI closes. */
	public void closed() {
		mouseReleased();
	}
}
