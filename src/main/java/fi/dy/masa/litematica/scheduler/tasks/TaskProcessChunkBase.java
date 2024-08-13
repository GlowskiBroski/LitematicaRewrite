package fi.dy.masa.litematica.scheduler.tasks;

import com.google.common.collect.ArrayListMultimap;
import fi.dy.masa.litematica.render.infohud.InfoHud;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import static com.glow.litematicarecode.LitematicaRewrite.MC;

public abstract class TaskProcessChunkBase extends TaskBase {
    protected final ArrayListMultimap<ChunkPos, IntBoundingBox> boxesInChunks = ArrayListMultimap.create();
    protected final ArrayList<ChunkPos> pendingChunks = new ArrayList<>();
    protected final ClientLevel clientWorld;
    protected final WorldSchematic schematicWorld;
    protected final Level world;
    protected final boolean isClientWorld;
    protected PositionUtils.ChunkPosComparator comparator = new PositionUtils.ChunkPosComparator();

    protected TaskProcessChunkBase(String nameOnHud) {
        this.clientWorld = MC.level;
        this.world = WorldUtils.getBestWorld(MC);
        this.schematicWorld = SchematicWorldHandler.getSchematicWorld();
        this.isClientWorld = (this.world == MC.level);
        this.name = StringUtils.translate(nameOnHud);
        this.comparator.setClosestFirst(true);

        InfoHud.getInstance().addInfoHudRenderer(this, true);
    }

    @Override
    public boolean execute() {
        return this.executeForAllPendingChunks();
    }

    @Override
    public void stop() {
        // Multiplayer, just a client world
        if (this.isClientWorld) {
            this.onStop();
        }
        // Single player, operating in the integrated server world
        else {
            MC.execute(this::onStop);
        }
    }

    protected void onStop() {
        this.notifyListener();
    }

    protected abstract boolean canProcessChunk(ChunkPos pos);

    protected boolean processChunk(ChunkPos pos) {
        return true;
    }

    protected boolean executeForAllPendingChunks() {
        Iterator<ChunkPos> iterator = this.pendingChunks.iterator();
        int processed = 0;

        while (iterator.hasNext()) {
            ChunkPos pos = iterator.next();

            if (this.canProcessChunk(pos) && this.processChunk(pos)) {
                iterator.remove();
                ++processed;
            }
        }

        if (processed > 0) {
            this.updateInfoHudLinesPendingChunks(this.pendingChunks);
        }

        this.finished = this.pendingChunks.isEmpty();

        return this.finished;
    }

    protected void addPerChunkBoxes(Collection<Box> allBoxes) {
        this.addPerChunkBoxes(allBoxes, new LayerRange(null));
    }

    protected void addPerChunkBoxes(Collection<Box> allBoxes, LayerRange range) {
        this.boxesInChunks.clear();
        this.pendingChunks.clear();

        if (range.getLayerMode() == LayerMode.ALL) {
            PositionUtils.getPerChunkBoxes(allBoxes, this::clampToWorldHeightAndAddBox);
        } else {
            PositionUtils.getLayerRangeClampedPerChunkBoxes(allBoxes, range, this::clampToWorldHeightAndAddBox);
        }

        this.pendingChunks.addAll(this.boxesInChunks.keySet());

        this.sortChunkList();
    }

    protected void clampToWorldHeightAndAddBox(ChunkPos pos, IntBoundingBox box) {
        box = PositionUtils.clampBoxToWorldHeightRange(box, this.clientWorld);

        if (box != null) {
            this.boxesInChunks.put(pos, box);
        }
    }

    protected List<IntBoundingBox> getBoxesInChunk(ChunkPos pos) {
        return this.boxesInChunks.get(pos);
    }

    protected void sortChunkList() {
        if (this.pendingChunks.size() > 0) {
            if (MC.player != null) {
                this.comparator.setReferencePosition(MC.player.blockPosition());
                this.pendingChunks.sort(this.comparator);
            }

            this.updateInfoHudLines();
            this.onChunkListSorted();
        }
    }

    protected void onChunkListSorted() {
    }

    protected void updateInfoHudLines() {
        this.updateInfoHudLinesPendingChunks(this.pendingChunks);
    }
}
