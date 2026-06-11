package net.p3pp3rf1y.sophisticatedbackpacksomegacrafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.p3pp3rf1y.sophisticatedbackpacksomegacrafting.network.SyncOmegaSubUpgradesPayload;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.anvil.AnvilUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.upgrades.smithing.SmithingUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeAccessModifier;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeItem;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapperAccessor;
import net.p3pp3rf1y.sophisticatedcore.upgrades.ITickableUpgrade;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeWrapperBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.blockconverter.BlockConverterUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.cooking.AutoCookingUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.jukebox.JukeboxUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.util.InventoryHelper;
import net.p3pp3rf1y.sophisticatedcore.util.RegistryHelper;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Virtual sub-upgrades store their state on {@link ItemStack} components inside {@link ModDataComponents#SUB_UPGRADES},
 * the same way real upgrades keep inventory on {@code LENIENT_CONTAINER} on the upgrade stack itself.
 */
public class CraftingUpgradeOmegaTierWrapper extends UpgradeWrapperBase<CraftingUpgradeOmegaTierWrapper, CraftingUpgradeOmegaTierItem>
        implements ITickableUpgrade, IUpgradeAccessModifier {

    @Nullable
    private final IUpgradeWrapper[] virtualWrapperCache = new IUpgradeWrapper[ConstituentUpgrade.COUNT];
    @Nullable
    private SubUpgradeContents cachedSubUpgrades;
    private int cachedSubUpgradesRevision = -1;
    private boolean virtualWrappersOnAdded;
    private boolean migrated;
    private boolean legacyMigrated;
    private boolean flushingSubUpgrades;
    private boolean inSave;
    private boolean inBeforeRemoved;
    private long lastSubUpgradeSyncGameTime = -1;

    protected CraftingUpgradeOmegaTierWrapper(IStorageWrapper storageWrapper, ItemStack upgrade, Consumer<ItemStack> upgradeSaveHandler) {
        super(storageWrapper, upgrade, upgradeSaveHandler);
    }

    public static List<ItemStack> getConstituentUpgradeStacks() {
        return ConstituentUpgrade.createDisplayStacks();
    }

    private static boolean isServerSide() {
        return Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER;
    }

    private ItemStack stackForComponentAccess() {
        UpgradeHandler handler = storageWrapper.getUpgradeHandler();
        if (handler != null) {
            int omegaSlot = findOmegaUpgradeSlot(handler);
            if (omegaSlot >= 0) {
                return handler.getStackInSlot(omegaSlot);
            }
            for (var entry : handler.getSlotWrappers().entrySet()) {
                if (entry.getValue() == this) {
                    return handler.getStackInSlot(entry.getKey());
                }
            }
        }
        return upgrade;
    }

    private static int findOmegaUpgradeSlot(UpgradeHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.getStackInSlot(slot).getItem() instanceof CraftingUpgradeOmegaTierItem) {
                return slot;
            }
        }
        return -1;
    }

    private void ensureSubUpgradesInitialized() {
        migrateLegacyCustomDataIfNeeded();
        ItemStack stack = stackForComponentAccess();
        if (isServerSide() && !hasCachedVirtualWrappers()) {
            applyCustomDataBackupToComponents(stack);
        }
        if (!stack.has(ModDataComponents.SUB_UPGRADES.get())) {
            if (isServerSide()) {
                stack.set(ModDataComponents.SUB_UPGRADES.get(), SubUpgradeContents.createDefault());
                stack.set(ModDataComponents.SUB_UPGRADES_REVISION.get(), 0);
                save();
            }
            return;
        }
        SubUpgradeContents current = stack.get(ModDataComponents.SUB_UPGRADES.get());
        int revision = stack.getOrDefault(ModDataComponents.SUB_UPGRADES_REVISION.get(), 0);
        if (cachedSubUpgrades != current) {
            if (cachedSubUpgrades != null && hasCachedVirtualWrappers()) {
                syncExistingVirtualWrapperStacks(current);
            } else {
                Arrays.fill(virtualWrapperCache, null);
            }
            cachedSubUpgrades = current;
            cachedSubUpgradesRevision = revision;
        } else if (cachedSubUpgradesRevision != revision && hasCachedVirtualWrappers()) {
            syncExistingVirtualWrapperStacks(current);
            cachedSubUpgradesRevision = revision;
        }
    }

    private boolean hasCachedVirtualWrappers() {
        for (IUpgradeWrapper wrapper : virtualWrapperCache) {
            if (wrapper != null) {
                return true;
            }
        }
        return false;
    }

    private void syncExistingVirtualWrapperStacks(SubUpgradeContents live) {
        for (int i = 0; i < ConstituentUpgrade.COUNT; i++) {
            if (virtualWrapperCache[i] != null) {
                virtualWrapperCache[i].getUpgradeStack().applyComponents(live.get(i).getComponentsPatch());
            }
        }
    }

    public void syncVirtualWrapperStacksFromHandlerStack() {
        if (isServerSide() && !hasCachedVirtualWrappers()) {
            ensureSubUpgradesInitialized();
            return;
        }
        ItemStack stack = stackForComponentAccess();
        if (isServerSide()) {
            migrateLegacyCustomDataIfNeeded();
            if (!hasCachedVirtualWrappers()) {
                applyCustomDataBackupToComponents(stack);
            }
        }
        if (!stack.has(ModDataComponents.SUB_UPGRADES.get())) {
            if (isServerSide()) {
                ensureSubUpgradesInitialized();
            }
            return;
        }
        SubUpgradeContents current = stack.get(ModDataComponents.SUB_UPGRADES.get());
        if (hasCachedVirtualWrappers()) {
            syncExistingVirtualWrapperStacks(current);
        }
        cachedSubUpgrades = current;
        cachedSubUpgradesRevision = stack.getOrDefault(ModDataComponents.SUB_UPGRADES_REVISION.get(), 0);
    }

    public void persistNow() {
        if (isServerSide()) {
            save();
        }
    }

    private void flushSubUpgradesToLiveStack() {
        if (flushingSubUpgrades) {
            return;
        }
        flushingSubUpgrades = true;
        try {
            migrateLegacyCustomDataIfNeeded();
            ItemStack live = stackForComponentAccess();
            SubUpgradeContents snapshot = buildSubUpgradeSnapshot(live);
            live.set(ModDataComponents.SUB_UPGRADES.get(), snapshot);
            int revision = live.getOrDefault(ModDataComponents.SUB_UPGRADES_REVISION.get(), 0) + 1;
            live.set(ModDataComponents.SUB_UPGRADES_REVISION.get(), revision);
            cachedSubUpgrades = snapshot;
            cachedSubUpgradesRevision = revision;
            if (hasCachedVirtualWrappers()) {
                syncExistingVirtualWrapperStacks(snapshot);
            }
            writeCustomDataBackup(live, snapshot);
        } finally {
            flushingSubUpgrades = false;
        }
    }

    private SubUpgradeContents buildSubUpgradeSnapshot(ItemStack live) {
        SubUpgradeContents current = live.getOrDefault(
                ModDataComponents.SUB_UPGRADES.get(), SubUpgradeContents.createDefault());
        NonNullList<ItemStack> list = NonNullList.withSize(ConstituentUpgrade.COUNT, ItemStack.EMPTY);
        for (int i = 0; i < ConstituentUpgrade.COUNT; i++) {
            if (virtualWrapperCache[i] != null) {
                list.set(i, virtualWrapperCache[i].getUpgradeStack().copy());
            } else {
                list.set(i, current.get(i).copy());
            }
        }
        return new SubUpgradeContents(list);
    }

    private void applyCustomDataBackupToComponents(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(ConstituentUpgrade.NBT_SUB_UPGRADES, Tag.TAG_LIST)) {
            return;
        }
        stack.set(ModDataComponents.SUB_UPGRADES.get(), loadSubUpgradesFromLegacyTag(tag));
        if (tag.contains(ConstituentUpgrade.NBT_ACTIVE_UI_INDEX)) {
            stack.set(ModDataComponents.ACTIVE_UI_INDEX.get(), tag.getInt(ConstituentUpgrade.NBT_ACTIVE_UI_INDEX));
        }
        if (!stack.has(ModDataComponents.SUB_UPGRADES_REVISION.get())) {
            stack.set(ModDataComponents.SUB_UPGRADES_REVISION.get(), 0);
        }
    }

    private void writeCustomDataBackup(ItemStack live, SubUpgradeContents contents) {
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess().orElse(null);
        if (registryAccess == null) {
            return;
        }
        ListTag list = new ListTag();
        for (int i = 0; i < ConstituentUpgrade.COUNT; i++) {
            list.add(contents.get(i).save(registryAccess));
        }
        CompoundTag tag = new CompoundTag();
        tag.put(ConstituentUpgrade.NBT_SUB_UPGRADES, list);
        if (live.has(ModDataComponents.ACTIVE_UI_INDEX.get())) {
            tag.putInt(ConstituentUpgrade.NBT_ACTIVE_UI_INDEX, live.get(ModDataComponents.ACTIVE_UI_INDEX.get()));
        }
        live.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public ItemStack getUpgradeStack() {
        return stackForComponentAccess();
    }

    @Override
    protected void save() {
        if (!isServerSide()) {
            return;
        }
        if (inSave) {
            return;
        }
        inSave = true;
        try {
            flushSubUpgradesToLiveStack();
            ItemStack live = stackForComponentAccess();
            upgradeSaveHandler.accept(live);
            syncSubUpgradesToOpenViewers(live);
        } finally {
            inSave = false;
        }
    }

    private void syncSubUpgradesToOpenViewers(ItemStack live) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null || !live.has(ModDataComponents.SUB_UPGRADES.get())) {
                return;
            }
            long gameTime = server.overworld().getGameTime();
            if (gameTime == lastSubUpgradeSyncGameTime) {
                return;
            }
            UpgradeHandler handler = storageWrapper.getUpgradeHandler();
            if (handler == null) {
                return;
            }
            int omegaSlot = findOmegaUpgradeSlot(handler);
            if (omegaSlot < 0) {
                return;
            }
            SubUpgradeContents snapshot = live.get(ModDataComponents.SUB_UPGRADES.get());
            int revision = live.getOrDefault(ModDataComponents.SUB_UPGRADES_REVISION.get(), 0);
            CompoundTag syncTag = buildSubUpgradeSyncTag(snapshot, revision);
            boolean sent = false;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!(player.containerMenu instanceof StorageContainerMenuBase<?> menu)) {
                    continue;
                }
                if (menu.getStorageWrapper() != storageWrapper) {
                    continue;
                }
                PacketDistributor.sendToPlayer(player, new SyncOmegaSubUpgradesPayload(
                        menu.containerId, omegaSlot, syncTag));
                sent = true;
            }
            if (sent) {
                lastSubUpgradeSyncGameTime = gameTime;
            }
        } catch (RuntimeException ignored) {
            // Never let client animation sync take down server tick/save.
        }
    }

    public static CompoundTag buildSubUpgradeSyncTag(SubUpgradeContents snapshot, int revision) {
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess().orElse(null);
        CompoundTag tag = new CompoundTag();
        tag.putInt("revision", revision);
        if (registryAccess == null) {
            return tag;
        }
        ListTag list = new ListTag();
        for (int i = 0; i < ConstituentUpgrade.COUNT; i++) {
            list.add(snapshot.get(i).save(registryAccess));
        }
        tag.put(ConstituentUpgrade.NBT_SUB_UPGRADES, list);
        return tag;
    }

    public static SubUpgradeContents subUpgradesFromSyncTag(CompoundTag tag) {
        if (!tag.contains(ConstituentUpgrade.NBT_SUB_UPGRADES, Tag.TAG_LIST)) {
            return SubUpgradeContents.createDefault();
        }
        return loadSubUpgradesFromLegacyTag(tag);
    }

    @Nullable
    private SubUpgradeContents getSubUpgradeContents() {
        ensureSubUpgradesInitialized();
        ItemStack stack = stackForComponentAccess();
        if (!stack.has(ModDataComponents.SUB_UPGRADES.get())) {
            return null;
        }
        return stack.get(ModDataComponents.SUB_UPGRADES.get());
    }

    private void migrateLegacyCustomDataIfNeeded() {
        if (legacyMigrated || !isServerSide()) {
            return;
        }
        legacyMigrated = true;
        ItemStack stack = stackForComponentAccess();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(ConstituentUpgrade.NBT_SUB_UPGRADES, Tag.TAG_LIST) && !tag.contains(ConstituentUpgrade.NBT_ACTIVE_UI_INDEX)) {
            return;
        }
        if (tag.contains(ConstituentUpgrade.NBT_SUB_UPGRADES, Tag.TAG_LIST) && !stack.has(ModDataComponents.SUB_UPGRADES.get())) {
            stack.set(ModDataComponents.SUB_UPGRADES.get(), loadSubUpgradesFromLegacyTag(tag));
            stack.set(ModDataComponents.SUB_UPGRADES_REVISION.get(), 0);
        }
        if (tag.contains(ConstituentUpgrade.NBT_ACTIVE_UI_INDEX) && !stack.has(ModDataComponents.ACTIVE_UI_INDEX.get())) {
            stack.set(ModDataComponents.ACTIVE_UI_INDEX.get(), tag.getInt(ConstituentUpgrade.NBT_ACTIVE_UI_INDEX));
        }
        save();
    }

    private static SubUpgradeContents loadSubUpgradesFromLegacyTag(CompoundTag tag) {
        SubUpgradeContents contents = SubUpgradeContents.createDefault();
        ListTag list = tag.getList(ConstituentUpgrade.NBT_SUB_UPGRADES, Tag.TAG_COMPOUND);
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess().orElse(null);
        if (registryAccess == null) {
            return contents;
        }
        for (int i = 0; i < ConstituentUpgrade.COUNT && i < list.size(); i++) {
            CompoundTag stackTag = list.getCompound(i);
            if (!stackTag.isEmpty()) {
                int index = i;
                ItemStack.parse(registryAccess, stackTag).ifPresent(stack -> contents.set(index, stack));
            }
        }
        return contents;
    }

    public int getActiveUiIndex() {
        ItemStack stack = stackForComponentAccess();
        return stack.getOrDefault(ModDataComponents.ACTIVE_UI_INDEX.get(), -1);
    }

    public void setActiveUiIndex(int index) {
        if (!isServerSide()) {
            return;
        }
        stackForComponentAccess().set(ModDataComponents.ACTIVE_UI_INDEX.get(), index);
        save();
    }

    public ItemStack getVirtualUpgradeStack(int index) {
        SubUpgradeContents contents = getSubUpgradeContents();
        if (contents != null) {
            return contents.get(index);
        }
        return ConstituentUpgrade.byIndex(index).createDefaultStack();
    }

    private void onVirtualUpgradeSaved() {
        if (!isServerSide()) {
            return;
        }
        save();
    }

    public boolean hasAnyStoredItems() {
        ItemStack omegaStack = stackForComponentAccess();
        if (omegaStack.has(ModDataComponents.SUB_UPGRADES.get())) {
            SubUpgradeContents contents = omegaStack.get(ModDataComponents.SUB_UPGRADES.get());
            for (int i = 0; i < ConstituentUpgrade.COUNT; i++) {
                if (stackHasStoredItems(contents.get(i))) {
                    return true;
                }
            }
            return false;
        }
        if (hasStoredItemsInLegacyCustomData(omegaStack)) {
            return true;
        }
        if (!isServerSide()) {
            return false;
        }
        ensureSubUpgradesInitialized();
        for (int i = 0; i < ConstituentUpgrade.COUNT; i++) {
            if (hasStoredItemsInWrapper(getOrCreateVirtualWrapper(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasStoredItemsInLegacyCustomData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(ConstituentUpgrade.NBT_SUB_UPGRADES, Tag.TAG_LIST)) {
            return false;
        }
        RegistryAccess registryAccess = RegistryHelper.getRegistryAccess().orElse(null);
        if (registryAccess == null) {
            return false;
        }
        ListTag list = tag.getList(ConstituentUpgrade.NBT_SUB_UPGRADES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag stackTag = list.getCompound(i);
            if (!stackTag.isEmpty()) {
                ItemStack subStack = ItemStack.parse(registryAccess, stackTag).orElse(ItemStack.EMPTY);
                if (stackHasStoredItems(subStack)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean stackHasStoredItems(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return hasItemsInContainer(stack, ModCoreDataComponents.LENIENT_CONTAINER.get())
                || hasItemsInContainer(stack, ModCoreDataComponents.COOKING_INVENTORY.get())
                || hasItemsInContainer(stack, DataComponents.CONTAINER);
    }

    private static boolean hasItemsInContainer(ItemStack stack, DataComponentType<ItemContainerContents> type) {
        ItemContainerContents contents = stack.get(type);
        return contents != null && contents.stream().anyMatch(item -> !item.isEmpty());
    }

    private static boolean hasStoredItemsInWrapper(IUpgradeWrapper wrapper) {
        IItemHandler inventory = getInventoryHandler(wrapper);
        return inventory != null && !InventoryHelper.isEmpty(inventory);
    }

    @Nullable
    private static IItemHandler getInventoryHandler(IUpgradeWrapper wrapper) {
        if (wrapper instanceof CraftingUpgradeWrapper crafting) {
            return crafting.getInventory();
        }
        if (wrapper instanceof AutoCookingUpgradeWrapper autoCooking) {
            return autoCooking.getCookingLogic().getCookingInventory();
        }
        if (wrapper instanceof JukeboxUpgradeWrapper jukebox) {
            return jukebox.getDiscInventory();
        }
        if (wrapper instanceof AnvilUpgradeWrapper anvil) {
            return anvil.getInventory();
        }
        if (wrapper instanceof SmithingUpgradeWrapper smithing) {
            return smithing.getInventory();
        }
        if (wrapper instanceof BlockConverterUpgradeWrapper<?, ?> blockConverter) {
            return blockConverter.getInputInventory();
        }
        return null;
    }

    public IUpgradeWrapper getOrCreateVirtualWrapper(int index) {
        ensureSubUpgradesInitialized();
        if (virtualWrapperCache[index] != null) {
            return virtualWrapperCache[index];
        }
        ItemStack stack = getVirtualUpgradeStack(index);
        Item item = stack.getItem();
        if (!(item instanceof IUpgradeItem<?> upgradeItem)) {
            throw new IllegalStateException("Constituent upgrade item is not an IUpgradeItem: " + item);
        }
        Consumer<ItemStack> saveHandler = saved -> onVirtualUpgradeSaved();
        IUpgradeWrapper wrapper = upgradeItem.getType().create(storageWrapper, stack, saveHandler);
        virtualWrapperCache[index] = wrapper;
        return wrapper;
    }

    @Override
    public IUpgradeWrapperAccessor wrapAccessor(IUpgradeWrapperAccessor accessor) {
        return new OmegaVirtualWrapperAccessor(this, accessor);
    }

    @Override
    public void onBeforeRemoved() {
        if (inBeforeRemoved || inSave) {
            return;
        }
        inBeforeRemoved = true;
        try {
            for (int i = 0; i < ConstituentUpgrade.COUNT; i++) {
                IUpgradeWrapper wrapper = virtualWrapperCache[i];
                if (wrapper != null) {
                    wrapper.onBeforeRemoved();
                }
            }
            if (isServerSide()) {
                save();
            }
        } finally {
            inBeforeRemoved = false;
        }
    }

    @Override
    public void onAdded() {
        if (!isServerSide()) {
            return;
        }
        cachedSubUpgrades = null;
        cachedSubUpgradesRevision = -1;
        Arrays.fill(virtualWrapperCache, null);
        ensureSubUpgradesInitialized();
        migrateFromPhysicalSlots();
        if (!virtualWrappersOnAdded) {
            virtualWrappersOnAdded = true;
            for (int i = 0; i < ConstituentUpgrade.COUNT; i++) {
                getOrCreateVirtualWrapper(i).onAdded();
            }
        }
    }

    private void migrateFromPhysicalSlots() {
        if (migrated) {
            return;
        }
        migrated = true;
        UpgradeHandler handler = storageWrapper.getUpgradeHandler();
        if (handler == null) {
            return;
        }
        boolean changed = false;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack slotStack = handler.getStackInSlot(slot);
            if (slotStack.isEmpty() || slotStack.getItem() instanceof CraftingUpgradeOmegaTierItem) {
                continue;
            }
            int index = ConstituentUpgrade.indexOf(slotStack.getItem());
            if (index < 0) {
                continue;
            }
            SubUpgradeContents contents = getSubUpgradeContents();
            if (contents != null) {
                contents.set(index, slotStack.copy());
            }
            virtualWrapperCache[index] = null;
            handler.setStackInSlot(slot, ItemStack.EMPTY);
            changed = true;
        }
        if (changed) {
            save();
        }
    }

    @Override
    public void clientTick(@Nullable Entity entity, Level level, BlockPos pos) {
        syncVirtualWrapperStacksFromHandlerStack();
    }

    @Override
    public void tick(@Nullable Entity entity, Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        ensureSubUpgradesInitialized();
        migrateFromPhysicalSlots();
        for (int i = 0; i < ConstituentUpgrade.COUNT; i++) {
            IUpgradeWrapper wrapper = getOrCreateVirtualWrapper(i);
            if (wrapper instanceof ITickableUpgrade tickable) {
                tickable.tick(entity, level, pos);
            }
        }
    }

    @Override
    public boolean canBeDisabled() {
        return false;
    }
}
