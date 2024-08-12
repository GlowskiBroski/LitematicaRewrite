package fi.dy.masa.litematica;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Litematica implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);

    @Override
    public void onInitialize() {
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }

    public static void debugLog(String msg, Object... args) {
        if (Configs.Generic.DEBUG_LOGGING.getBooleanValue()) {
            Litematica.LOGGER.info(msg, args);
        }
    }
}
