package com.glucose.aerowarpdrive.network;

import com.glucose.aerowarpdrive.core.WarpAnchor;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.core.BlockPos;

import java.util.List;

public record AnchorListPacket(List<WarpAnchor> anchors, BlockPos pos) {
    public static final StructEndec<AnchorListPacket> ENDEC = StructEndecBuilder.of(
            AWDEndecs.WARP_ANCHOR_ENDEC.listOf().fieldOf("anchors", AnchorListPacket::anchors),
            MinecraftEndecs.BLOCK_POS.fieldOf("pos", AnchorListPacket::pos),
            AnchorListPacket::new
    );
}
