package fi.dy.masa.litematica.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Entity.class)
public interface IMixinEntity {
    @Accessor("level")
    void litematica_setWorld(Level world);

    @Invoker("load")
    void readCustomDataFromNbt(CompoundTag nbt);
}
