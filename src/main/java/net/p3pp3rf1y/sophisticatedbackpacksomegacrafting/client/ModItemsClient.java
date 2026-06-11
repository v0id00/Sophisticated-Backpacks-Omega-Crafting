package net.p3pp3rf1y.sophisticatedbackpacksomegacrafting.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.p3pp3rf1y.sophisticatedbackpacksomegacrafting.CraftingUpgradeOmegaTierContainer;
import net.p3pp3rf1y.sophisticatedbackpacksomegacrafting.ModItems;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

public class ModItemsClient {
    public static void init(IEventBus modBus) {
        modBus.addListener(ModItemsClient::onMenuScreenRegister);
    }

    private static void onMenuScreenRegister(RegisterMenuScreensEvent event) {
        UpgradeGuiManager.registerTab(ModItems.OMEGA_CRAFTING_TYPE,
                (CraftingUpgradeOmegaTierContainer uc, Position p, StorageScreenBase<?> s) ->
                        new CraftingUpgradeOmegaTierTab(uc, p, s));
    }
}
