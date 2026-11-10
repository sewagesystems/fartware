package fartware.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import fartware.Fartware;
import fartware.client.FartwareClient;
import fartware.client.gui.BlurShader;
import fartware.client.gui.GlowShader;
import fartware.client.module.impl.EspModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;

/**
 * The world side of the ESP: the Glow and Chams modes re-draw the players after the entities have rendered,
 * and the 2D mode uses the camera matrices captured at the end of the world render to project players onto the screen.
 */
public final class EspRenderer {
	private static final Matrix4f VIEW = new Matrix4f();
	private static final Matrix4f PROJECTION = new Matrix4f();
	private static Vec3 cameraPos = Vec3.ZERO;

	private static RenderTarget mask;

	private EspRenderer() {
	}

	public static void register() {
		WorldRenderEvents.AFTER_ENTITIES.register(EspRenderer::afterEntities);
		WorldRenderEvents.LAST.register(context -> {
			VIEW.set(context.positionMatrix());
			PROJECTION.set(context.projectionMatrix());
			cameraPos = context.camera().getPosition();
		});
	}

	private static void afterEntities(WorldRenderContext context) {
		EspModule esp = FartwareClient.modules().esp();
		if (!esp.isEnabled() || !(esp.isGlow() || esp.isChams()) || !FlatEntityRenderer.isReady()) {
			return;
		}
		List<AbstractClientPlayer> players = esp.targets();
		if (players.isEmpty()) {
			return;
		}

		try {
			if (esp.isGlow()) {
				renderGlow(context, players, esp);
			} else {
				renderChams(context, players, esp);
			}
		} catch (RuntimeException e) {
			Fartware.LOGGER.error("ESP failed to render, disabling it", e);
			esp.setEnabled(false);
		}
	}

	private static void renderChams(WorldRenderContext context, List<AbstractClientPlayer> players, EspModule esp) {
		if (esp.chamsStatic()) {
			FlatEntityRenderer.setColor(esp.staticColor());
			drawPlayers(context, players, FlatEntityRenderer.source(true));
			return;
		}
		// Everything in the hidden colour first, then the parts that pass the depth test over it in the visible colour.
		FlatEntityRenderer.setColor(esp.hiddenColor());
		drawPlayers(context, players, FlatEntityRenderer.source(true));
		FlatEntityRenderer.setColor(esp.visibleColor());
		drawPlayers(context, players, FlatEntityRenderer.source(false));
	}

	private static void renderGlow(WorldRenderContext context, List<AbstractClientPlayer> players, EspModule esp) {
		if (!GlowShader.isReady()) {
			return;
		}
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		if (mask == null) {
			mask = new TextureTarget(main.width, main.height, true, false);
		} else if (mask.width != main.width || mask.height != main.height) {
			mask.resize(main.width, main.height, false);
		}

		// Draw the players in white on an empty target to get their silhouette.
		mask.setClearColor(0f, 0f, 0f, 0f);
		mask.clear(false);
		boolean throughWalls = esp.glowThroughWalls();
		if (!throughWalls) {
			mask.copyDepthFrom(main);
		}
		mask.bindWrite(true);
		FlatEntityRenderer.setColor(0xFFFFFFFF);
		drawPlayers(context, players, FlatEntityRenderer.source(throughWalls));

		int blurred = BlurShader.blurTarget(mask, esp.glowSize());
		if (blurred != -1) {
			GlowShader.composite(main, blurred, mask.getColorTextureId(), esp.glowStrength());
		}
		main.bindWrite(true);
	}

	/** Draws the players through the given buffer source with the camera relative positions the world renderer uses. */
	private static void drawPlayers(WorldRenderContext context, List<AbstractClientPlayer> players, MultiBufferSource source) {
		Minecraft minecraft = Minecraft.getInstance();
		EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
		Vec3 camera = context.camera().getPosition();
		float partialTick = context.tickCounter().getGameTimeDeltaPartialTick(true);

		// Name tags are skipped while the GUI is hidden, and the ground shadow would show up in the colour.
		boolean hideGui = minecraft.options.hideGui;
		minecraft.options.hideGui = true;
		dispatcher.setRenderShadow(false);
		try {
			for (AbstractClientPlayer player : players) {
				if (!dispatcher.shouldRender(player, context.frustum(), camera.x, camera.y, camera.z)) {
					continue;
				}
				double x = Mth.lerp(partialTick, player.xOld, player.getX());
				double y = Mth.lerp(partialTick, player.yOld, player.getY());
				double z = Mth.lerp(partialTick, player.zOld, player.getZ());
				float yaw = Mth.lerp(partialTick, player.yRotO, player.getYRot());
				dispatcher.render(player, x - camera.x, y - camera.y, z - camera.z, yaw, partialTick,
					context.matrixStack(), source, dispatcher.getPackedLightCoords(player, partialTick));
			}
			FlatEntityRenderer.buffers().endBatch();
		} finally {
			minecraft.options.hideGui = hideGui;
			dispatcher.setRenderShadow(true);
		}
	}

	/**
	 * Projects the entity's bounding box onto the screen.
	 *
	 * @return {left, top, right, bottom} in GUI coordinates, or null if any part of the box is behind the camera
	 */
	public static float[] project(Entity entity, float partialTick, int guiWidth, int guiHeight) {
		double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
		double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
		double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
		AABB box = entity.getBoundingBox().move(x - entity.getX(), y - entity.getY(), z - entity.getZ());

		float minX = Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		Vector4f point = new Vector4f();
		for (int corner = 0; corner < 8; corner++) {
			double cx = (corner & 1) == 0 ? box.minX : box.maxX;
			double cy = (corner & 2) == 0 ? box.minY : box.maxY;
			double cz = (corner & 4) == 0 ? box.minZ : box.maxZ;
			point.set((float) (cx - cameraPos.x), (float) (cy - cameraPos.y), (float) (cz - cameraPos.z), 1f);
			VIEW.transform(point);
			PROJECTION.transform(point);
			if (point.w <= 0.05f) {
				return null;
			}
			float screenX = (point.x / point.w * 0.5f + 0.5f) * guiWidth;
			float screenY = (0.5f - point.y / point.w * 0.5f) * guiHeight;
			minX = Math.min(minX, screenX);
			maxX = Math.max(maxX, screenX);
			minY = Math.min(minY, screenY);
			maxY = Math.max(maxY, screenY);
		}
		return new float[] {minX, minY, maxX, maxY};
	}
}
