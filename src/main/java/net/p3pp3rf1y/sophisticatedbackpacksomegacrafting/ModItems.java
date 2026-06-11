package net.p3pp3rf1y.sophisticatedbackpacksomegacrafting;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;

public class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, OmegaCraftingMod.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB.location(), OmegaCraftingMod.MOD_ID);

    public static final DeferredHolder<Item, CraftingUpgradeOmegaTierItem> CRAFTING_UPGRADE_OMEGA_TIER =
            ITEMS.register("crafting_upgrade_omega_tier", CraftingUpgradeOmegaTierItem::new);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = CREATIVE_MODE_TABS.register("main", () ->
            CreativeModeTab.builder()
                    .icon(() -> new ItemStack(CRAFTING_UPGRADE_OMEGA_TIER.get()))
                    .title(Component.translatable("itemGroup." + OmegaCraftingMod.MOD_ID))
                    .displayItems((featureFlags, output) -> output.accept(CRAFTING_UPGRADE_OMEGA_TIER.get()))
                    .build());

    public static final UpgradeContainerType<CraftingUpgradeOmegaTierWrapper, CraftingUpgradeOmegaTierContainer> OMEGA_CRAFTING_TYPE =
            new UpgradeContainerType<>(CraftingUpgradeOmegaTierContainer::new);

    public static void registerContainers(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.MENU)) {
            return;
        }
        UpgradeContainerRegistry.register(CRAFTING_UPGRADE_OMEGA_TIER.getId(), OMEGA_CRAFTING_TYPE);
    }
}
