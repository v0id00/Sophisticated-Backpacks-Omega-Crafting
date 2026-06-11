package net.p3pp3rf1y.sophisticatedbackpacksomegacrafting.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.p3pp3rf1y.sophisticatedbackpacksomegacrafting.CraftingUpgradeOmegaTierContainer;
import net.p3pp3rf1y.sophisticatedbackpacksomegacrafting.OmegaCraftingMod;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.util.StreamCodecHelper;

import javax.annotation.Nullable;

public record SyncOmegaSubUpgradesPayload(int windowId, int upgradeSlot, @Nullable CompoundTag data)
        implements CustomPacketPayload {

    public static final Type<SyncOmegaSubUpgradesPayload> TYPE =
            new Type<>(OmegaCraftingMod.id("sync_omega_sub_upgrades"));

    public static final StreamCodec<ByteBuf, SyncOmegaSubUpgradesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SyncOmegaSubUpgradesPayload::windowId,
            ByteBufCodecs.INT,
            SyncOmegaSubUpgradesPayload::upgradeSlot,
            StreamCodecHelper.ofNullable(ByteBufCodecs.COMPOUND_TAG),
            SyncOmegaSubUpgradesPayload::data,
            SyncOmegaSubUpgradesPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handlePayload(SyncOmegaSubUpgradesPayload payload, IPayloadContext context) {
        if (payload.data == null) {
            return;
        }
        Player player = context.player();
        if (!(player.containerMenu instanceof StorageContainerMenuBase<?> menu)
                || menu.containerId != payload.windowId()) {
            return;
        }
        if (menu.getUpgradeContainers().get(payload.upgradeSlot())
                instanceof CraftingUpgradeOmegaTierContainer omegaContainer) {
            omegaContainer.applyClientSubUpgradeSync(payload.data());
        }
    }
}
