package fi.dy.masa.litematica.render.schematic;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import javax.annotation.Nullable;

// Thanks plusls for this hack fix :p
public class OmegaHackfixForCrashJustTemporarilyForNowISwearBecauseOfShittyBrokenCodeBufferBuilder extends BufferBuilder {
    @Nullable
    public BufferBuilder.RenderedBuffer lastRenderBuildBuffer;
    public boolean first = true;
    private double offsetY;

    public OmegaHackfixForCrashJustTemporarilyForNowISwearBecauseOfShittyBrokenCodeBufferBuilder(int initialCapacity) {
        super(initialCapacity);
    }

    public void setYOffset(double offsetY) {
        this.offsetY = offsetY;
    }

    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        return super.vertex(x, y + this.offsetY, z);
    }

    @Override
    public void begin(VertexFormat.Mode drawMode, VertexFormat format) {
        if (this.lastRenderBuildBuffer == null) {
            if (!this.first) {
                this.end();
            } else {
                this.first = false;
            }
        } else {
            this.lastRenderBuildBuffer = null;
        }

        super.begin(drawMode, format);
    }

    @Override
    public BufferBuilder.RenderedBuffer end() {
        this.lastRenderBuildBuffer = super.end();
        return this.lastRenderBuildBuffer;
    }
}
