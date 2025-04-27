package net.flazesmp.flazesmpitems.network;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FlazeSMPItems.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    private static int id = 0;
    
    public static void init() {
        // Make sure the OpenGuiPacket and UpdateItemPacket classes exist
        INSTANCE.registerMessage(
                id++,
                OpenGuiPacket.class,
                OpenGuiPacket::encode,
                OpenGuiPacket::decode,
                OpenGuiPacket::handle
        );
        
        INSTANCE.registerMessage(
                id++,
                UpdateItemPacket.class,
                UpdateItemPacket::encode,
                UpdateItemPacket::decode,
                UpdateItemPacket::handle
        );
        
        FlazeSMPItems.LOGGER.info("Registered network packets");
    }
    
    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }
    
    public static void sendToPlayer(Object packet, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
    
    public static void sendToAll(Object packet) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }
}