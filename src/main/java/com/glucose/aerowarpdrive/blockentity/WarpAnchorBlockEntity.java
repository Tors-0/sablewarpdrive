package com.glucose.aerowarpdrive.blockentity;

import com.glucose.aerowarpdrive.block.WarpAnchorBlock;
import com.glucose.aerowarpdrive.core.WarpAnchor;
import com.glucose.aerowarpdrive.store.SavedAnchorsDataStore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static com.glucose.aerowarpdrive.AeronauticsWarpDrive.WARP_ANCHOR_BLOCK_ENTITY;

public class WarpAnchorBlockEntity extends BlockEntity implements Nameable {
    private static String anchorNbtKey = "WarpAnchor";

    private WarpAnchor anchor;

    public WarpAnchorBlockEntity(BlockPos pos, BlockState blockState) {
        super(WARP_ANCHOR_BLOCK_ENTITY.get(), pos, blockState);

        anchor = new WarpAnchor(pos);
    }

    public WarpAnchor getAnchor() {
        return anchor;
    }

    public void activate() {
        if (this.isActivated()) return;
        if (getLevel() instanceof ServerLevel dimension) {
            SavedAnchorsDataStore store = SavedAnchorsDataStore.getDimensionAnchorStore(dimension);
            this.anchor = new WarpAnchor(this.getBlockPos(), this.anchor.getName(), this.anchor.getUId());
            store.addAnchor(this.anchor);
        }
        getLevel().setBlock(getBlockPos(), getBlockState().setValue(WarpAnchorBlock.ACTIVATED, true), WarpAnchorBlock.UPDATE_CLIENTS);
    }

    public boolean isActivated() {
        return this.getBlockState().getValue(WarpAnchorBlock.ACTIVATED);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        CustomData compound = componentInput.get(DataComponents.CUSTOM_DATA);
        if (compound != null)
            this.anchor = WarpAnchor.read(compound.copyTag());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CUSTOM_DATA, CustomData.of(WarpAnchor.write(anchor, new CompoundTag())));
        components.set(DataComponents.CUSTOM_NAME, Component.literal(anchor.getName()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        anchor = WarpAnchor.read(tag.getCompound(anchorNbtKey));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(anchorNbtKey, WarpAnchor.write(anchor, new CompoundTag()));
    }

    @Override
    public Component getName() {
        return Component.literal(this.anchor.getName()).withStyle(Style.EMPTY.withItalic(true));
    }

    @Override
    public boolean hasCustomName() {
        return true;
    }
}
