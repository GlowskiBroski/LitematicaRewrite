package fi.dy.masa.litematica.world;

import com.glow.litematicarecode.LitematicaRewrite;
import fi.dy.masa.litematica.render.LitematicaRenderer;
import fi.dy.masa.litematica.render.schematic.WorldRendererSchematic;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

import static com.glow.litematicarecode.LitematicaRewrite.MC;

public class SchematicWorldHandler {
    public static final SchematicWorldHandler INSTANCE = new SchematicWorldHandler(LitematicaRenderer.getInstance()::getWorldRenderer);

    protected final Supplier<WorldRendererSchematic> rendererSupplier;
    @Nullable
    protected WorldSchematic world;

    // The supplier can return null, but it can't be null itself!
    public SchematicWorldHandler(Supplier<WorldRendererSchematic> rendererSupplier) {
        this.rendererSupplier = rendererSupplier;
    }

    @Nullable
    public static WorldSchematic getSchematicWorld() {
        return INSTANCE.getWorld();
    }

    @Nullable
    public WorldSchematic getWorld() {
        if (this.world == null) {
            this.world = createSchematicWorld(this.rendererSupplier.get());
        }

        return this.world;
    }

    public static WorldSchematic createSchematicWorld(@Nullable WorldRendererSchematic worldRenderer) {
        Level world = MC.level;

        if (world == null) {
            return null;
        }

        HolderGetter.Provider lookup = world.registryAccess().asGetterLookup();
        Optional<HolderGetter<DimensionType>> entryLookup = lookup.lookup(Registries.DIMENSION_TYPE);
        Holder<DimensionType> entry = null;

        if (entryLookup.isPresent()) {
            Optional<? extends Holder<DimensionType>> dimOptional = entryLookup.get().get(BuiltinDimensionTypes.OVERWORLD);

            if (dimOptional.isPresent()) {
                entry = dimOptional.get();
            }
        }

        // Use the DimensionType of the current client world as a fallback
        if (entry == null) {
            entry = world.dimensionTypeRegistration();
        }

        ClientLevel.ClientLevelData levelInfo = new ClientLevel.ClientLevelData(Difficulty.PEACEFUL, false, true);

        return new WorldSchematic(levelInfo, world.registryAccess(), entry, MC::getProfiler, worldRenderer);
    }

    public void recreateSchematicWorld(boolean remove) {
        if (remove) {
            LitematicaRewrite.debugLog("Removing the schematic world...");
            this.world = null;
        } else {
            LitematicaRewrite.debugLog("(Re-)creating the schematic world...");
            @Nullable WorldRendererSchematic worldRenderer = this.world != null ? this.world.worldRenderer : LitematicaRenderer.getInstance().getWorldRenderer();
            // Note: The dimension used here must have no skylight, because the custom Chunks don't have those arrays
            this.world = createSchematicWorld(worldRenderer);
            LitematicaRewrite.debugLog("Schematic world (re-)created: {}", this.world);
        }

        LitematicaRenderer.getInstance().onSchematicWorldChanged(this.world);
    }
}
