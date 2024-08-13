package fi.dy.masa.litematica.mixin;

import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen extends Screen {
    private MixinAbstractContainerScreen(Component title) {
        super(title);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void renderSlotHighlights(GuiGraphics drawContext, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MaterialListHudRenderer.renderLookedAtBlockInInventory((AbstractContainerScreen<?>) (Object) this, this.minecraft);
    }
}
