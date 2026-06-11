package net.p3pp3rf1y.sophisticatedbackpacksomegacrafting;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;

@EventBusSubscriber(modid = OmegaCraftingMod.MOD_ID)
public class OmegaBackpackLifecycleHandler {

    private OmegaBackpackLifecycleHandler() {}

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        persistOmegaUpgradesInBackpack(event.getFrom());
        persistOmegaUpgradesInBackpack(event.getTo());
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack placed = player.getMainHandItem();
        if (placed.getItem() instanceof BackpackItem) {
            persistOmegaUpgradesInBackpack(placed);
        }
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof BackpackItem) {
            persistOmegaUpgradesInBackpack(offhand);
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof BackpackBlockEntity backpackBlockEntity) {
            persistOmegaUpgradesInWrapper(backpackBlockEntity.getBackpackWrapper());
        }
    }

    private static void persistOmegaUpgradesInWrapper(IBackpackWrapper wrapper) {
        UpgradeHandler upgradeHandler = wrapper.getUpgradeHandler();
        for (IUpgradeWrapper upgradeWrapper : upgradeHandler.getSlotWrappers().values()) {
            if (upgradeWrapper instanceof CraftingUpgradeOmegaTierWrapper omegaWrapper) {
                omegaWrapper.persistNow();
            }
        }
        upgradeHandler.saveInventory();
    }

    private static void persistOmegaUpgradesInBackpack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BackpackItem)) {
            return;
        }
        persistOmegaUpgradesInWrapper(BackpackWrapper.fromStack(stack));
    }
}
