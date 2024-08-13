package fi.dy.masa.litematica.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FaceAttachedHorizontalDirectionalBlock.class)
public interface IMixinFaceAttachedHorizontalDirectionalBlock {

    @Invoker("canSurvive")
    boolean invokeCanPlaceAt(BlockState state, LevelReader world, BlockPos pos);
}
