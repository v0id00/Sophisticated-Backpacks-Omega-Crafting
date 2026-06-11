package net.p3pp3rf1y.sophisticatedbackpacksomegacrafting;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModDataComponents {
    private ModDataComponents() {}

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, OmegaCraftingMod.MOD_ID);

    public static final Supplier<DataComponentType<SubUpgradeContents>> SUB_UPGRADES = DATA_COMPONENT_TYPES.register(
            "sub_upgrades",
            () -> DataComponentType.<SubUpgradeContents>builder()
                    .persistent(SubUpgradeContents.CODEC)
                    .networkSynchronized(SubUpgradeContents.STREAM_CODEC)
                    .build()
    );

    public static final Supplier<DataComponentType<Integer>> ACTIVE_UI_INDEX = DATA_COMPONENT_TYPES.register(
            "active_ui_index",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build()
    );

    /** Bumped on every server flush so slot sync detects nested sub-upgrade mutations. */
    public static final Supplier<DataComponentType<Integer>> SUB_UPGRADES_REVISION = DATA_COMPONENT_TYPES.register(
            "sub_upgrades_revision",
            () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build()
    );

    public static void register(IEventBus modBus) {
        DATA_COMPONENT_TYPES.register(modBus);
    }
}
