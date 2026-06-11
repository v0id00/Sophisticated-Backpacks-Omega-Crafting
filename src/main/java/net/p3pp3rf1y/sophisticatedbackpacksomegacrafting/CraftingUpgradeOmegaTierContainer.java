package net.p3pp3rf1y.sophisticatedbackpacksomegacrafting;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerRegistry;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerType;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.NBTHelper;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CraftingUpgradeOmegaTierContainer extends UpgradeContainerBase<CraftingUpgradeOmegaTierWrapper, CraftingUpgradeOmegaTierContainer> {

    private static final String DATA_SET_ACTIVE_UI_INDEX = "setActiveUiIndex";
    private static final String DATA_PERSIST = "persist";

    private final Map<Integer, UpgradeContainerBase<?, ?>> subContainers = new HashMap<>();

    public CraftingUpgradeOmegaTierContainer(Player player, int upgradeContainerId,
                                              CraftingUpgradeOmegaTierWrapper upgradeWrapper,
                                              UpgradeContainerType<CraftingUpgradeOmegaTierWrapper, CraftingUpgradeOmegaTierContainer> type) {
        super(player, upgradeContainerId, upgradeWrapper, type);
        for (int i = 0; i < ConstituentUpgrade.COUNT; i++) {
            final int index = i;
            IUpgradeWrapper virtualWrapper = upgradeWrapper.getOrCreateVirtualWrapper(index);
            UpgradeContainerRegistry.instantiateContainer(player, upgradeContainerId, virtualWrapper)
                    .ifPresent(subContainer -> {
                        subContainers.put(index, subContainer);
                        slots.addAll(subContainer.getSlots());
                    });
        }
    }

    public Map<Integer, UpgradeContainerBase<?, ?>> getSubContainers() {
        return subContainers;
    }

    public UpgradeContainerBase<?, ?> getSubContainer(int index) {
        return subContainers.get(index);
    }

    public int getActiveUiIndex() {
        return getUpgradeWrapper().getActiveUiIndex();
    }

    public void requestSetActiveUiIndex(int index) {
        sendDataToServer(() -> NBTHelper.putInt(new CompoundTag(), DATA_SET_ACTIVE_UI_INDEX, index));
    }

    public void requestPersist() {
        sendDataToServer(() -> NBTHelper.putBoolean(new CompoundTag(), DATA_PERSIST, true));
    }

    public void applyClientSubUpgradeSync(CompoundTag data) {
        CraftingUpgradeOmegaTierWrapper wrapper = getUpgradeWrapper();
        ItemStack stack = wrapper.getUpgradeStack();
        if (data.contains(ConstituentUpgrade.NBT_SUB_UPGRADES)) {
            stack.set(ModDataComponents.SUB_UPGRADES.get(),
                    CraftingUpgradeOmegaTierWrapper.subUpgradesFromSyncTag(data));
        }
        if (data.contains("revision")) {
            stack.set(ModDataComponents.SUB_UPGRADES_REVISION.get(), data.getInt("revision"));
        }
        wrapper.syncVirtualWrapperStacksFromHandlerStack();
    }

    @Override
    public void onInit() {
        super.onInit();
        subContainers.values().forEach(UpgradeContainerBase::onInit);
    }

    @Override
    public void handlePacket(CompoundTag data) {
        if (data.contains(DATA_PERSIST)) {
            getUpgradeWrapper().persistNow();
            return;
        }
        if (data.contains(DATA_SET_ACTIVE_UI_INDEX)) {
            getUpgradeWrapper().setActiveUiIndex(data.getInt(DATA_SET_ACTIVE_UI_INDEX));
            return;
        }
        int active = getActiveUiIndex();
        if (active >= 0) {
            UpgradeContainerBase<?, ?> subContainer = subContainers.get(active);
            if (subContainer != null) {
                subContainer.handlePacket(data);
                return;
            }
        }
        Integer fallbackIndex = resolveSubContainerIndexForPacket(data);
        if (fallbackIndex != null) {
            UpgradeContainerBase<?, ?> subContainer = subContainers.get(fallbackIndex);
            if (subContainer != null) {
                subContainer.handlePacket(data);
            }
        }
    }

    @Nullable
    private static Integer resolveSubContainerIndexForPacket(CompoundTag data) {
        if (data.contains("action") || data.contains("shuffle") || data.contains("repeat")) {
            return ConstituentUpgrade.ADVANCED_JUKEBOX.index();
        }
        return null;
    }

    @Override
    public boolean containsSlot(Slot slot) {
        return findSubContainerForSlot(slot).isPresent();
    }

    @Override
    public ItemStack getSlotStackToTransfer(Slot slot) {
        return findSubContainerForSlot(slot)
                .map(sub -> sub.getSlotStackToTransfer(slot))
                .orElse(super.getSlotStackToTransfer(slot));
    }

    @Override
    public void onTakeFromSlot(Slot slot, Player slotPlayer, ItemStack slotStack) {
        findSubContainerForSlot(slot).ifPresentOrElse(
                sub -> sub.onTakeFromSlot(slot, slotPlayer, slotStack),
                () -> super.onTakeFromSlot(slot, slotPlayer, slotStack)
        );
    }

    @Override
    public boolean mergeIntoStorageFirst(Slot slot) {
        return findSubContainerForSlot(slot)
                .map(sub -> sub.mergeIntoStorageFirst(slot))
                .orElse(super.mergeIntoStorageFirst(slot));
    }

    @Override
    public boolean allowsPickupAll(Slot slot) {
        return findSubContainerForSlot(slot)
                .map(sub -> sub.allowsPickupAll(slot))
                .orElse(super.allowsPickupAll(slot));
    }

    @Override
    public int getRepeatedQuickMoveLimit(Slot slot, ItemStack transferredStack) {
        return findSubContainerForSlot(slot)
                .map(sub -> sub.getRepeatedQuickMoveLimit(slot, transferredStack))
                .orElse(super.getRepeatedQuickMoveLimit(slot, transferredStack));
    }

    private Optional<UpgradeContainerBase<?, ?>> findSubContainerForSlot(Slot slot) {
        for (UpgradeContainerBase<?, ?> subContainer : subContainers.values()) {
            if (subContainer.containsSlot(slot)) {
                return Optional.of(subContainer);
            }
        }
        return Optional.empty();
    }
}
