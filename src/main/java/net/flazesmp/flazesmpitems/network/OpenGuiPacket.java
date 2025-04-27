package net.flazesmp.flazesmpitems.network;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.flazesmp.flazesmpitems.gui.ItemBrowserScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenGuiPacket {
    
    public OpenGuiPacket() {
        // Empty constructor needed for packet registration
    }
    
    public void encode(FriendlyByteBuf buffer) {
        // No data to encode
    }
    
    public static OpenGuiPacket decode(FriendlyByteBuf buffer) {
        return new OpenGuiPacket();
    }
    
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft.getInstance().setScreen(new ItemBrowserScreen());
            });
        });
        
        ctx.get().setPacketHandled(true);
    }
}