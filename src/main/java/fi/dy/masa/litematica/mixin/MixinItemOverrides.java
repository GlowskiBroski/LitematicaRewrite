package fi.dy.masa.litematica.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import static com.glow.litematicarecode.LitematicaRewrite.MC;

@Mixin(ItemOverrides.class)
public abstract class MixinItemOverrides {
    @SuppressWarnings("deprecation")
    //@Redirect(method = "resolve", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/item/ModelPredicateProvider;call(" + "Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/world/ClientWorld;" + "Lnet/minecraft/entity/LivingEntity;I)F"))
    @Redirect(method = "resolve", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemPropertyFunction;call(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/LivingEntity;I)F"))
    private float fixCrashWithNullWorld(ItemPropertyFunction provider, ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int i) {
        if (world == null) {
            return provider.call(stack, MC.level, entity, i);
        }

        return provider.call(stack, world, entity, i);
    }
}
