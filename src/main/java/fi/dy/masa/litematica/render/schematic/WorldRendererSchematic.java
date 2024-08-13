package fi.dy.masa.litematica.render.schematic;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo.OverlayRenderType;
import fi.dy.masa.litematica.world.ChunkSchematic;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.LayerRange;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import javax.annotation.Nullable;
import java.util.*;

import static com.glow.litematicarecode.LitematicaRewrite.MC;

public class WorldRendererSchematic {
    private final Minecraft mc;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final BlockRenderDispatcher blockRenderManager;
    private final BlockModelRendererSchematic blockModelRenderer;
    private final Set<BlockEntity> blockEntities = new HashSet<>();
    private final List<ChunkRendererSchematicVbo> renderInfos = new ArrayList<>(1024);
    private final RenderBuffers bufferBuilders;
    private Set<ChunkRendererSchematicVbo> chunksToUpdate = new LinkedHashSet<>();
    private WorldSchematic world;
    private ChunkRenderDispatcherSchematic chunkRendererDispatcher;
    private double lastCameraChunkUpdateX = Double.MIN_VALUE;
    private double lastCameraChunkUpdateY = Double.MIN_VALUE;
    private double lastCameraChunkUpdateZ = Double.MIN_VALUE;
    private double lastCameraX = Double.MIN_VALUE;
    private double lastCameraY = Double.MIN_VALUE;
    private double lastCameraZ = Double.MIN_VALUE;
    private float lastCameraPitch = Float.MIN_VALUE;
    private float lastCameraYaw = Float.MIN_VALUE;
    private ChunkRenderDispatcherLitematica renderDispatcher;
    private final IChunkRendererFactory renderChunkFactory;
    //private ShaderGroup entityOutlineShader;
    //private boolean entityOutlinesRendered;

    private int renderDistanceChunks = -1;
    private int renderEntitiesStartupCounter = 2;
    private int countEntitiesTotal;
    private int countEntitiesRendered;
    private int countEntitiesHidden;

    private double lastTranslucentSortX;
    private double lastTranslucentSortY;
    private double lastTranslucentSortZ;
    private boolean displayListEntitiesDirty = true;

    public WorldRendererSchematic(Minecraft mc) {
        this.mc = mc;
        this.entityRenderDispatcher = mc.getEntityRenderDispatcher();
        this.bufferBuilders = mc.renderBuffers();

        this.renderChunkFactory = ChunkRendererSchematicVbo::new;

        this.blockRenderManager = MC.getBlockRenderer();
        this.blockModelRenderer = new BlockModelRendererSchematic(mc.getBlockColors());
    }

    public void markNeedsUpdate() {
        this.displayListEntitiesDirty = true;
    }

    public boolean hasWorld() {
        return this.world != null;
    }

    public String getDebugInfoRenders() {
        int rcTotal = this.chunkRendererDispatcher != null ? this.chunkRendererDispatcher.getRendererCount() : 0;
        int rcRendered = this.chunkRendererDispatcher != null ? this.getRenderedChunks() : 0;
        return String.format("C: %d/%d %sD: %d, L: %d, %s", rcRendered, rcTotal, this.mc.smartCull ? "(s) " : "", this.renderDistanceChunks, 0, this.renderDispatcher == null ? "null" : this.renderDispatcher.getDebugInfo());
    }

    public String getDebugInfoEntities() {
        return "E: " + this.countEntitiesRendered + "/" + this.countEntitiesTotal + ", B: " + this.countEntitiesHidden;
    }

    protected int getRenderedChunks() {
        int count = 0;

        for (ChunkRendererSchematicVbo chunkRenderer : this.renderInfos) {
            ChunkRenderDataSchematic data = chunkRenderer.chunkRenderData;

            if (data != ChunkRenderDataSchematic.EMPTY && !data.isEmpty()) {
                ++count;
            }
        }

        return count;
    }

    public void setWorldAndLoadRenderers(@Nullable WorldSchematic worldSchematic) {
        this.lastCameraChunkUpdateX = Double.MIN_VALUE;
        this.lastCameraChunkUpdateY = Double.MIN_VALUE;
        this.lastCameraChunkUpdateZ = Double.MIN_VALUE;
        //this.renderManager.setWorld(worldClientIn);
        this.world = worldSchematic;

        if (worldSchematic != null) {
            this.loadRenderers();
        } else {
            this.chunksToUpdate.clear();
            this.renderInfos.clear();

            if (this.chunkRendererDispatcher != null) {
                this.chunkRendererDispatcher.delete();
                this.chunkRendererDispatcher = null;
            }

            if (this.renderDispatcher != null) {
                this.renderDispatcher.stopWorkerThreads();
            }

            this.renderDispatcher = null;
            this.blockEntities.clear();
        }
    }

    public void loadRenderers() {
        if (this.hasWorld()) {
            if (this.renderDispatcher == null) {
                this.renderDispatcher = new ChunkRenderDispatcherLitematica();
            }

            this.displayListEntitiesDirty = true;
            this.renderDistanceChunks = this.mc.options.renderDistance().get();

            if (this.chunkRendererDispatcher != null) {
                this.chunkRendererDispatcher.delete();
            }

            this.stopChunkUpdates();

            synchronized (this.blockEntities) {
                this.blockEntities.clear();
            }

            this.chunkRendererDispatcher = new ChunkRenderDispatcherSchematic(this.world, this.renderDistanceChunks, this, this.renderChunkFactory);
            this.renderEntitiesStartupCounter = 2;
        }
    }

    protected void stopChunkUpdates() {
        this.chunksToUpdate.clear();
        this.renderDispatcher.stopChunkUpdates();
    }

    public void setupTerrain(Camera camera, Frustum frustum, int frameCount, boolean playerSpectator) {
        this.world.getProfiler().push("setup_terrain");

        if (this.chunkRendererDispatcher == null ||
                this.mc.options.renderDistance().get() != this.renderDistanceChunks) {
            this.loadRenderers();
        }

        Entity entity = EntityUtils.getCameraEntity();

        if (entity == null) {
            entity = this.mc.player;
        }

        //camera.update(this.world, entity, this.mc.options.perspective > 0, this.mc.options.perspective == 2, this.mc.getTickDelta());

        this.world.getProfiler().push("camera");

        double entityX = entity.getX();
        double entityY = entity.getY();
        double entityZ = entity.getZ();
        double diffX = entityX - this.lastCameraChunkUpdateX;
        double diffY = entityY - this.lastCameraChunkUpdateY;
        double diffZ = entityZ - this.lastCameraChunkUpdateZ;

        if (diffX * diffX + diffY * diffY + diffZ * diffZ > 256.0) {
            this.lastCameraChunkUpdateX = entityX;
            this.lastCameraChunkUpdateY = entityY;
            this.lastCameraChunkUpdateZ = entityZ;
            this.chunkRendererDispatcher.removeOutOfRangeRenderers();
        }

        this.world.getProfiler().popPush("renderlist_camera");

        Vec3 cameraPos = camera.getPosition();
        double cameraX = cameraPos.x;
        double cameraY = cameraPos.y;
        double cameraZ = cameraPos.z;

        this.renderDispatcher.setCameraPosition(cameraPos);

        this.world.getProfiler().popPush("culling");
        BlockPos viewPos = BlockPos.containing(cameraX, cameraY + (double) entity.getEyeHeight(), cameraZ);
        final int centerChunkX = (viewPos.getX() >> 4);
        final int centerChunkZ = (viewPos.getZ() >> 4);
        final int renderDistance = this.mc.options.renderDistance().get();
        ChunkPos viewChunk = new ChunkPos(viewPos);

        this.displayListEntitiesDirty = this.displayListEntitiesDirty || !this.chunksToUpdate.isEmpty() ||
                entityX != this.lastCameraX ||
                entityY != this.lastCameraY ||
                entityZ != this.lastCameraZ ||
                entity.getXRot() != this.lastCameraPitch ||
                entity.getYRot() != this.lastCameraYaw;
        this.lastCameraX = cameraX;
        this.lastCameraY = cameraY;
        this.lastCameraZ = cameraZ;
        this.lastCameraPitch = camera.getXRot();
        this.lastCameraYaw = camera.getYRot();

        this.world.getProfiler().popPush("update");

        if (this.displayListEntitiesDirty) {
            this.world.getProfiler().push("fetch");

            this.displayListEntitiesDirty = false;
            this.renderInfos.clear();

            this.world.getProfiler().popPush("sort");
            List<ChunkPos> positions = DataManager.getSchematicPlacementManager().getAndUpdateVisibleChunks(viewChunk);
            //positions.sort(new SubChunkPos.DistanceComparator(viewSubChunk));

            //Queue<SubChunkPos> queuePositions = new PriorityQueue<>(new SubChunkPos.DistanceComparator(viewSubChunk));
            //queuePositions.addAll(set);

            //if (GuiBase.isCtrlDown()) System.out.printf("sorted positions: %d\n", positions.size());

            this.world.getProfiler().popPush("iteration");

            //while (queuePositions.isEmpty() == false)
            for (ChunkPos chunkPos : positions) {
                //SubChunkPos subChunk = queuePositions.poll();
                int cx = chunkPos.x;
                int cz = chunkPos.z;
                // Only render sub-chunks that are within the client's render distance, and that
                // have been already properly loaded on the client
                if (Math.abs(cx - centerChunkX) <= renderDistance &&
                        Math.abs(cz - centerChunkZ) <= renderDistance &&
                        this.world.getChunkProvider().hasChunk(cx, cz)) {
                    ChunkRendererSchematicVbo chunkRenderer = this.chunkRendererDispatcher.getChunkRenderer(cx, cz);

                    if (chunkRenderer != null && frustum.isVisible(chunkRenderer.getBoundingBox())) {
                        //if (GuiBase.isCtrlDown()) System.out.printf("add @ %s\n", subChunk);
                        if (chunkRenderer.needsUpdate() && chunkPos.equals(viewChunk)) {
                            chunkRenderer.setNeedsUpdate(true);
                        }

                        this.renderInfos.add(chunkRenderer);
                    }
                }
            }

            this.world.getProfiler().pop(); // fetch
        }

        this.world.getProfiler().popPush("rebuild_near");
        Set<ChunkRendererSchematicVbo> set = this.chunksToUpdate;
        this.chunksToUpdate = new LinkedHashSet<>();

        for (ChunkRendererSchematicVbo chunkRendererTmp : this.renderInfos) {
            if (chunkRendererTmp.needsUpdate() || set.contains(chunkRendererTmp)) {
                this.displayListEntitiesDirty = true;
                BlockPos pos = chunkRendererTmp.getOrigin().offset(8, 8, 8);
                boolean isNear = pos.distSqr(viewPos) < 1024.0D;

                if (!chunkRendererTmp.needsImmediateUpdate() && !isNear) {
                    this.chunksToUpdate.add(chunkRendererTmp);
                } else {
                    //if (GuiBase.isCtrlDown()) System.out.printf("====== update now\n");
                    this.world.getProfiler().push("build_near");

                    this.renderDispatcher.updateChunkNow(chunkRendererTmp);
                    chunkRendererTmp.clearNeedsUpdate();

                    this.world.getProfiler().pop();
                }
            }
        }

        this.chunksToUpdate.addAll(set);

        this.world.getProfiler().pop();
        this.world.getProfiler().pop();
    }

    public void updateChunks(long finishTimeNano) {
        this.mc.getProfiler().push("litematica_run_chunk_uploads");
        this.displayListEntitiesDirty |= this.renderDispatcher.runChunkUploads(finishTimeNano);

        this.mc.getProfiler().popPush("litematica_check_update");

        if (!this.chunksToUpdate.isEmpty()) {
            Iterator<ChunkRendererSchematicVbo> iterator = this.chunksToUpdate.iterator();

            while (iterator.hasNext()) {
                ChunkRendererSchematicVbo renderChunk = iterator.next();
                boolean flag;

                if (renderChunk.needsImmediateUpdate()) {
                    this.mc.getProfiler().push("litematica_update_now");
                    flag = this.renderDispatcher.updateChunkNow(renderChunk);
                } else {
                    this.mc.getProfiler().push("litematica_update_later");
                    flag = this.renderDispatcher.updateChunkLater(renderChunk);
                }

                this.mc.getProfiler().pop();

                if (!flag) {
                    break;
                }

                renderChunk.clearNeedsUpdate();
                iterator.remove();
                long i = finishTimeNano - System.nanoTime();

                if (i < 0L) {
                    break;
                }
            }
        }

        this.mc.getProfiler().pop();
    }

    public int renderBlockLayer(RenderType renderLayer, Matrix4f matrices, Camera camera, Matrix4f projMatrix) {
        this.world.getProfiler().push("render_block_layer_" + renderLayer.toString());

        boolean isTranslucent = renderLayer == RenderType.translucent();

        renderLayer.setupRenderState();
        //RenderUtils.disableDiffuseLighting();
        Vec3 cameraPos = camera.getPosition();
        double x = cameraPos.x;
        double y = cameraPos.y;
        double z = cameraPos.z;

        if (isTranslucent) {
            this.world.getProfiler().push("translucent_sort");
            double diffX = x - this.lastTranslucentSortX;
            double diffY = y - this.lastTranslucentSortY;
            double diffZ = z - this.lastTranslucentSortZ;

            if (diffX * diffX + diffY * diffY + diffZ * diffZ > 1.0D) {
                this.lastTranslucentSortX = x;
                this.lastTranslucentSortY = y;
                this.lastTranslucentSortZ = z;
                int i = 0;

                for (ChunkRendererSchematicVbo chunkRenderer : this.renderInfos) {
                    if ((chunkRenderer.getChunkRenderData().isBlockLayerStarted(renderLayer) ||
                            (chunkRenderer.getChunkRenderData() != ChunkRenderDataSchematic.EMPTY && chunkRenderer.hasOverlay())) && i++ < 15) {
                        this.renderDispatcher.updateTransparencyLater(chunkRenderer);
                    }
                }
            }

            this.world.getProfiler().pop();
        }

        this.world.getProfiler().push("filter_empty");
        this.world.getProfiler().popPush("render");

        boolean reverse = isTranslucent;
        int startIndex = reverse ? this.renderInfos.size() - 1 : 0;
        int stopIndex = reverse ? -1 : this.renderInfos.size();
        int increment = reverse ? -1 : 1;
        int count = 0;

        ShaderInstance shader = RenderSystem.getShader();
        BufferUploader.reset();

        boolean renderAsTranslucent = Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT.getBooleanValue();

        if (renderAsTranslucent) {
            float alpha = (float) Configs.Visuals.GHOST_BLOCK_ALPHA.getDoubleValue();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        }

        initShader(shader, matrices, projMatrix);
        RenderSystem.setupShaderLights(shader);
        shader.apply();

        Uniform chunkOffsetUniform = shader.CHUNK_OFFSET;
        boolean startedDrawing = false;

        for (int i = startIndex; i != stopIndex; i += increment) {
            ChunkRendererSchematicVbo renderer = this.renderInfos.get(i);

            if (!renderer.getChunkRenderData().isBlockLayerEmpty(renderLayer)) {
                BlockPos chunkOrigin = renderer.getOrigin();
                VertexBuffer buffer = renderer.getBlocksVertexBufferByLayer(renderLayer);

                if (chunkOffsetUniform != null) {
                    chunkOffsetUniform.set((float) (chunkOrigin.getX() - x), (float) (chunkOrigin.getY() - y), (float) (chunkOrigin.getZ() - z));
                    chunkOffsetUniform.upload();
                }

                buffer.bind();
                buffer.draw();
                VertexBuffer.unbind();
                startedDrawing = true;
                ++count;
            }
        }

        if (renderAsTranslucent) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        if (chunkOffsetUniform != null) {
            chunkOffsetUniform.set(0.0F, 0.0F, 0.0F);
        }

        shader.clear();

        if (startedDrawing) {
            renderLayer.format().clearBufferState();
        }

        VertexBuffer.unbind();
        renderLayer.clearRenderState();

        this.world.getProfiler().pop();
        this.world.getProfiler().pop();

        return count;
    }

    public void renderBlockOverlays(Matrix4f matrix4f, Camera camera, Matrix4f projMatrix) {
        this.renderBlockOverlay(OverlayRenderType.OUTLINE, matrix4f, camera, projMatrix);
        this.renderBlockOverlay(OverlayRenderType.QUAD, matrix4f, camera, projMatrix);
    }

    protected static void initShader(ShaderInstance shader, Matrix4f matrix4f, Matrix4f projMatrix) {
        for (int i = 0; i < 12; ++i) shader.setSampler("Sampler" + i, RenderSystem.getShaderTexture(i));

        if (shader.MODEL_VIEW_MATRIX != null) shader.MODEL_VIEW_MATRIX.set(matrix4f);
        if (shader.PROJECTION_MATRIX != null) shader.PROJECTION_MATRIX.set(projMatrix);
        if (shader.COLOR_MODULATOR != null) shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        if (shader.FOG_START != null) shader.FOG_START.set(RenderSystem.getShaderFogStart());
        if (shader.FOG_END != null) shader.FOG_END.set(RenderSystem.getShaderFogEnd());
        if (shader.FOG_COLOR != null) shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        if (shader.TEXTURE_MATRIX != null) shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        if (shader.GAME_TIME != null) shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
    }

    protected void renderBlockOverlay(OverlayRenderType type, Matrix4f matrix4f, Camera camera, Matrix4f projMatrix) {
        RenderType renderLayer = RenderType.translucent();
        renderLayer.setupRenderState();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Vec3 cameraPos = camera.getPosition();
        double x = cameraPos.x;
        double y = cameraPos.y;
        double z = cameraPos.z;

        this.world.getProfiler().push("overlay_" + type.name());
        this.world.getProfiler().popPush("render");

        boolean renderThrough = Configs.Visuals.SCHEMATIC_OVERLAY_RENDER_THROUGH.getBooleanValue() || Hotkeys.RENDER_OVERLAY_THROUGH_BLOCKS.getKeybind().isKeybindHeld();

        if (renderThrough) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }

        ShaderInstance originalShader = RenderSystem.getShader();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        ShaderInstance shader = RenderSystem.getShader();
        BufferUploader.reset();

        // I tried using the matrix4f value here, only to have things break
        Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();

        for (int i = this.renderInfos.size() - 1; i >= 0; --i) {
            ChunkRendererSchematicVbo renderer = this.renderInfos.get(i);

            if (renderer.getChunkRenderData() != ChunkRenderDataSchematic.EMPTY && renderer.hasOverlay()) {
                ChunkRenderDataSchematic compiledChunk = renderer.getChunkRenderData();

                if (!compiledChunk.isOverlayTypeEmpty(type)) {
                    VertexBuffer buffer = renderer.getOverlayVertexBuffer(type);
                    BlockPos chunkOrigin = renderer.getOrigin();

                    matrix4fStack.pushMatrix();
                    matrix4fStack.translate((float) (chunkOrigin.getX() - x), (float) (chunkOrigin.getY() - y), (float) (chunkOrigin.getZ() - z));
                    buffer.bind();
                    buffer.drawWithShader(matrix4fStack, projMatrix, shader);

                    VertexBuffer.unbind();
                    matrix4fStack.popMatrix();
                }
            }
        }

        renderLayer.clearRenderState();

        RenderSystem.setShader(() -> originalShader);
        RenderSystem.disableBlend();

        this.world.getProfiler().pop();
    }

    public boolean renderBlock(BlockAndTintGetter world, BlockState state, BlockPos pos, PoseStack matrixStack, BufferBuilder bufferBuilderIn) {
        try {
            RenderShape renderType = state.getRenderShape();

            if (renderType == RenderShape.INVISIBLE) {
                return false;
            } else {
                return renderType == RenderShape.MODEL &&
                        this.blockModelRenderer.renderModel(world, this.getModelForState(state), state, pos, matrixStack, bufferBuilderIn, state.getSeed(pos));
            }
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating block in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being tesselated");
            CrashReportCategory.populateBlockDetails(crashreportcategory, world, pos, state);
            throw new ReportedException(crashreport);
        }
    }

    public void renderFluid(BlockAndTintGetter world, FluidState state, BlockPos pos, BufferBuilder bufferBuilderIn) {
        this.blockRenderManager.renderLiquid(pos, world, bufferBuilderIn, state.createLegacyBlock(), state);
    }

    public BakedModel getModelForState(BlockState state) {
        if (state.getRenderShape() == RenderShape.ENTITYBLOCK_ANIMATED) {
            return this.blockRenderManager.getBlockModelShaper().getModelManager().getMissingModel();
        }

        return this.blockRenderManager.getBlockModel(state);
    }

    public void renderEntities(Camera camera, Frustum frustum, Matrix4f matrix4f, float partialTicks) {
        if (this.renderEntitiesStartupCounter > 0) {
            --this.renderEntitiesStartupCounter;
        } else {
            this.world.getProfiler().push("prepare");

            double cameraX = camera.getPosition().x;
            double cameraY = camera.getPosition().y;
            double cameraZ = camera.getPosition().z;

            MC.getBlockEntityRenderDispatcher().prepare(this.world, camera, this.mc.hitResult);
            this.entityRenderDispatcher.prepare(this.world, camera, this.mc.crosshairPickEntity);

            this.countEntitiesTotal = 0;
            this.countEntitiesRendered = 0;
            this.countEntitiesHidden = 0;

            this.countEntitiesTotal = this.world.getRegularEntityCount();

            this.world.getProfiler().popPush("regular_entities");
            //List<Entity> entitiesMultipass = Lists.<Entity>newArrayList();

            // TODO --> Convert Matrix4f back to to MatrixStack?
            //  Causes strange entity behavior (translations not applied)
            //  if this is missing ( Including the push() and pop() ... ?)
            //  Doing this restores the expected behavior of Entity Rendering in the Schematic World

            PoseStack matrixStack = new PoseStack();
            matrixStack.pushPose();
            matrixStack.mulPose(matrix4f);
            matrixStack.popPose();

            MultiBufferSource.BufferSource entityVertexConsumers = this.bufferBuilders.bufferSource();
            LayerRange layerRange = DataManager.getRenderLayerRange();

            for (ChunkRendererSchematicVbo chunkRenderer : this.renderInfos) {
                BlockPos pos = chunkRenderer.getOrigin();
                ChunkSchematic chunk = this.world.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                List<Entity> list = chunk.getEntityList();

                if (!list.isEmpty()) {
                    for (Entity entityTmp : list) {
                        if (!layerRange.isPositionWithinRange((int) entityTmp.getX(), (int) entityTmp.getY(), (int) entityTmp.getZ())) {
                            continue;
                        }

                        boolean shouldRender = this.entityRenderDispatcher.shouldRender(entityTmp, frustum, cameraX, cameraY, cameraZ);

                        if (shouldRender) {
                            double x = entityTmp.getX() - cameraX;
                            double y = entityTmp.getY() - cameraY;
                            double z = entityTmp.getZ() - cameraZ;

                            matrixStack.pushPose();

                            // TODO --> this render() call does not seem to have a push() and pop(),
                            //  and does not accept Matrix4f/Matrix4fStack as a parameter
                            this.entityRenderDispatcher.render(entityTmp, x, y, z, entityTmp.getYRot(), 1.0f, matrixStack, entityVertexConsumers, this.entityRenderDispatcher.getPackedLightCoords(entityTmp, partialTicks));
                            ++this.countEntitiesRendered;

                            matrixStack.popPose();
                        }
                    }
                }
            }

            this.world.getProfiler().popPush("block_entities");
            BlockEntityRenderDispatcher renderer = MC.getBlockEntityRenderDispatcher();

            for (ChunkRendererSchematicVbo chunkRenderer : this.renderInfos) {
                ChunkRenderDataSchematic data = chunkRenderer.getChunkRenderData();
                List<BlockEntity> tiles = data.getBlockEntities();

                if (!tiles.isEmpty()) {
                    BlockPos chunkOrigin = chunkRenderer.getOrigin();
                    ChunkSchematic chunk = this.world.getChunkProvider().getChunkForLighting(chunkOrigin.getX() >> 4, chunkOrigin.getZ() >> 4);

                    if (chunk != null && data.getTimeBuilt() >= chunk.getTimeCreated()) {
                        for (BlockEntity te : tiles) {
                            try {
                                BlockPos pos = te.getBlockPos();
                                matrixStack.pushPose();
                                matrixStack.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);

                                // TODO --> this render() call does not seem to have a push() and pop(),
                                //  and does not accept Matrix4f/Matrix4fStack as a parameter
                                renderer.render(te, partialTicks, matrixStack, entityVertexConsumers);

                                matrixStack.popPose();
                            } catch (Exception ignore) {
                            }
                        }
                    }
                }
            }

            synchronized (this.blockEntities) {
                for (BlockEntity te : this.blockEntities) {
                    try {
                        BlockPos pos = te.getBlockPos();
                        matrixStack.pushPose();
                        matrixStack.translate(pos.getX() - cameraX, pos.getY() - cameraY, pos.getZ() - cameraZ);

                        // TODO --> this render() call does not seem to have a push() and pop(),
                        //  and does not accept Matrix4f/Matrix4fStack as a parameter
                        renderer.render(te, partialTicks, matrixStack, entityVertexConsumers);

                        matrixStack.popPose();
                    } catch (Exception ignore) {
                    }
                }
            }

            this.world.getProfiler().pop();
        }
    }

    /*
    private boolean isOutlineActive(Entity entityIn, Entity viewer, Camera camera)
    {
        boolean sleeping = viewer instanceof LivingEntity && ((LivingEntity) viewer).isSleeping();

        if (entityIn == viewer && this.mc.options.perspective == 0 && sleeping == false)
        {
            return false;
        }
        else if (entityIn.isGlowing())
        {
            return true;
        }
        else if (this.mc.player.isSpectator() && this.mc.options.keySpectatorOutlines.isPressed() && entityIn instanceof PlayerEntity)
        {
            return entityIn.ignoreFrustumCheck || camera.isBoundingBoxInFrustum(entityIn.getBoundingBox()) || entityIn.isRidingOrBeingRiddenBy(this.mc.player);
        }
        else
        {
            return false;
        }
    }
    */

    public void updateBlockEntities(Collection<BlockEntity> toRemove, Collection<BlockEntity> toAdd) {
        synchronized (this.blockEntities) {
            this.blockEntities.removeAll(toRemove);
            this.blockEntities.addAll(toAdd);
        }
    }

    public void scheduleChunkRenders(int chunkX, int chunkZ) {
        if (Configs.Visuals.ENABLE_RENDERING.getBooleanValue() &&
                Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue()) {
            this.chunkRendererDispatcher.scheduleChunkRender(chunkX, chunkZ);
        }
    }
}
