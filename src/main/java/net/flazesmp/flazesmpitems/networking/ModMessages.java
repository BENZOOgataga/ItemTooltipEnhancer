package net.flazesmp.flazesmpitems.networking;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.networking.packet.OpenItemManagerGuiPacket;
import net.flazesmp.flazesmpitems.networking.packet.SaveItemDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    
    private static int packetId = 0;
    private static int id() {
        return packetId++;
    }
    
    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(FlazeSMPItems.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();
        
        INSTANCE = net;
        
        // Register packets
        net.messageBuilder(OpenItemManagerGuiPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(OpenItemManagerGuiPacket::decode)
                .encoder(OpenItemManagerGuiPacket::encode)
                .consumerMainThread(OpenItemManagerGuiPacket::handle)
                .add();
                
        net.messageBuilder(SaveItemDataPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(SaveItemDataPacket::decode)
                .encoder(SaveItemDataPacket::encode)
                .consumerMainThread(SaveItemDataPacket::handle)
                .add();
    }
    
    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
    
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}