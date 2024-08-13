package fi.dy.masa.litematica.mixin;

import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SignBlockEntity.class)
public interface IMixinSignBlockEntity {
    @Accessor("frontText")
    SignText litematica_getFrontText();

    @Accessor("backText")
    SignText litematica_getBackText();
}
