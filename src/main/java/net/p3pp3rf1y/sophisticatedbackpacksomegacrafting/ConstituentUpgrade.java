package net.p3pp3rf1y.sophisticatedbackpacksomegacrafting;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;

import java.util.Arrays;
import java.util.List;

public enum ConstituentUpgrade {
    STONECUTTER(0, ModItems.STONECUTTER_UPGRADE),
    CRAFTING(1, ModItems.CRAFTING_UPGRADE),
    SMITHING(2, ModItems.SMITHING_UPGRADE),
    ANVIL(3, ModItems.ANVIL_UPGRADE),
    ADVANCED_JUKEBOX(4, ModItems.ADVANCED_JUKEBOX_UPGRADE),
    AUTO_BLASTING(5, ModItems.AUTO_BLASTING_UPGRADE),
    AUTO_SMELTING(6, ModItems.AUTO_SMELTING_UPGRADE),
    AUTO_SMOKING(7, ModItems.AUTO_SMOKING_UPGRADE);

    public static final int COUNT = 8;
    public static final String NBT_ACTIVE_UI_INDEX = "activeUiIndex";
    public static final String NBT_SUB_UPGRADES = "subUpgrades";

    private final int index;
    private final DeferredHolder<Item, ? extends Item> itemHolder;

    ConstituentUpgrade(int index, DeferredHolder<Item, ? extends Item> itemHolder) {
        this.index = index;
        this.itemHolder = itemHolder;
    }

    public int index() {
        return index;
    }

    public Item getItem() {
        return itemHolder.get();
    }

    public ItemStack createDefaultStack() {
        return new ItemStack(getItem());
    }

    public static ConstituentUpgrade byIndex(int index) {
        if (index < 0 || index >= COUNT) {
            throw new IllegalArgumentException("Invalid constituent upgrade index: " + index);
        }
        return values()[index];
    }

    public static int indexOf(Item item) {
        for (ConstituentUpgrade upgrade : values()) {
            if (upgrade.getItem() == item) {
                return upgrade.index();
            }
        }
        return -1;
    }

    public static List<ItemStack> createDisplayStacks() {
        return Arrays.stream(values()).map(ConstituentUpgrade::createDefaultStack).toList();
    }

    public static List<IUpgradeItem.UpgradeConflictDefinition> getConflictDefinitions() {
        return Arrays.stream(values())
                .map(upgrade -> new IUpgradeItem.UpgradeConflictDefinition(
                        item -> item == upgrade.getItem(),
                        0,
                        Component.translatable("tooltip." + OmegaCraftingMod.MOD_ID + ".conflicts_with", upgrade.createDefaultStack().getHoverName())
                ))
                .toList();
    }
}
