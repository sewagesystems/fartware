package fartware.client.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ChatUtil {
	private ChatUtil() {
	}

	private static MutableComponent prefix() {
		return Component.literal("[fartware] ").withStyle(ChatFormatting.GREEN);
	}

	public static void raw(Component message) {
		Minecraft.getInstance().gui.getChat().addMessage(message);
	}

	public static void info(String message) {
		raw(prefix().append(Component.literal(message).withStyle(ChatFormatting.WHITE)));
	}

	public static void error(String message) {
		raw(prefix().append(Component.literal(message).withStyle(ChatFormatting.RED)));
	}

	public static void plain(String message, ChatFormatting color) {
		raw(Component.literal(message).withStyle(color));
	}
}
