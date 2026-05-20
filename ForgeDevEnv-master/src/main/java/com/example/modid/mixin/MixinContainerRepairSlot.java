package com.example.modid.mixin;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ContainerRepair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// We target the Player directly now!
@Mixin(EntityPlayer.class)
public abstract class MixinContainerRepairSlot {

    // We intercept the exact moment the game tries to change the player's levels.
    // "require = 1" means if this fails to apply, the game will actually crash and warn us instead of failing silently!
    @Inject(method = "addExperienceLevel(I)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void interceptAnvilXpCost(int levels, CallbackInfo ci) {
        EntityPlayer player = (EntityPlayer) (Object) this;

        // Check if levels are being DEDUCTED (negative) AND the player has an Anvil open
        if (levels < 0 && player.openContainer instanceof ContainerRepair) {
            int costInLevels = -levels; 
            
            // Calculate raw points
            int rawXpCost = getRawXpForLevel(costInLevels);

            // Deduct raw points safely
            removeRawExperiencePoints(player, rawXpCost);
            
            // CRITICAL: Cancel the vanilla method so it doesn't ALSO steal your levels!
            ci.cancel(); 
        }
    }

    // 1.12.2 Vanilla Math to convert a level requirement into raw points
    private int getRawXpForLevel(int level) {
        if (level >= 30) {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        } else if (level >= 15) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return level * level + 6 * level;
        }
    }

    // A helper method to calculate the player's total current raw XP points
    private int getPlayerTotalXp(EntityPlayer player) {
        int level = player.experienceLevel;
        int total = getRawXpForLevel(level);
        return total + Math.round(player.experience * player.xpBarCap());
    }

    // Safely deduct the points and recalculate the player's bar
    private void removeRawExperiencePoints(EntityPlayer player, int amount) {
        int currentXp = getPlayerTotalXp(player);
        int newXp = Math.max(0, currentXp - amount);

        // Wipe the player's bar temporarily
        player.experienceLevel = 0;
        player.experience = 0.0F;
        player.experienceTotal = 0;

        // Re-add the remaining XP points to let Minecraft naturally calculate the correct levels
        player.addExperience(newXp);
    }
}