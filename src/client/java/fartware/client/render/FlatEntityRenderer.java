package fartware.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import fartware.Fartware;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws entity models in a single flat colour, for the chams and glow ESP modes.
 * <p>
 * Entity renderers pick their own render types (skin, armour, capes...). This wraps each of those in a
 * {@link FlatType} that runs the original type's setup, so the right texture is bound, and then swaps in the
 * flat colour shader and a depth test of our choosing. Types that aren't plain entity quads (enchantment
 * glint, shadows) are dropped.
 */
public final class FlatEntityRenderer {
	private static final Map<RenderType, RenderType> ALWAYS_TYPES = new HashMap<>();
	private static final Map<RenderType, RenderType> DEPTH_TYPES = new HashMap<>();

	private static ShaderInstance shader;
	private static float red = 1f;
	private static float green = 1f;
	private static float blue = 1f;

	private static MultiBufferSource.BufferSource buffers;

	private FlatEntityRenderer() {
	}

	public static void register() {
		CoreShaderRegistrationCallback.EVENT.register(context ->
			context.register(Fartware.id("flat_entity"), DefaultVertexFormat.NEW_ENTITY, loaded -> shader = loaded));
	}

	public static boolean isReady() {
		return shader != null;
	}

	/** Sets the colour used by everything drawn from now until it is changed again. Flush the buffers between colours. */
	public static void setColor(int argb) {
		red = ((argb >> 16) & 0xFF) / 255f;
		green = ((argb >> 8) & 0xFF) / 255f;
		blue = (argb & 0xFF) / 255f;
	}

	/** The buffer source the flat types are drawn through. Call endBatch() on it to draw what has been queued. */
	public static MultiBufferSource.BufferSource buffers() {
		if (buffers == null) {
			buffers = MultiBufferSource.immediate(new ByteBufferBuilder(1 << 20));
		}
		return buffers;
	}

	/**
	 * A buffer source to hand to the entity renderer.
	 *
	 * @param ignoreDepth true to draw everywhere, even through walls; false to draw only where the model is not hidden
	 */
	public static MultiBufferSource source(boolean ignoreDepth) {
		Map<RenderType, RenderType> cache = ignoreDepth ? ALWAYS_TYPES : DEPTH_TYPES;
		return type -> {
			if (!type.format().equals(DefaultVertexFormat.NEW_ENTITY) || type.mode() != VertexFormat.Mode.QUADS || type.isOutline()) {
				return NoopConsumer.INSTANCE;
			}
			return buffers().getBuffer(cache.computeIfAbsent(type, original -> new FlatType(original, ignoreDepth)));
		};
	}

	private static final class FlatType extends RenderType {
		FlatType(RenderType original, boolean ignoreDepth) {
			super("fartware_flat", original.format(), original.mode(), original.bufferSize(), false, false,
				() -> setup(original, ignoreDepth), () -> clear(original));
		}
	}

	private static void setup(RenderType original, boolean ignoreDepth) {
		original.setupRenderState();
		RenderSystem.setShader(() -> shader);
		RenderSystem.setShaderColor(red, green, blue, 1f);
		RenderSystem.depthFunc(ignoreDepth ? GL11.GL_ALWAYS : GL11.GL_LEQUAL);
		// Never write depth, so drawing the colours doesn't change what the rest of the world renders against.
		RenderSystem.depthMask(false);
	}

	private static void clear(RenderType original) {
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		original.clearRenderState();
		RenderSystem.depthFunc(GL11.GL_LEQUAL);
		RenderSystem.depthMask(true);
	}

	/** Swallows everything written to it. */
	private static final class NoopConsumer implements VertexConsumer {
		static final NoopConsumer INSTANCE = new NoopConsumer();

		@Override
		public VertexConsumer addVertex(float x, float y, float z) {
			return this;
		}

		@Override
		public VertexConsumer setColor(int red, int green, int blue, int alpha) {
			return this;
		}

		@Override
		public VertexConsumer setUv(float u, float v) {
			return this;
		}

		@Override
		public VertexConsumer setUv1(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setUv2(int u, int v) {
			return this;
		}

		@Override
		public VertexConsumer setNormal(float x, float y, float z) {
			return this;
		}
	}
}
