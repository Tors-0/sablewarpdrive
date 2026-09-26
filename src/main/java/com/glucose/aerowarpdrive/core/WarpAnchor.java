package com.glucose.aerowarpdrive.core;

import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.UUID;

public class WarpAnchor {
    static String[] nameAdjectives = {
            "Ancient ","Beautiful ","Boring ","Bustling ","Charming ","Contemporary ","Compact ","Cosmopolitan ","Crowded ","Exciting ","Expensive ","Famous ","Fantastic ","Fascinating ","Huge ","Lively ","Inexpensive ","Popular ","Picturesque ","Polluted ","Touristy ","Voidswallowed ","Faraway ","Nearby ","Inaccessible ","Open ","Blazing "
    };
    static String[] nameNouns = {
            "Plains","Village","Cave","Shore","Wood","Forest","Swamp","Marsh","Wetland","Bog","Moor","Fells","Jungle","Rainforest","Fields","Grass","Savannah","Flats","Prairie","Tundra","Iceberg","Glacier","Snowfields","Hills","Highlands","Heights","Plateau","Badland","Kame","Shield","Downs","Ridge","Hollow","Valley","Vale","Glen","Dell","Mountain","Peak","Summit","Rise","Pass","Notch","Crown","Mount","Furth","Canyon","Cliff","Bluff","Ravine","Gully","Gulch","Gorge","Desert","Scrub","Waste","Wasteland","Sands","Dunes","Volcano","Crater","Cone","Geyser","Lava Fields","Ocean","Bay","Port","Harbour","Fjord","Vike","Cove","Shoals","Lagoon","Firth","Bight","Sound","Strait","Gulf","Inlet","Loch","Bayou", "Dock","Pier","Anchorage","Jetty","Wharf","Marina","Landing","Mooring","Berth","Quay","Staith", "River","Stream","Creek","Brook","Waterway","Rill","Delta","Bank","Runoff","Channel","Bend","Meander", "Backwater","Lake","Pool","Pond","Dugout","Fountain","Spring","Watering-hole","Oasis","Well","Cistern", "Reservoir","Waterfall","Falls","Rapids","Cataract","Cascade","Bridge","Crossing","Causeway","Viaduct","Aquaduct","Ford","Ferry","Dam","Dike","Bar","Canal","Ditch","Peninsula","Isthmus","Island","Isle", "Sandbar","Reef","Atoll","Archipelago","Cay","Shipwreck","Derelict","Clearing","Meadow","Grove","Glade","Fairy Ring","Ruin","Acropolis","Desolation","Remnant","Remains","Henge","Cairn","Circle","Mound","Barrow", "Earthworks","Petroglyphs","Lookout","Aerie","Promontory","Outcropping","Ledge","Overhang","Mesa","Butte", "Outland","Outback","Territory","Reaches","Wild","Wilderness","Expanse","View","Vista","Tableau", "Spectacle","Landscape","Seascape","Aurora","Landmark","Battlefield","Trenches","Gambit","Folly","Conquest", "Claim","Muster","Post","Path","Road","Track","Route","Highway","Way","Trail","Lane","Thoroughfare","Pike", "Alley","Street","Avenue","Boulevard","Promenade","Esplande","Boardwalk","Crossroad","Junction", "Intersection","Turn","Corner","Plaza","Terrace","Square","Courtyard","Court","Park","Marketplace","Bazaar","Fairground"
    };

    private UUID id;
    private BlockPos pos;
    private String name;

    public WarpAnchor(BlockPos pos, String name, UUID id) {
        this.pos = pos;
        this.name = name;
        this.id = id;
    }
    public WarpAnchor(BlockPos pos, String name) {
        this.pos = pos;
        this.name = name;
        this.id = UUID.randomUUID();
    }
    public WarpAnchor(BlockPos pos) {
        this.pos = pos;
        this.name = nameAdjectives[(int) (Math.random() * nameAdjectives.length)] + nameNouns[(int) (Math.random() * nameNouns.length)];
        this.id = UUID.randomUUID();
    }

    public UUID getUId() {
        return id;
    }

    public BlockPos getPos() {
        return pos;
    }

    public String getName() {
        return name;
    }

    public String getSummary(Player origin) {
        double distance = SableCompanion.INSTANCE.distanceSquaredWithSubLevels(origin.level(), origin.position(), this.pos.getCenter());
        return String.format("%s | %.1fm", this.name, Math.sqrt(distance));
    }

    public static CompoundTag write(WarpAnchor anchor, CompoundTag compound) {
        compound.put("AnchorUid", NbtUtils.createUUID(anchor.id));
        compound.putString("Name", anchor.name);
        compound.put("BlockPos", NbtUtils.writeBlockPos(anchor.pos));
        return compound;
    }

    public static WarpAnchor read(CompoundTag compound) {
        final var anchorId = NbtUtils.loadUUID(Objects.requireNonNull(compound.get("AnchorUid")));
        final var anchorPos = NbtUtils.readBlockPos(compound, "BlockPos");
        final var anchorName = compound.getString("Name");

        return anchorPos.map(blockPos -> new WarpAnchor(blockPos, anchorName, anchorId)).orElse(null);
    }
}
