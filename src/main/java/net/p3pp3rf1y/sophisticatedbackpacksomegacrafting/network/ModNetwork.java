package net.p3pp3rf1y.sophisticatedbackpacksomegacrafting.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.p3pp3rf1y.sophisticatedbackpacksomegacrafting.OmegaCraftingMod;

public class ModNetwork {
    private ModNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(OmegaCraftingMod.MOD_ID).versioned("1");
        registrar.playToClient(
                SyncOmegaSubUpgradesPayload.TYPE,
                SyncOmegaSubUpgradesPayload.STREAM_CODEC,
                SyncOmegaSubUpgradesPayload::handlePayload
        );
    }
}
