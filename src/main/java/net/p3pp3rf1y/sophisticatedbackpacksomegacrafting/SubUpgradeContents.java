package net.p3pp3rf1y.sophisticatedbackpacksomegacrafting;

import com.mojang.serialization.Codec;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class SubUpgradeContents {
    public static final Codec<SubUpgradeContents> CODEC = Codec.list(ItemStack.OPTIONAL_CODEC)
            .xmap(SubUpgradeContents::fromOptionalList, SubUpgradeContents::asOptionalList);

    public static final StreamCodec<RegistryFriendlyByteBuf, SubUpgradeContents> STREAM_CODEC = StreamCodec.of(
            (buf, contents) -> {
                for (int i = 0; i < ConstituentUpgrade.COUNT; i++) {
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, contents.get(i));
                }
            },
            buf -> {
                NonNullList<ItemStack> stacks = NonNullList.withSize(ConstituentUpgrade.COUNT, ItemStack.EMPTY);
                for (int i = 0; i < ConstituentUpgrade.COUNT; i++) {
                    stacks.set(i, ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                }
                return fromOptionalList(stacks);
            }
    );

    private final NonNullList<ItemStack> stacks;

    public SubUpgradeContents(List<ItemStack> stacks) {
        this.stacks = NonNullList.withSize(ConstituentUpgrade.COUNT, ItemStack.EMPTY);
        for (int i = 0; i < ConstituentUpgrade.COUNT; i++) {
            this.stacks.set(i, i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY);
        }
    }

    private static SubUpgradeContents fromOptionalList(List<ItemStack> stacks) {
        NonNullList<ItemStack> normalized = NonNullList.withSize(ConstituentUpgrade.COUNT, ItemStack.EMPTY);
        for (int i = 0; i < ConstituentUpgrade.COUNT; i++) {
            ItemStack stack = i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY;
            normalized.set(i, stack.isEmpty() ? ConstituentUpgrade.byIndex(i).createDefaultStack() : stack);
        }
        return new SubUpgradeContents(normalized);
    }

    private static List<ItemStack> asOptionalList(SubUpgradeContents contents) {
        return contents.asList();
    }

    public static SubUpgradeContents createDefault() {
        return fromOptionalList(List.of());
    }

    public ItemStack get(int index) {
        return stacks.get(index);
    }

    public void set(int index, ItemStack stack) {
        stacks.set(index, stack);
    }

    public List<ItemStack> asList() {
        return stacks;
    }
}
