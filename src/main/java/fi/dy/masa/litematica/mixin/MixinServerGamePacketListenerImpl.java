package fi.dy.masa.litematica.mixin;

import fi.dy.masa.litematica.config.Configs;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 1010)
public class MixinServerGamePacketListenerImpl {

    @Redirect(method = "handleUseItemOn", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 litematica_removeHitPosCheck(Vec3 hitVec, Vec3 blockCenter) {
        if (Configs.Generic.ITEM_USE_PACKET_CHECK_BYPASS.getBooleanValue()) {
            return Vec3.ZERO;
        }

        return hitVec.subtract(blockCenter);
    }
}
