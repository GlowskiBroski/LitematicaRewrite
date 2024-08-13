package fi.dy.masa.litematica.mixin;

import com.glow.litematicarecode.LitematicaRewrite;
import fi.dy.masa.litematica.data.DataManager;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * They keep moving where the effective CustomPayload handling is... keeping them both
 */
@Mixin(ClientCommonPacketListenerImpl.class)
public class MixinClientCommonPacketListenerImpl {
    @Inject(method = "handleCustomPayload(Lnet/minecraft/network/protocol/common/ClientboundCustomPayloadPacket;)V", at = @At("HEAD"))
    private void litematica_onCustomPayload(ClientboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (packet.payload().type().id().equals(DataManager.CARPET_HELLO)) {
            LitematicaRewrite.debugLog("ClientCommonNetworkHandler#litematica_onCustomPayload(): received carpet hello packet");
            DataManager.setIsCarpetServer(true);
        }
    }
}
