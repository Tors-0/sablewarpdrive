package com.glucose.aerowarpdrive.gui;

import com.glucose.aerowarpdrive.AeronauticsWarpDrive;
import com.glucose.aerowarpdrive.core.WarpAnchor;
import com.glucose.aerowarpdrive.network.WarpDrivePacket;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class WarpDriveScreen extends BaseOwoScreen<FlowLayout> {
    private final List<WarpAnchor> anchors;

    public WarpDriveScreen(List<WarpAnchor> anchors) {
        this.anchors = anchors;
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
        for (WarpAnchor anchor : anchors) {
            layout.child(
                    Components.button(
                            Component.literal(anchor.getSummary(player)),
                            buttonComponent -> {
                                AeronauticsWarpDrive.WARP_DRIVE_SELECT_CHANNEL.clientHandle().send(new WarpDrivePacket(anchor.getUId()));
                                getMinecraft().setScreen(null);
                            })
            );
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
