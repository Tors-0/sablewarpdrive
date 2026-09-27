package com.glucose.aerowarpdrive.gui;

import com.glucose.aerowarpdrive.AeronauticsWarpDrive;
import com.glucose.aerowarpdrive.core.WarpAnchor;
import com.glucose.aerowarpdrive.network.WarpDriveTargetPacket;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class WarpDriveScreen extends BaseOwoScreen<FlowLayout> {
    private final List<WarpAnchor> anchors;
    private final BlockPos pos;
    private final String message;

    public WarpDriveScreen(List<WarpAnchor> anchors, BlockPos pos, String message) {
        this.anchors = anchors;
        this.pos = pos;
        this.message = message;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        FlowLayout layout = Containers.verticalFlow(Sizing.fill(), Sizing.content());
        Player player = Objects.requireNonNull(getMinecraft().player);
        Component dimension = player.level().getDescription().copy()
                .append(Component.translatable("string.aerowarpdrive.destinations"));
        if (!anchors.isEmpty()) {
            for (WarpAnchor anchor : anchors) {
                layout.child(
                        Components.button(
                                Component.literal(anchor.getSummary(player)),
                                buttonComponent -> {
                                    AeronauticsWarpDrive.WARP_DRIVE_SELECT_CHANNEL.clientHandle().send(new WarpDriveTargetPacket(anchor.getUId(), pos));
                                    getMinecraft().setScreen(null);
                                })
                );
            }
        } else {
            layout.child(Components.label(Component.literal(message)).maxWidth(180));
        }
        layout
                .gap(2)
                .surface(Surface.PANEL_INSET)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .padding(Insets.of(2))
                .margins(Insets.right(6));

        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);
        rootComponent.child(
                Containers.verticalFlow(Sizing.fill(50), Sizing.content())
                        .child(Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                                .child(Components.label(dimension)
                                        .color(Color.ofDye(DyeColor.GRAY)))
                                .surface(Surface.PANEL)
                                .padding(Insets.of(6))
                                .margins(Insets.of(-6)))
                        .child(Containers.verticalScroll(Sizing.fill(), Sizing.fill(50), layout)
                                .scrollbarThiccness(6)
                                .scrollbar(ScrollContainer.Scrollbar.vanilla())
                                .padding(Insets.of(10))
                                .horizontalAlignment(HorizontalAlignment.CENTER))
                        .horizontalAlignment(HorizontalAlignment.CENTER)
                        .surface(Surface.PANEL)
                        .allowOverflow(true)
        );
    }
}
