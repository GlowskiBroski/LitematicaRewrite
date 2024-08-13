package fi.dy.masa.litematica.render.schematic;

import com.mojang.blaze3d.vertex.BufferBuilder;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;

public class BufferBuilderCache {
    private final Map<RenderType, OmegaHackfixForCrashJustTemporarilyForNowISwearBecauseOfShittyBrokenCodeBufferBuilder> blockBufferBuilders = new HashMap<>();
    private final OmegaHackfixForCrashJustTemporarilyForNowISwearBecauseOfShittyBrokenCodeBufferBuilder[] overlayBufferBuilders;

    public BufferBuilderCache() {
        for (RenderType layer : RenderType.chunkBufferLayers()) {
            this.blockBufferBuilders.put(layer, new OmegaHackfixForCrashJustTemporarilyForNowISwearBecauseOfShittyBrokenCodeBufferBuilder(layer.bufferSize()));
        }

        this.overlayBufferBuilders = new OmegaHackfixForCrashJustTemporarilyForNowISwearBecauseOfShittyBrokenCodeBufferBuilder[OverlayRenderType.values().length];

        for (int i = 0; i < this.overlayBufferBuilders.length; ++i) {
            this.overlayBufferBuilders[i] = new OmegaHackfixForCrashJustTemporarilyForNowISwearBecauseOfShittyBrokenCodeBufferBuilder(262144);
        }
    }

    public OmegaHackfixForCrashJustTemporarilyForNowISwearBecauseOfShittyBrokenCodeBufferBuilder getBlockBufferByLayer(RenderType layer) {
        return this.blockBufferBuilders.get(layer);
    }

    public OmegaHackfixForCrashJustTemporarilyForNowISwearBecauseOfShittyBrokenCodeBufferBuilder getOverlayBuffer(OverlayRenderType type) {
        return this.overlayBufferBuilders[type.ordinal()];
    }

    public void clear() {
        this.blockBufferBuilders.values().forEach(BufferBuilder::discard);

        for (BufferBuilder buffer : this.overlayBufferBuilders) {
            buffer.discard();
        }
    }
}
