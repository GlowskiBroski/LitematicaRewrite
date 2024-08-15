package fi.dy.masa.litematica.mixin;

import net.minecraft.client.DebugQueryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DebugQueryHandler.class)
public interface IMixinDebugQueryHandler {
    @Accessor("transactionId")
    int currentTransactionId();
}