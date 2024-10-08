package com.glow.litematicarecode;

import com.glow.gbgui.gui.GuiManager;
import com.glow.litematicarecode.gui.TestGUI;
import fi.dy.masa.litematica.Reference;
import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.event.InputHandler;
import fi.dy.masa.litematica.event.KeyCallbacks;
import fi.dy.masa.litematica.event.RenderHandler;
import fi.dy.masa.litematica.event.WorldLoadListener;
import fi.dy.masa.litematica.render.infohud.StatusInfoRenderer;
import fi.dy.masa.litematica.scheduler.ClientTickHandler;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.event.WorldLoadHandler;
import fi.dy.masa.malilib.interfaces.IRenderer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

//TODO: Put new and repaired classes into the com.glow.litematicarecode folder. Slowly transition to a better base

//TODO: After you make refinements, separate out the common classes from this and phoenixclient into a library that can be used by both
// It will be more organized. That would be very nice

public class LitematicaRewrite implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);
    public static final Minecraft MC = Minecraft.getInstance();

    public static void debugLog(String msg, Object... args) {
        if (Configs.Generic.DEBUG_LOGGING.getBooleanValue()) {
            LOGGER.info(msg, args);
        }
    }

    @Override
    public void onInitialize() {
        oldLitematicaInitializers();

        GuiManager.getInstance()
                .addGUI(new TestGUI("Title"), new KeyMapping("Test GUI Key", GLFW.GLFW_KEY_ENTER, "Litematica"));
    }

    private void oldLitematicaInitializers() {
        ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());

        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerKeyboardInputHandler(InputHandler.getInstance());
        InputEventHandler.getInputManager().registerMouseInputHandler(InputHandler.getInstance());

        IRenderer renderer = new RenderHandler();
        RenderEventHandler.getInstance().registerGameOverlayRenderer(renderer);
        RenderEventHandler.getInstance().registerWorldLastRenderer(renderer);

        TickHandler.getInstance().registerClientTickHandler(new ClientTickHandler());

        WorldLoadListener listener = new WorldLoadListener();
        WorldLoadHandler.getInstance().registerWorldLoadPreHandler(listener);
        WorldLoadHandler.getInstance().registerWorldLoadPostHandler(listener);

        KeyCallbacks.init(MC);
        StatusInfoRenderer.init();

        DataManager.getAreaSelectionsBaseDirectory();
        DataManager.getSchematicsBaseDirectory();
    }
}
