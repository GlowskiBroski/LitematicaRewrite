package fi.dy.masa.litematica.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;
import fi.dy.masa.litematica.world.WorldSchematic;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;

import static com.glow.litematicarecode.LitematicaRewrite.MC;

public class LitematicaRenderer {
    private static final LitematicaRenderer INSTANCE = new LitematicaRenderer();

    private WorldRendererSchematic worldRenderer;
    private Frustum frustum;
    private int frameCount;
    private long finishTimeNano;

    private boolean renderCollidingSchematicBlocks;
    private boolean renderPiecewiseSchematic;
    private boolean renderPiecewiseBlocks;

    private LitematicaRenderer() {
    }

    public static LitematicaRenderer getInstance() {
        return INSTANCE;
    }

    public WorldRendererSchematic getWorldRenderer() {
        if (this.worldRenderer == null) {
            this.worldRenderer = new WorldRendererSchematic(MC);
        }

        return this.worldRenderer;
    }

    public void loadRenderers() {
        this.getWorldRenderer().loadRenderers();
    }

    public void onSchematicWorldChanged(@Nullable WorldSchematic worldClient) {
        this.getWorldRenderer().setWorldAndLoadRenderers(worldClient);
    }

    private void calculateFinishTime() {
        // TODO 1.15+
        long fpsTarget = 60L;

        if (Configs.Generic.RENDER_THREAD_NO_TIMEOUT.getBooleanValue()) {
            this.finishTimeNano = Long.MAX_VALUE;
        } else {
            this.finishTimeNano = System.nanoTime() + Math.max(1000000000L / fpsTarget / 2L, 0L);
        }
    }

    /*
    public void renderSchematicWorld(MatrixStack matrices, Matrix4f matrix, float partialTicks)
    {
        if (MC.skipGameRender == false)
        {
            MC.getProfiler().push("litematica_schematic_world_render");

            if (MC.getCameraEntity() == null)
            {
                MC.setCameraEntity(MC.player);
            }

            RenderSystem.pushMatrix();
            RenderSystem.enableDepthTest();

            this.calculateFinishTime();
            this.renderWorld(matrices, matrix, partialTicks, this.finishTimeNano);
            this.cleanup();

            RenderSystem.popMatrix();

            MC.getProfiler().pop();
        }
    }

    private void renderWorld(MatrixStack matrices, Matrix4f matrix, float partialTicks, long finishTimeNano)
    {
        MC.getProfiler().push("culling");

        RenderSystem.shadeModel(GL11.GL_SMOOTH);

        Camera camera = this.getCamera();
        Vec3d cameraPos = camera.getPos();
        double x = cameraPos.x;
        double y = cameraPos.y;
        double z = cameraPos.z;

        Frustum frustum = new Frustum(matrices.peek().getModel(), matrix);
        frustum.setPosition(x, y, z);

        MC.getProfiler().swap("prepare_terrain");
        MC.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
        fi.dy.masa.malilib.render.RenderUtils.disableDiffuseLighting();
        WorldRendererSchematic worldRenderer = this.getWorldRenderer();

        MC.getProfiler().swap("terrain_setup");
        worldRenderer.setupTerrain(camera, frustum, this.frameCount++, MC.player.isSpectator());

        MC.getProfiler().swap("update_chunks");
        worldRenderer.updateChunks(finishTimeNano);

        MC.getProfiler().swap("terrain");
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.disableAlphaTest();

        if (Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.getBooleanValue())
        {
            RenderSystem.pushMatrix();

            if (Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue())
            {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-0.2f, -0.4f);
            }

            this.setupAlphaShader();
            this.enableAlphaShader();

            fi.dy.masa.malilib.render.RenderUtils.setupBlend();

            worldRenderer.renderBlockLayer(RenderLayer.getSolid(), matrices, camera);
            worldRenderer.renderBlockLayer(RenderLayer.getCutoutMipped(), matrices, camera);
            worldRenderer.renderBlockLayer(RenderLayer.getCutout(), matrices, camera);

            if (Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue())
            {
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            RenderSystem.disableBlend();
            RenderSystem.shadeModel(GL11.GL_FLAT);
            RenderSystem.alphaFunc(GL11.GL_GREATER, 0.01F);

            RenderSystem.matrixMode(GL11.GL_MODELVIEW);
            RenderSystem.popMatrix();

            MC.getProfiler().swap("entities");

            RenderSystem.pushMatrix();

            fi.dy.masa.malilib.render.RenderUtils.enableDiffuseLightingForLevel(matrices);
            fi.dy.masa.malilib.render.RenderUtils.setupBlend();

            worldRenderer.renderEntities(camera, frustum, matrices, partialTicks);

            RenderSystem.disableFog(); // Fixes Structure Blocks breaking all rendering
            RenderSystem.disableBlend();
            fi.dy.masa.malilib.render.RenderUtils.disableDiffuseLighting();

            RenderSystem.matrixMode(GL11.GL_MODELVIEW);
            RenderSystem.popMatrix();

            RenderSystem.enableCull();
            RenderSystem.alphaFunc(GL11.GL_GREATER, 0.1F);
            MC.getTextureManager().bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
            RenderSystem.shadeModel(GL11.GL_SMOOTH);

            MC.getProfiler().swap("translucent");
            RenderSystem.depthMask(false);

            RenderSystem.pushMatrix();

            fi.dy.masa.malilib.render.RenderUtils.setupBlend();

            worldRenderer.renderBlockLayer(RenderLayer.getTranslucent(), matrices, camera);

            RenderSystem.popMatrix();

            this.disableAlphaShader();
        }

        MC.getProfiler().swap("overlay");
        this.renderSchematicOverlay(matrices);

        RenderSystem.enableAlphaTest();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.enableCull();

        MC.getProfiler().pop();
    }
    */

    public void renderSchematicOverlay(Matrix4f matrix4f, Matrix4f projMatrix) {
        boolean invert = Hotkeys.INVERT_OVERLAY_RENDER_STATE.getKeybind().isKeybindHeld();

        if (Configs.Visuals.ENABLE_SCHEMATIC_OVERLAY.getBooleanValue() != invert) {
            boolean renderThrough = Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH.getBooleanValue() || Hotkeys.RENDER_OVERLAY_THROUGH_BLOCKS.getKeybind().isKeybindHeld();
            float lineWidth = (float) (renderThrough ? Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH_THROUGH.getDoubleValue() : Configs.Visuals.SCHEMATIC_OVERLAY_OUTLINE_WIDTH.getDoubleValue());

            RenderSystem.disableCull();
            //TODO: RenderSystem.alphaFunc(GL11.GL_GREATER, 0.001F);
            RenderSystem.enablePolygonOffset();
            RenderSystem.polygonOffset(-0.4f, -0.8f);
            RenderSystem.lineWidth(lineWidth);
            fi.dy.masa.malilib.render.RenderUtils.setupBlend();
            fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);
            //TODO: RenderSystem.glMultiTexCoord2f(GL13.GL_TEXTURE1, 240.0F, 240.0F);

            this.getWorldRenderer().renderBlockOverlays(matrix4f, this.getCamera(), projMatrix);

            RenderSystem.enableDepthTest();
            RenderSystem.polygonOffset(0f, 0f);
            RenderSystem.disablePolygonOffset();
            RenderSystem.enableCull();
        }
    }

    public void piecewisePrepareAndUpdate(Frustum frustum) {
        boolean render = Configs.Generic.BETTER_RENDER_ORDER.getBooleanValue() &&
                Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
                MC.getCameraEntity() != null;
        this.renderPiecewiseSchematic = false;
        this.renderPiecewiseBlocks = false;
        WorldRendererSchematic worldRenderer = this.getWorldRenderer();

        if (render && frustum != null && worldRenderer.hasWorld()) {
            boolean invert = Hotkeys.INVERT_GHOST_BLOCK_RENDER_STATE.getKeybind().isKeybindHeld();
            this.renderPiecewiseSchematic = Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() != invert;
            this.renderPiecewiseBlocks = this.renderPiecewiseSchematic && Configs.Visuals.ENABLE_SCHEMATIC_BLOCKS.getBooleanValue();
            this.renderCollidingSchematicBlocks = Configs.Visuals.RENDER_COLLIDING_SCHEMATIC_BLOCKS.getBooleanValue();

            if (this.renderPiecewiseSchematic) {
                MC.getProfiler().push("litematica_culling");

                this.calculateFinishTime();

                MC.getProfiler().popPush("litematica_terrain_setup");
                worldRenderer.setupTerrain(this.getCamera(), frustum, this.frameCount++, MC.player.isSpectator());

                MC.getProfiler().popPush("litematica_update_chunks");
                worldRenderer.updateChunks(this.finishTimeNano);

                MC.getProfiler().pop();

                this.frustum = frustum;
            }
        }
    }

    public void piecewiseRenderSolid(Matrix4f matrix4f, Matrix4f projMatrix) {
        if (this.renderPiecewiseBlocks) {
            MC.getProfiler().push("litematica_blocks_solid");

            if (this.renderCollidingSchematicBlocks) {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-0.3f, -0.6f);
            }

            this.getWorldRenderer().renderBlockLayer(RenderType.solid(), matrix4f, this.getCamera(), projMatrix);

            if (this.renderCollidingSchematicBlocks) {
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            MC.getProfiler().pop();
        }
    }

    public void piecewiseRenderCutoutMipped(Matrix4f matrix4f, Matrix4f projMatrix) {
        if (this.renderPiecewiseBlocks) {
            MC.getProfiler().push("litematica_blocks_cutout_mipped");

            if (this.renderCollidingSchematicBlocks) {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-0.3f, -0.6f);
            }

            this.getWorldRenderer().renderBlockLayer(RenderType.cutoutMipped(), matrix4f, this.getCamera(), projMatrix);

            if (this.renderCollidingSchematicBlocks) {
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            MC.getProfiler().pop();
        }
    }

    public void piecewiseRenderCutout(Matrix4f matrix4f, Matrix4f projMatrix) {
        if (this.renderPiecewiseBlocks) {
            MC.getProfiler().push("litematica_blocks_cutout");

            if (this.renderCollidingSchematicBlocks) {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-0.3f, -0.6f);
            }

            this.getWorldRenderer().renderBlockLayer(RenderType.cutout(), matrix4f, this.getCamera(), projMatrix);

            if (this.renderCollidingSchematicBlocks) {
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            MC.getProfiler().pop();
        }
    }

    public void piecewiseRenderTranslucent(Matrix4f matrix4f, Matrix4f projMatrix) {
        if (this.renderPiecewiseBlocks) {
            MC.getProfiler().push("litematica_translucent");

            if (this.renderCollidingSchematicBlocks) {
                RenderSystem.enablePolygonOffset();
                RenderSystem.polygonOffset(-0.3f, -0.6f);
            }

            this.getWorldRenderer().renderBlockLayer(RenderType.translucent(), matrix4f, this.getCamera(), projMatrix);

            if (this.renderCollidingSchematicBlocks) {
                RenderSystem.polygonOffset(0f, 0f);
                RenderSystem.disablePolygonOffset();
            }

            MC.getProfiler().pop();
        }
    }

    public void piecewiseRenderOverlay(Matrix4f matrix4f, Matrix4f projMatrix) {
        if (this.renderPiecewiseSchematic) {
            MC.getProfiler().push("litematica_overlay");

            RenderTarget frameBuffer = MC.levelRenderer.getTranslucentTarget();

            if (!Minecraft.useShaderTransparency()) frameBuffer = null;

            if (frameBuffer != null) frameBuffer.bindWrite(false);

            this.renderSchematicOverlay(matrix4f, projMatrix);

            if (frameBuffer != null) MC.getMainRenderTarget().bindWrite(false);

            MC.getProfiler().pop();
        }

        this.cleanup();
    }

    public void piecewiseRenderEntities(Matrix4f matrix4f, float partialTicks) {
        if (this.renderPiecewiseBlocks) {
            MC.getProfiler().push("litematica_entities");

            this.getWorldRenderer().renderEntities(this.getCamera(), this.frustum, matrix4f, partialTicks);

            MC.getProfiler().pop();
        }
    }

    private Camera getCamera() {
        return MC.gameRenderer.getMainCamera();
    }

    private void cleanup() {
        this.renderPiecewiseSchematic = false;
        this.renderPiecewiseBlocks = false;
    }
}
