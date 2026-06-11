package net.p3pp3rf1y.sophisticatedbackpacksomegacrafting.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

public class EmbeddedUpgradePanel extends WidgetBase {
    private final UpgradeSettingsTab<?> tab;
    private boolean embeddedOpen;

    public EmbeddedUpgradePanel(Position position, UpgradeSettingsTab<?> tab) {
        super(position, new Dimension(tab.getWidth(), tab.getHeight()));
        this.tab = tab;
        setVisible(false);
    }

    public UpgradeSettingsTab<?> getTab() {
        return tab;
    }

    public void setEmbeddedOpen(boolean open) {
        if (embeddedOpen == open) {
            return;
        }
        embeddedOpen = open;
        setVisible(open);
        if (open) {
            SubUpgradeTabHelper.openEmbeddedTab(tab);
            updateDimensions(tab.getWidth(), tab.getHeight());
        } else {
            SubUpgradeTabHelper.closeEmbeddedTab(tab);
        }
    }

    public void refreshDimensions() {
        if (embeddedOpen) {
            Dimension dim = SubUpgradeTabHelper.getOpenTabDimension(tab);
            updateDimensions(dim.width(), dim.height());
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {}

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (embeddedOpen) {
            tab.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (embeddedOpen) {
            tab.renderTooltip(screen, guiGraphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return embeddedOpen && tab.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return embeddedOpen && tab.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return embeddedOpen && tab.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (!embeddedOpen) {
            return false;
        }
        try {
            var method = tab.getClass().getMethod("charTyped", char.class, int.class);
            return (boolean) method.invoke(tab, codePoint, modifiers);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!embeddedOpen) {
            return false;
        }
        try {
            var method = tab.getClass().getMethod("keyPressed", int.class, int.class, int.class);
            return (boolean) method.invoke(tab, keyCode, scanCode, modifiers);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    public void tick() {
        if (embeddedOpen) {
            tab.tick();
        }
    }

    public boolean slotIsNotCoveredAt(net.minecraft.world.inventory.Slot slot, double mouseX, double mouseY) {
        return !embeddedOpen || tab.slotIsNotCoveredAt(slot, mouseX, mouseY);
    }
}
