package com.glucose.aerowarpdrive.network;

import com.glucose.aerowarpdrive.core.WarpAnchor;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.BuiltInEndecs;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;

public class AWDEndecs {
    public static final Endec<WarpAnchor> WARP_ANCHOR_ENDEC = StructEndecBuilder.of(
            MinecraftEndecs.BLOCK_POS.fieldOf("pos", WarpAnchor::getPos),
            Endec.STRING.fieldOf("name", WarpAnchor::getName),
            BuiltInEndecs.UUID.fieldOf("id", WarpAnchor::getUId),
            WarpAnchor::new
    );
}
