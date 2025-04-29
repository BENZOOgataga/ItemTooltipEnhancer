package net.flazesmp.flazesmpitems.clearlag;

import net.flazesmp.flazesmpitems.FlazeSMPItems;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = FlazeSMPItems.MOD_ID)
public class ClearlagManager {
    // Track the next scheduled automatic clearlag time
    private static long nextClearlagTime = -1;
    
    // Keep track of previous warning times to avoid duplicate warnings
    private static int lastWarningSeconds = -1;
    
    // Track if a manual clearlag is currently running
    private static boolean manualClearlagRunning = false;
    private static long manualClearlagEndTime = -1;
    
    // For scheduled manual clearlag
    private static long manualClearlagScheduledTime = -1;
    
    // Manual clearlag delay in seconds (1 minute)
    private static final int MANUAL_CLEARLAG_DELAY_SECONDS = 60;
    
    // Countdown from the last manual clearlag command (to prevent spam)
    private static final int CLEARLAG_COOLDOWN_SECONDS = 5;
    
    /**
     * Schedule the next automatic clearlag
     */
    public static void scheduleNextClearlag() {
        if (!ClearlagConfig.SERVER.enableAutoClearlag.get()) {
            nextClearlagTime = -1; // Disabled
            return;
        }
        
        int intervalMinutes = ClearlagConfig.SERVER.clearlagIntervalMinutes.get();
        nextClearlagTime = System.currentTimeMillis() + (intervalMinutes * 60 * 1000L);
        FlazeSMPItems.LOGGER.info("Next clearlag scheduled in {} minutes", intervalMinutes);
    }
    
    /**
     * Reschedule clearlag with new settings (called after config reload)
     */
    public static void rescheduleWithNewSettings() {
        scheduleNextClearlag();
    }
    
    /**
     * Check if enough time has elapsed to trigger clearlag warnings or execution
     * 
     * @param event Server tick event
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        // Only check once every second (20 ticks)
        if (ServerLifecycleHooks.getCurrentServer().getTickCount() % 20 != 0) return;
        
        // Handle scheduled manual clearlag first
        if (manualClearlagScheduledTime > 0) {
            long currentTime = System.currentTimeMillis();
            long remainingMs = manualClearlagScheduledTime - currentTime;
            
            // Check if it's time to perform the manual clearlag
            if (remainingMs <= 0) {
                executeClearlag(ServerLifecycleHooks.getCurrentServer());
                manualClearlagScheduledTime = -1;
                return;
            }
            
            // Convert to seconds for warning messages
            int remainingSeconds = (int)(remainingMs / 1000);
            
            // Check if we should send a warning
            checkAndSendWarnings(remainingSeconds, true);
        }
        
        // Handle automatic clearlag
        if (nextClearlagTime <= 0) return;
        
        // Calculate remaining time
        long currentTime = System.currentTimeMillis();
        long remainingMs = nextClearlagTime - currentTime;
        
        // Check if it's time to perform clearlag
        if (remainingMs <= 0) {
            executeClearlag(ServerLifecycleHooks.getCurrentServer());
            scheduleNextClearlag(); // Schedule the next one
            return;
        }
        
        // Convert to seconds for warning messages
        int remainingSeconds = (int)(remainingMs / 1000);
        
        // Check if we should send a warning
        checkAndSendWarnings(remainingSeconds, false);
    }
    
    /**
     * Handle player login to send notification preferences
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Check if automatic clearlag is enabled
            if (ClearlagConfig.SERVER.enableAutoClearlag.get() && nextClearlagTime > 0) {
                // Calculate remaining time
                long remainingMs = nextClearlagTime - System.currentTimeMillis();
                if (remainingMs > 0) {
                    // Convert to minutes and seconds
                    int remainingSeconds = (int)(remainingMs / 1000);
                    
                    // Send initial message about next clearlag
                    String timeRemaining = ClearlagConfig.formatTimeRemaining(remainingSeconds);
                    
                    // Send based on their preference
                    NotificationType type = ClearlagConfig.getPlayerNotificationType(player.getUUID());
                    if (type == NotificationType.CHAT) {
                        player.sendSystemMessage(Component.literal("Next automatic clearlag in " + timeRemaining)
                                .withStyle(ChatFormatting.YELLOW));
                    } else if (type == NotificationType.HOTBAR) {
                        displayHotbarMessage(player, "Next clearlag in " + timeRemaining, 
                                ClearlagConfig.SERVER.notificationDisplayDurationSeconds.get());
                    }
                }
            }
            
            // Check if a manual clearlag is scheduled
            if (manualClearlagScheduledTime > 0) {
                long remainingMs = manualClearlagScheduledTime - System.currentTimeMillis();
                if (remainingMs > 0) {
                    int remainingSeconds = (int)(remainingMs / 1000);
                    String timeRemaining = ClearlagConfig.formatTimeRemaining(remainingSeconds);
                    
                    NotificationType type = ClearlagConfig.getPlayerNotificationType(player.getUUID());
                    if (type == NotificationType.CHAT) {
                        player.sendSystemMessage(Component.literal("Manual clearlag scheduled in " + timeRemaining)
                                .withStyle(ChatFormatting.YELLOW));
                    } else if (type == NotificationType.HOTBAR) {
                        displayHotbarMessage(player, "Manual clearlag in " + timeRemaining,
                                ClearlagConfig.SERVER.notificationDisplayDurationSeconds.get());
                    }
                }
            }
        }
    }
    
    /**
     * Handle player logout to ensure we keep their preferences saved
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerUuid = player.getUUID();
            NotificationType type = ClearlagConfig.getPlayerNotificationType(playerUuid);
            
            // Log the player logout with their notification preference
            FlazeSMPItems.LOGGER.debug("Player {} logged out with notification preference: {}", 
                player.getName().getString(), type);
            
            // Their preference is already saved to disk through ClearlagConfig.setPlayerNotificationType
        }
    }
    
    /**
     * Check and send warning messages if needed
     * 
     * @param remainingSeconds Time in seconds until clearlag
     * @param isManual Whether this is for a manual clearlag
     */
    private static void checkAndSendWarnings(int remainingSeconds, boolean isManual) {
        // Get sorted warning times
        List<Integer> warningTimes = ClearlagConfig.getSortedWarningTimes();
        
        // Find the next warning time
        for (int warningTime : warningTimes) {
            // If we're at or just past a warning time (within 0.5 seconds to avoid timing issues)
            if (remainingSeconds <= warningTime && 
                (lastWarningSeconds == -1 || lastWarningSeconds > warningTime)) {
                
                // Send warning to all players
                sendClearlagWarning(warningTime, remainingSeconds, isManual);
                
                // Update last warning time
                lastWarningSeconds = warningTime;
                break;
            }
        }
    }
    
    /**
     * Send a clearlag warning to all players
     * 
     * @param warningTime The warning threshold that triggered this message
     * @param actualSeconds The actual seconds remaining
     * @param isManual Whether this is for a manual clearlag
     */
    private static void sendClearlagWarning(int warningTime, int actualSeconds, boolean isManual) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        
        PlayerList playerList = server.getPlayerList();
        if (playerList == null) return;
        
        String formattedTime = ClearlagConfig.formatTimeRemaining(actualSeconds);
        String message = "Items will be cleared in " + formattedTime;
        if (isManual) {
            message = "Manual clearlag: " + message;
        }
        
        // Send message to each player based on their preference
        for (ServerPlayer player : playerList.getPlayers()) {
            NotificationType type = ClearlagConfig.getPlayerNotificationType(player.getUUID());
            
            switch (type) {
                case CHAT:
                    player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.YELLOW));
                    break;
                case HOTBAR:
                    // Use the display duration from config
                    displayHotbarMessage(player, message, ClearlagConfig.SERVER.notificationDisplayDurationSeconds.get());
                    break;
                case NONE:
                    // Do nothing
                    break;
            }
        }
        
        // Also log to console
        FlazeSMPItems.LOGGER.info("Clearlag warning: {}", message);
    }
    
    /**
     * Send a hotbar message to a player
     * 
     * @param player The player to send the message to
     * @param message The message to display
     * @param durationSeconds How long the message should stay visible
     */
    public static void displayHotbarMessage(ServerPlayer player, String message, int durationSeconds) {
        player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.YELLOW), true);
    }
    
    /**
     * Execute the clearlag operation
     * 
     * @param server The Minecraft server instance
     */
    public static void executeClearlag(MinecraftServer server) {
        if (server == null) return;
        
        int totalCleared = 0;
        
        // Get entity types to clear from config
        List<? extends String> entityTypesToClear = ClearlagConfig.SERVER.entityTypesToClear.get();
        
        // Clear items in each dimension
        for (ServerLevel level : server.getAllLevels()) {
            // Create a list to store entities to remove
            List<Entity> toRemove = new ArrayList<>();
            
            // Iterate through all entities in the level
            for (Entity entity : level.getAllEntities()) {
                // Get the registry name for this entity type
                ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                
                if (entityId != null && entityTypesToClear.contains(entityId.toString())) {
                    toRemove.add(entity);
                }
            }
            
            // Remove the collected entities
            for (Entity entity : toRemove) {
                entity.remove(Entity.RemovalReason.DISCARDED);
            }
            
            totalCleared += toRemove.size();
        }
        
        // Create notification message
        String message = "Cleared " + totalCleared + " item" + (totalCleared == 1 ? "" : "s");
        
        // Notify all players
        sendCompletionMessage(server.getPlayerList(), message);
        
        // Log to console
        FlazeSMPItems.LOGGER.info(message);
        
        // Reset last warning time
        lastWarningSeconds = -1;
        
        // If this was a manual clearlag, mark it as complete
        if (manualClearlagRunning) {
            manualClearlagRunning = false;
        }
    }
    
    /**
     * Check if a manual clearlag can be executed
     * 
     * @return true if allowed, false if another clearlag is already in progress
     */
    public static boolean canExecuteManualClearlag() {
        // Check if another manual clearlag is already in progress
        if (manualClearlagRunning || manualClearlagScheduledTime > 0) {
            long currentTime = System.currentTimeMillis();
            if (manualClearlagScheduledTime > currentTime) {
                // Another clearlag operation is currently scheduled
                return false;
            } else if (manualClearlagRunning && currentTime < manualClearlagEndTime) {
                // Another clearlag operation is currently in progress
                return false;
            } else {
                // The previous operation has timed out, so we can proceed
                manualClearlagRunning = false;
                manualClearlagScheduledTime = -1;
            }
        }
        return true;
    }
    
    /**
     * Mark that a manual clearlag has been initiated
     */
    public static void setManualClearlagActive() {
        manualClearlagRunning = true;
        // Set an expiration time (in case the clearlag operation somehow fails to complete)
        manualClearlagEndTime = System.currentTimeMillis() + (CLEARLAG_COOLDOWN_SECONDS * 1000L);
    }
    
    /**
     * Schedule a manual clearlag to occur after the specified delay
     */
    public static void startManualClearlag() {
        // Check if we can perform a manual clearlag
        if (!canExecuteManualClearlag()) {
            return;
        }
        
        // Schedule the clearlag to happen after the delay
        manualClearlagScheduledTime = System.currentTimeMillis() + (MANUAL_CLEARLAG_DELAY_SECONDS * 1000L);
        
        // Reset the warning timer to ensure warnings are sent
        lastWarningSeconds = -1;
        
        // Mark that a manual clearlag is in progress
        setManualClearlagActive();
        
        // Get the server instance
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            // Announce that a manual clearlag has been scheduled
            PlayerList playerList = server.getPlayerList();
            String timeRemaining = ClearlagConfig.formatTimeRemaining(MANUAL_CLEARLAG_DELAY_SECONDS);
            String message = "Manual clearlag scheduled in " + timeRemaining;
            sendNotificationToAllPlayers(playerList, Component.literal(message), false);
        }
    }
    
    /**
     * Get the time until the next clearlag (automatic or manual)
     * 
     * @return Remaining time in seconds until next clearlag, -1 if no clearlag is scheduled
     */
    public static int getTimeUntilNextClearlag() {
        long currentTime = System.currentTimeMillis();
        
        // Check manual clearlag first as it takes precedence
        if (manualClearlagScheduledTime > 0) {
            return (int)((manualClearlagScheduledTime - currentTime) / 1000);
        }
        
        // Check automatic clearlag
        if (nextClearlagTime > 0) {
            return (int)((nextClearlagTime - currentTime) / 1000);
        }
        
        // No clearlag scheduled
        return -1;
    }
    
    /**
     * Sends a notification to a specific player according to their preference
     * 
     * @param player The player to notify
     * @param message The notification message
     * @param isCompletion Whether this is a completion message (affects color)
     */
    public static void sendNotificationToPlayer(ServerPlayer player, Component message, boolean isCompletion) {
        if (player == null) return;
        
        NotificationType type = ClearlagConfig.getPlayerNotificationType(player.getUUID());
        
        switch (type) {
            case CHAT:
                // Send as chat message with appropriate color
                player.sendSystemMessage(message.copy().withStyle(
                    isCompletion ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
                break;
            case HOTBAR:
                // Display on hotbar with appropriate color
                player.displayClientMessage(message.copy().withStyle(
                    isCompletion ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
                break;
            case NONE:
                // No notification
                break;
        }
    }
    
    /**
     * Notifies all players that a clearlag operation completed
     * 
     * @param playerList The server's player list
     * @param message The message to send
     */
    private static void sendCompletionMessage(PlayerList playerList, String message) {
        if (playerList == null) return;
        
        Component messageComponent = Component.literal(message);
        sendNotificationToAllPlayers(playerList, messageComponent, true);
    }
    
    /**
     * Sends a notification to all players
     * 
     * @param playerList The server's player list
     * @param message The message to send
     * @param isCompletion Whether this is a completion message (affects color)
     */
    private static void sendNotificationToAllPlayers(PlayerList playerList, Component message, boolean isCompletion) {
        if (playerList == null) return;
        
        // Send message to each player based on their preference
        for (ServerPlayer player : playerList.getPlayers()) {
            sendNotificationToPlayer(player, message, isCompletion);
        }
    }
}