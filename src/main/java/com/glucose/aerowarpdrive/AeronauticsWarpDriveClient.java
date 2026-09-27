package com.glucose.aerowarpdrive;

import com.glucose.aerowarpdrive.gui.WarpDriveScreen;
import com.glucose.aerowarpdrive.network.AnchorListPacket;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import static com.glucose.aerowarpdrive.AeronauticsWarpDrive.ANCHORS_LIST_SEND_CHANNEL;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = AeronauticsWarpDrive.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = AeronauticsWarpDrive.MODID, value = Dist.CLIENT)
public class AeronauticsWarpDriveClient {
    public AeronauticsWarpDriveClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        ANCHORS_LIST_SEND_CHANNEL.registerClientbound(AnchorListPacket.class, ((message, access) -> {
            access.runtime().setScreen(new WarpDriveScreen(message.anchors(), message.pos(), message.message()));
        }));

        // Some client setup code
        AeronauticsWarpDrive.LOGGER.info("HELLO FROM CLIENT SETUP");
        AeronauticsWarpDrive.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
