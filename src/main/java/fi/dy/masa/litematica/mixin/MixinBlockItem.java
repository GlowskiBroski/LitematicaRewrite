package fi.dy.masa.litematica.mixin;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.PlacementHandler;
import fi.dy.masa.litematica.util.PlacementHandler.UseContext;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockItem.class, priority = 980)
public abstract class MixinBlockItem extends Item {
    private MixinBlockItem(Item.Properties builder) {
        super(builder);
    }

    @Shadow
    protected abstract BlockState getPlacementState(BlockPlaceContext context);

    @Shadow
    protected abstract boolean canPlace(BlockPlaceContext context, BlockState state);

    @Shadow
    public abstract Block getBlock();

    @Inject(method = "getPlacementState", at = @At("HEAD"), cancellable = true)
    private void modifyPlacementState(BlockPlaceContext ctx, CallbackInfoReturnable<BlockState> cir) {
        if (Configs.Generic.EASY_PLACE_MODE.getBooleanValue() && Configs.Generic.EASY_PLACE_SP_HANDLING.getBooleanValue()) {
            BlockState stateOrig = this.getBlock().getStateForPlacement(ctx);

            if (stateOrig != null && this.canPlace(ctx, stateOrig)) {
                UseContext context = UseContext.from(ctx, ctx.getHand());
                cir.setReturnValue(PlacementHandler.applyPlacementProtocolToPlacementState(stateOrig, context));
            }
        }
    }
}
