package fartware.client.gui;

/** Spacing shared by the click GUI and the HUD. Change a value here and everything follows. */
public final class Layout {
	/** Inner padding on the left and right of every panel row. */
	public static final int PAD = 6;
	/** Space between a label and the value or control next to it. */
	public static final int INNER_GAP = 4;
	/** Space between panels, and between the panels and the screen edge. */
	public static final int GAP = 8;
	/** Space between HUD text and the screen edge. */
	public static final int HUD_MARGIN = 4;

	public static final int PANEL_WIDTH = 116;
	/** Header height including its 2px accent line. */
	public static final int HEADER_HEIGHT = 18;
	public static final int ROW_HEIGHT = 16;
	public static final int SUB_ROW_HEIGHT = 14;
	/** Extra row under a number setting's label that holds its slider. */
	public static final int SLIDER_ROW_HEIGHT = 8;
	/** Corner radius of the array list's left edge. */
	public static final int CORNER_RADIUS = 3;
	/** Space above and below the capital letters in an array list row. */
	public static final int LIST_ROW_PADDING = 3;
	/** Space between a module's name and its mode in the array list. */
	public static final int LIST_MODE_GAP = 3;

	/** Slider track and toggle box sizes. Even values stay centred in the (even) row heights. */
	public static final int TRACK_HEIGHT = 4;
	public static final int TOGGLE_SIZE = 6;
	public static final int SWATCH_HEIGHT = 8;

	/** The colour swatch shown on a colour setting's row. */
	public static final int COLOR_SWATCH_WIDTH = 16;
	/** Colour picker: the saturation/brightness square, the hue bar, and the space between the parts. */
	public static final int PICKER_SV_HEIGHT = 48;
	public static final int PICKER_HUE_HEIGHT = 8;
	public static final int PICKER_GAP = 4;

	private Layout() {
	}
}
