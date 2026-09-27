package com.glucose.aerowarpdrive;

import com.glucose.aerowarpdrive.block.WarpAnchorBlock;
import com.glucose.aerowarpdrive.block.WarpDriveBlock;
import com.glucose.aerowarpdrive.blockentity.WarpAnchorBlockEntity;
import com.glucose.aerowarpdrive.blockentity.WarpDriveBlockEntity;
import com.glucose.aerowarpdrive.core.WarpAnchor;
import com.glucose.aerowarpdrive.network.AWDEndecs;
import com.glucose.aerowarpdrive.network.AnchorListPacket;
import com.glucose.aerowarpdrive.network.WarpDriveTargetPacket;
import com.glucose.aerowarpdrive.store.SavedAnchorsDataStore;
import io.wispforest.owo.network.OwoNetChannel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(AeronauticsWarpDrive.MODID)
public class AeronauticsWarpDrive {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "aerowarpdrive";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "aerowarpdrive" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "aerowarpdrive" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold BlockEntities which will all be registered under the "aerowarpdrive" namespace
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, AeronauticsWarpDrive.MODID);

    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "aerowarpdrive" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);


    // Block
    public static final DeferredBlock<WarpDriveBlock> WARP_DRIVE = BLOCKS.register(
            "warp_drive",
            () -> new WarpDriveBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).destroyTime(5f).noOcclusion())
    );
    public static final DeferredBlock<WarpAnchorBlock> WARP_ANCHOR = BLOCKS.register(
            "warp_anchor",
            () -> new WarpAnchorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).destroyTime(5f).noOcclusion())
    );


    // BlockItem
    public static final DeferredItem<BlockItem> WARP_DRIVE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("warp_drive", WARP_DRIVE);
    public static final DeferredItem<BlockItem> WARP_ANCHOR_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("warp_anchor", WARP_ANCHOR);


    // Item
    public static final DeferredItem<Item> UNTETHERED_FRUIT = ITEMS.registerSimpleItem("untethered_fruit", new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));


    // BlockEntityType
    public static final Supplier<BlockEntityType<WarpDriveBlockEntity>> WARP_DRIVE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "warp_drive_block_entity",
            () -> BlockEntityType.Builder.of(
                    WarpDriveBlockEntity::new,
                    WARP_DRIVE.get()
            )
                    .build(null)
    );
    public static final Supplier<BlockEntityType<WarpAnchorBlockEntity>> WARP_ANCHOR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "warp_anchor_block_entity",
            () -> BlockEntityType.Builder.of(
                    WarpAnchorBlockEntity::new,
                    WARP_ANCHOR.get()
            )
                    .build(null)
    );


    public static final OwoNetChannel WARP_DRIVE_SELECT_CHANNEL = OwoNetChannel.create(ResourceLocation.fromNamespaceAndPath(MODID, "warp_drive_select"));
    public static final OwoNetChannel ANCHORS_LIST_SEND_CHANNEL = OwoNetChannel.create(ResourceLocation.fromNamespaceAndPath(MODID, "anchors_list_send"));


    // Creates a creative tab with the id "aerowarpdrive:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_MODE_TAB = CREATIVE_MODE_TABS.register("creative_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.aerowarpdrive")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> UNTETHERED_FRUIT.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(UNTETHERED_FRUIT.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
                output.accept(WARP_ANCHOR_BLOCK_ITEM.get());
                output.accept(WARP_DRIVE_BLOCK_ITEM.get());
            }).build());


    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public AeronauticsWarpDrive(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so block entities get registered
        BLOCK_ENTITY_TYPES.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        ANCHORS_LIST_SEND_CHANNEL.addEndecs(builder -> {
            builder.register(AnchorListPacket.ENDEC, AnchorListPacket.class);
            builder.register(AWDEndecs.WARP_ANCHOR_ENDEC, WarpAnchor.class);
        });
        ANCHORS_LIST_SEND_CHANNEL.registerClientboundDeferred(AnchorListPacket.class);
        WARP_DRIVE_SELECT_CHANNEL.registerServerbound(WarpDriveTargetPacket.class, ((message, access) -> {
            WarpAnchor anchor = SavedAnchorsDataStore.getDimensionAnchorStore(access.player().serverLevel()).getAnchorByUid(message.anchorId()).get();
            BlockEntity entity = access.player().serverLevel().getBlockEntity(message.pos());

            System.out.println(anchor.getName());
        }));

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (AeronauticsWarpDrive) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
//            event.accept(WARP_DRIVE_BLOCK_ITEM);
//            event.accept(WARP_ANCHOR_BLOCK_ITEM);
//            event.accept(UNTETHERED_FRUIT);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
