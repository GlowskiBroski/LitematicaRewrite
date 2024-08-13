package fi.dy.masa.litematica.event;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.conversion.SchematicConversionMaps;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.malilib.interfaces.IWorldLoadListener;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public class WorldLoadListener implements IWorldLoadListener {
    @Override
    public void onWorldLoadPre(@Nullable ClientLevel worldBefore, @Nullable ClientLevel worldAfter, Minecraft mc) {
        // Save the settings before the integrated server gets shut down
        if (worldBefore != null) {
            DataManager.save();
        }
    }

    @Override
    public void onWorldLoadPost(@Nullable ClientLevel worldBefore, @Nullable ClientLevel worldAfter, Minecraft mc) {
        SchematicWorldHandler.INSTANCE.recreateSchematicWorld(worldAfter == null);

        if (worldAfter != null) {
            DataManager.load();
            SchematicConversionMaps.computeMaps();
        } else {
            DataManager.clear();
        }
    }
}
