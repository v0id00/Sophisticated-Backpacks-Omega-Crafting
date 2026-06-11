package net.p3pp3rf1y.sophisticatedbackpacksomegacrafting;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.p3pp3rf1y.sophisticatedbackpacksomegacrafting.client.ModItemsClient;
import net.p3pp3rf1y.sophisticatedbackpacksomegacrafting.network.ModNetwork;

@Mod(OmegaCraftingMod.MOD_ID)
public class OmegaCraftingMod {
    public static final String MOD_ID = "sophisticatedbackpacksomegacrafting";

    public OmegaCraftingMod(IEventBus modBus, ModContainer container) {
        ModDataComponents.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModItems.CREATIVE_MODE_TABS.register(modBus);

        modBus.addListener(ModItems::registerContainers);
        modBus.addListener(ModNetwork::registerPayloads);
        modBus.addListener(this::addCreative);

        if (FMLEnvironment.dist.isClient()) {
            ModItemsClient.init(modBus);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems.CREATIVE_TAB.get()) {
            event.accept(ModItems.CRAFTING_UPGRADE_OMEGA_TIER.get());
        }
    }
}
