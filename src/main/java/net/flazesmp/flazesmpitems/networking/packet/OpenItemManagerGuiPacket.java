package net.flazesmp.flazesmpitems.networking.packet;

import java.util.function.Supplier;

import net.flazesmp.flazesmpitems.gui.ItemManagerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class OpenItemManagerGuiPacket {
    
    public OpenItemManagerGuiPacket() {
    }
    
    public static void encode(OpenItemManagerGuiPacket message, FriendlyByteBuf buffer) {
        // Nothing to encode
    }
    
    public static OpenItemManagerGuiPacket decode(FriendlyByteBuf buffer) {
        return new OpenItemManagerGuiPacket();
    }
    
    public static void handle(OpenItemManagerGuiPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Make sure we're on the client side
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ItemManagerScreen.open();
            });
        });
        context.setPacketHandled(true);
    }
}