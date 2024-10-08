package fi.dy.masa.litematica.mixin;

import fi.dy.masa.litematica.render.LitematicaRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.glow.litematicarecode.LitematicaRewrite.MC;

@Mixin(net.minecraft.client.renderer.LevelRenderer.class)
public abstract class MixinLevelRenderer {

    @Shadow private net.minecraft.client.multiplayer.ClientLevel level;

    @Inject(method = "allChanged", at = @At("RETURN"))
    private void onLoadRenderers(CallbackInfo ci) {
        // Also (re-)load our renderer when the vanilla renderer gets reloaded
        if (this.level != null && this.level == MC.level) {
            LitematicaRenderer.getInstance().loadRenderers();
        }
    }

    @Inject(method = "setupRender", at = @At("TAIL"))
    private void onPostSetupTerrain(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator, CallbackInfo ci) {
        LitematicaRenderer.getInstance().piecewisePrepareAndUpdate(frustum);
    }

    @Inject(method = "renderSectionLayer", at = @At("TAIL"))
    private void onRenderLayer(RenderType renderLayer, double x, double y, double z, Matrix4f matrix4f, Matrix4f positionMatrix, CallbackInfo ci) {
        if (renderLayer == RenderType.solid()) {
            LitematicaRenderer.getInstance().piecewiseRenderSolid(matrix4f, positionMatrix);
        } else if (renderLayer == RenderType.cutoutMipped()) {
            LitematicaRenderer.getInstance().piecewiseRenderCutoutMipped(matrix4f, positionMatrix);
        } else if (renderLayer == RenderType.cutout()) {
            LitematicaRenderer.getInstance().piecewiseRenderCutout(matrix4f, positionMatrix);
        } else if (renderLayer == RenderType.translucent()) {
            LitematicaRenderer.getInstance().piecewiseRenderTranslucent(matrix4f, positionMatrix);
            LitematicaRenderer.getInstance().piecewiseRenderOverlay(matrix4f, positionMatrix);
        }
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", args = "ldc=blockentities", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V"))
    private void onPostRenderEntities(DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        LitematicaRenderer.getInstance().piecewiseRenderEntities(matrix4f, deltaTracker.getGameTimeDeltaPartialTick(false));
    }

}
