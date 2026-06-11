package net.p3pp3rf1y.sophisticatedbackpacksomegacrafting;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.p3pp3rf1y.sophisticatedbackpacks.Config;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TranslationHelper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeSlotChangeResult;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeItemBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeType;

import java.util.Collections;
import java.util.List;

public class CraftingUpgradeOmegaTierItem extends UpgradeItemBase<CraftingUpgradeOmegaTierWrapper> {
    private static final UpgradeType<CraftingUpgradeOmegaTierWrapper> TYPE = new UpgradeType<>(CraftingUpgradeOmegaTierWrapper::new);

    public CraftingUpgradeOmegaTierItem() {
        super(Config.SERVER.maxUpgradesPerStorage);
    }

    @Override
    public UpgradeType<CraftingUpgradeOmegaTierWrapper> getType() {
        return TYPE;
    }

    @Override
    public List<UpgradeConflictDefinition> getUpgradeConflicts() {
        return ConstituentUpgrade.getConflictDefinitions();
    }

    @Override
    public UpgradeSlotChangeResult canRemoveUpgradeFrom(IStorageWrapper storageWrapper, boolean isClientSide, Player player) {
        for (IUpgradeWrapper wrapper : storageWrapper.getUpgradeHandler().getSlotWrappers().values()) {
            if (wrapper instanceof CraftingUpgradeOmegaTierWrapper omegaWrapper && omegaWrapper.hasAnyStoredItems()) {
                return UpgradeSlotChangeResult.fail(
                        TranslationHelper.INSTANCE.translError("remove.banned_item"),
                        Collections.emptySet(),
                        Collections.emptySet(),
                        Collections.emptySet()
                );
            }
        }
        return super.canRemoveUpgradeFrom(storageWrapper, isClientSide, player);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip." + OmegaCraftingMod.MOD_ID + ".crafting_upgrade_omega_tier"));
    }
}
