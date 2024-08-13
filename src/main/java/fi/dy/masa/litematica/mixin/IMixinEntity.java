package fi.dy.masa.litematica.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface IMixinEntity {
    @Accessor("level")
    void litematica_setWorld(Level world);
}
