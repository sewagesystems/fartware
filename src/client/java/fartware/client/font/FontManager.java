package fartware.client.font;

import fartware.Fartware;
import fartware.client.gui.ThemeManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Owns the font folder and the active font. Falls back to the vanilla font when no custom font is selected,
 * so everything can draw text through this class unconditionally.
 */
public class FontManager {
	/** Name of the base font that ships inside the jar. */
	public static final String DEFAULT_FONT = "default.ttf";
	/** Pseudo font name that selects Minecraft's own font. */
	public static final String VANILLA = "Minecraft";
	private static final String BUNDLED_DEFAULT = "/assets/fartware/fonts/" + DEFAULT_FONT;

	public static final float DEFAULT_SIZE = 10f;
	public static final float MIN_SIZE = 6f;
	public static final float MAX_SIZE = 32f;

	private final Path directory = FabricLoader.getInstance().getGameDir().resolve("fartware").resolve("fonts");

	private CustomFont current;
	private String currentName;
	private float size = DEFAULT_SIZE;

	/**
	 * Creates the font folder, copies the bundled default font into it if it is missing and selects it.
	 * An existing default.ttf is never overwritten, so you can replace it with your own.
	 */
	public void init() {
		createDirectory();
		extractDefaultFont();
		resetToDefault();
	}

	private void createDirectory() {
		try {
			Files.createDirectories(directory);
		} catch (IOException e) {
			Fartware.LOGGER.error("Could not create font directory {}", directory, e);
		}
	}

	private void extractDefaultFont() {
		Path target = directory.resolve(DEFAULT_FONT);
		if (Files.exists(target)) {
			return;
		}
		try (InputStream in = FontManager.class.getResourceAsStream(BUNDLED_DEFAULT)) {
			if (in == null) {
				Fartware.LOGGER.error("Bundled font {} is missing from the jar", BUNDLED_DEFAULT);
				return;
			}
			Files.copy(in, target);
		} catch (IOException e) {
			Fartware.LOGGER.error("Could not extract the default font to {}", target, e);
		}
	}

	public Path getDirectory() {
		return directory;
	}

	public void openDirectory() {
		createDirectory();
		Util.getPlatform().openPath(directory);
	}

	/** File names (.ttf / .otf) in the font directory. */
	public List<String> list() {
		try (Stream<Path> files = Files.list(directory)) {
			return files
				.filter(Files::isRegularFile)
				.map(path -> path.getFileName().toString())
				.filter(name -> {
					String lower = name.toLowerCase(Locale.ROOT);
					return lower.endsWith(".ttf") || lower.endsWith(".otf");
				})
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.toList();
		} catch (IOException e) {
			return List.of();
		}
	}

	public boolean isCustom() {
		return current != null;
	}

	public String getCurrentName() {
		return currentName;
	}

	public float getSize() {
		return size;
	}

	/**
	 * Selects a font by file name (the extension is optional, case-insensitive).
	 *
	 * @return the resolved file name, or null if the font could not be found or loaded
	 */
	public String select(String name) {
		if (name.equalsIgnoreCase(VANILLA) || name.equalsIgnoreCase("vanilla")) {
			useVanilla();
			return VANILLA;
		}

		String match = null;
		for (String file : list()) {
			String withoutExtension = file.substring(0, file.lastIndexOf('.'));
			if (file.equalsIgnoreCase(name) || withoutExtension.equalsIgnoreCase(name)) {
				match = file;
				break;
			}
		}
		if (match == null) {
			return null;
		}

		try {
			java.awt.Font font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, new File(directory.toFile(), match));
			CustomFont previous = current;
			current = new CustomFont(font, size);
			currentName = match;
			closeLater(previous);
			return match;
		} catch (Exception e) {
			Fartware.LOGGER.error("Could not load font {}", match, e);
			return null;
		}
	}

	public void setSize(float newSize) {
		size = Math.max(MIN_SIZE, Math.min(MAX_SIZE, newSize));
		if (currentName != null) {
			select(currentName);
		}
	}

	/** Switches to Minecraft's own font. */
	public void useVanilla() {
		CustomFont previous = current;
		current = null;
		currentName = null;
		closeLater(previous);
	}

	/** Switches to default.ttf, or to the vanilla font if that file can't be loaded. */
	public void resetToDefault() {
		if (select(DEFAULT_FONT) == null) {
			useVanilla();
		}
	}

	private void closeLater(CustomFont font) {
		if (font != null) {
			Minecraft.getInstance().execute(font::close);
		}
	}

	public int width(String text) {
		return current != null ? current.width(text) : Minecraft.getInstance().font.width(text);
	}

	public int height() {
		return current != null ? current.height() : Minecraft.getInstance().font.lineHeight;
	}

	/** Distance from the top of a line to its baseline (vanilla capitals are 7 pixels tall and sit on row 7). */
	private float ascent() {
		return current != null ? current.ascent() : 7f;
	}

	private float capHeight() {
		return current != null ? current.capHeight() : 7f;
	}

	/**
	 * Height of a capital letter in whole GUI units. Unlike {@link #height()}, which is the full line box
	 * and differs a lot between fonts, this is a good basis for sizing rows so they look the same with any font.
	 */
	public int capitalHeight() {
		return Math.round(capHeight());
	}

	/** Y to draw text at so the capital letters are vertically centred in a row. */
	public int centerY(int rowY, int rowHeight) {
		float capCentre = ascent() - capHeight() / 2f;
		return rowY + (int) Math.floor(rowHeight / 2f - capCentre + 0.49f);
	}

	/** Y to draw text at so the top of the capital letters sits exactly on topEdge. */
	public int topAligned(int topEdge) {
		return topEdge - Math.round(ascent() - capHeight());
	}

	/** Y to draw text at so its baseline sits exactly on baselineEdge. */
	public int baselineAligned(int baselineEdge) {
		return baselineEdge - Math.round(ascent());
	}

	/** Draws accent-coloured text. With a custom font the colour comes from the breathing theme shader. */
	public void drawAccent(GuiGraphics graphics, String text, int x, int y, boolean shadow) {
		if (current != null) {
			current.drawShaded(graphics, text, x, y, shadow);
		} else {
			graphics.drawString(Minecraft.getInstance().font, text, x, y, ThemeManager.accent(), shadow);
		}
	}

	public void drawString(GuiGraphics graphics, String text, int x, int y, int color, boolean shadow) {
		if (current != null) {
			current.draw(graphics, text, x, y, color, shadow);
		} else {
			graphics.drawString(Minecraft.getInstance().font, text, x, y, color, shadow);
		}
	}
}
