package net.p3pp3rf1y.sophisticatedbackpacksomegacrafting.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedbackpacksomegacrafting.ConstituentUpgrade;
import net.p3pp3rf1y.sophisticatedbackpacksomegacrafting.CraftingUpgradeOmegaTierContainer;
import net.p3pp3rf1y.sophisticatedbackpacksomegacrafting.CraftingUpgradeOmegaTierWrapper;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeGuiManager;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;

import java.util.ArrayList;
import java.util.List;

public class CraftingUpgradeOmegaTierTab extends UpgradeSettingsTab<CraftingUpgradeOmegaTierContainer> {

    private static final int COLS = 4;
    private static final int ROWS = 2;
    private static final int SLOT = 20;
    private static final int PAD = 4;
    /** Inset between the omega panel border and inner content (selector + embedded sub-UIs). */
    private static final int PANEL_SIDE_PAD = 5;
    private static final int PANEL_BOTTOM_PAD = 5;
    private static final int EMBEDDED_TOP_GAP = 2;
    private static final int TAB_W = COLS * (SLOT + PAD) + PAD;
    private static final int SELECTOR_HEIGHT = ROWS * (SLOT + PAD) + PAD + 22;

    private final List<OmegaCraftingButton> buttons = new ArrayList<>();
    private final EmbeddedUpgradePanel[] embeddedPanels = new EmbeddedUpgradePanel[ConstituentUpgrade.COUNT];
    /** Fixed inner width sized to the widest constituent sub-UI so the panel does not resize on switch. */
    private final int fixedContentWidth;
    private int displayedActiveIndex = -1;

    public CraftingUpgradeOmegaTierTab(CraftingUpgradeOmegaTierContainer upgradeContainer, Position position, StorageScreenBase<?> screen) {
        super(upgradeContainer, position, screen,
                Component.translatable("item.sophisticatedbackpacksomegacrafting.crafting_upgrade_omega_tier"),
                Component.translatable("tooltip.sophisticatedbackpacksomegacrafting.crafting_upgrade_omega_tier"));

        List<ItemStack> stacks = CraftingUpgradeOmegaTierWrapper.getConstituentUpgradeStacks();
        int maxContentWidth = TAB_W;
        for (int i = 0; i < ConstituentUpgrade.COUNT; i++) {
            final int index = i;
            OmegaCraftingButton btn = new OmegaCraftingButton(
                    new Position(x, y),
                    new Dimension(SLOT, SLOT),
                    stacks.get(i),
                    btnIndex -> selectUpgrade(index)
            );
            buttons.add(btn);
            addHideableChild(btn);

            UpgradeContainerBase<?, ?> subContainer = upgradeContainer.getSubContainer(i);
            if (subContainer != null) {
                Position embeddedAnchor = embeddedContentPosition();
                UpgradeSettingsTab<?> embeddedTab = UpgradeGuiManager.getTab(subContainer, embeddedAnchor, screen);
                maxContentWidth = Math.max(maxContentWidth, SubUpgradeTabHelper.getOpenTabDimension(embeddedTab).width());
                EmbeddedUpgradePanel panel = new EmbeddedUpgradePanel(embeddedAnchor, embeddedTab);
                embeddedPanels[i] = panel;
                addHideableChild(panel);
            }
        }
        fixedContentWidth = maxContentWidth;
        repositionSelectorButtons();
        openTabDimension = new Dimension(panelWidth(fixedContentWidth), panelHeight(SELECTOR_HEIGHT, 0));

        displayedActiveIndex = upgradeContainer.getActiveUiIndex();
        updateButtonStates();
    }

    private void repositionSelectorButtons() {
        int selectorOffsetX = (fixedContentWidth - TAB_W) / 2;
        for (int i = 0; i < buttons.size(); i++) {
            int bx = x + PANEL_SIDE_PAD + selectorOffsetX + PAD + (i % COLS) * (SLOT + PAD);
            int by = y + 22 + (i / COLS) * (SLOT + PAD);
            buttons.get(i).setPosition(new Position(bx, by));
        }
    }

    private void selectUpgrade(int index) {
        displayedActiveIndex = index;
        getContainer().requestSetActiveUiIndex(index);
        updateButtonStates();
        applyActivePanel(displayedActiveIndex, true);
    }

    private void updateButtonStates() {
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setActive(displayedActiveIndex == i);
        }
    }

    private void applyActivePanel(int activeIndex, boolean resizeIfOpen) {
        for (int i = 0; i < embeddedPanels.length; i++) {
            EmbeddedUpgradePanel panel = embeddedPanels[i];
            if (panel != null) {
                panel.setEmbeddedOpen(i == activeIndex);
            }
        }
        updateTabDimensions(activeIndex);
        if (activeIndex >= 0) {
            layoutEmbeddedPanel(activeIndex);
        }
        if (resizeIfOpen && isOpen) {
            setWidth(Math.max(openTabDimension.width(), DEFAULT_WIDTH));
            setHeight(openTabDimension.height());
            moveSlotsToTab();
        }
    }

    private void layoutEmbeddedPanel(int index) {
        if (index < 0 || index >= embeddedPanels.length) {
            return;
        }
        EmbeddedUpgradePanel panel = embeddedPanels[index];
        if (panel == null) {
            return;
        }
        UpgradeSettingsTab<?> tab = panel.getTab();
        Dimension subDim = SubUpgradeTabHelper.getOpenTabDimension(tab);
        int centeredX = x + PANEL_SIDE_PAD + Math.max(0, (fixedContentWidth - subDim.width()) / 2);
        Position centeredPos = new Position(centeredX, embeddedContentY());
        panel.setPosition(centeredPos);
        panel.refreshDimensions();
        SubUpgradeTabHelper.setTabScreenPosition(tab, centeredPos);
    }

    private void updateTabDimensions(int activeIndex) {
        if (activeIndex < 0 || embeddedPanels[activeIndex] == null) {
            openTabDimension = new Dimension(panelWidth(fixedContentWidth), panelHeight(SELECTOR_HEIGHT, 0));
            return;
        }
        EmbeddedUpgradePanel panel = embeddedPanels[activeIndex];
        panel.refreshDimensions();
        Dimension subDim = SubUpgradeTabHelper.getOpenTabDimension(panel.getTab());
        openTabDimension = new Dimension(panelWidth(fixedContentWidth), panelHeight(SELECTOR_HEIGHT, subDim.height()));
    }

    private static int panelWidth(int contentWidth) {
        return contentWidth + 2 * PANEL_SIDE_PAD;
    }

    private static int panelHeight(int selectorHeight, int embeddedHeight) {
        if (embeddedHeight <= 0) {
            return selectorHeight + PANEL_BOTTOM_PAD;
        }
        return selectorHeight + EMBEDDED_TOP_GAP + embeddedHeight + PANEL_BOTTOM_PAD;
    }

    private int embeddedContentY() {
        return y + SELECTOR_HEIGHT + EMBEDDED_TOP_GAP;
    }

    private Position embeddedContentPosition() {
        return new Position(x + PANEL_SIDE_PAD, embeddedContentY());
    }

    @Override
    protected void onTabOpen() {
        displayedActiveIndex = getContainer().getActiveUiIndex();
        updateButtonStates();
        applyActivePanel(displayedActiveIndex, false);
        super.onTabOpen();
    }

    @Override
    protected void onTabClose() {
        getContainer().requestPersist();
        for (EmbeddedUpgradePanel panel : embeddedPanels) {
            if (panel != null) {
                panel.setEmbeddedOpen(false);
            }
        }
        for (UpgradeSettingsTab<?> tab : getEmbeddedTabs()) {
            if (tab != null) {
                SubUpgradeTabHelper.moveSlotsOutOfView(tab);
            }
        }
        super.onTabClose();
    }

    @Override
    protected void moveSlotsToTab() {
        for (UpgradeSettingsTab<?> tab : getEmbeddedTabs()) {
            if (tab != null) {
                SubUpgradeTabHelper.moveSlotsOutOfView(tab);
            }
        }
        if (displayedActiveIndex >= 0 && embeddedPanels[displayedActiveIndex] != null) {
            layoutEmbeddedPanel(displayedActiveIndex);
            SubUpgradeTabHelper.moveSlotsToTab(embeddedPanels[displayedActiveIndex].getTab());
        }
    }

    @Override
    public void tick() {
        super.tick();
        getContainer().getUpgradeWrapper().syncVirtualWrapperStacksFromHandlerStack();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        syncVirtualStacksFromHandler();
        int containerActive = getContainer().getActiveUiIndex();
        if (containerActive != displayedActiveIndex) {
            displayedActiveIndex = containerActive;
            updateButtonStates();
            applyActivePanel(displayedActiveIndex, true);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        for (EmbeddedUpgradePanel panel : embeddedPanels) {
            if (panel != null) {
                panel.tick();
            }
        }
    }

    private void syncVirtualStacksFromHandler() {
        getContainer().getUpgradeWrapper().syncVirtualWrapperStacksFromHandlerStack();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return forwardToActivePanel(panel -> panel.mouseClicked(mouseX, mouseY, button));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (super.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return forwardToActivePanel(panel -> panel.mouseReleased(mouseX, mouseY, button));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (super.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return forwardToActivePanel(panel -> panel.mouseDragged(mouseX, mouseY, button, dragX, dragY));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        EmbeddedUpgradePanel panel = getActivePanel();
        return panel != null && panel.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (super.charTyped(codePoint, modifiers)) {
            return true;
        }
        EmbeddedUpgradePanel panel = getActivePanel();
        return panel != null && panel.charTyped(codePoint, modifiers);
    }

    @Override
    public void renderTooltip(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(screen, guiGraphics, mouseX, mouseY);
        EmbeddedUpgradePanel panel = getActivePanel();
        if (panel != null) {
            panel.renderTooltip(screen, guiGraphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean slotIsNotCoveredAt(Slot slot, double mouseX, double mouseY) {
        EmbeddedUpgradePanel panel = getActivePanel();
        if (panel != null && !panel.slotIsNotCoveredAt(slot, mouseX, mouseY)) {
            return false;
        }
        return super.slotIsNotCoveredAt(slot, mouseX, mouseY);
    }

    private boolean forwardToActivePanel(java.util.function.Predicate<EmbeddedUpgradePanel> action) {
        EmbeddedUpgradePanel panel = getActivePanel();
        return panel != null && action.test(panel);
    }

    private EmbeddedUpgradePanel getActivePanel() {
        if (displayedActiveIndex < 0 || displayedActiveIndex >= embeddedPanels.length) {
            return null;
        }
        return embeddedPanels[displayedActiveIndex];
    }

    private List<UpgradeSettingsTab<?>> getEmbeddedTabs() {
        List<UpgradeSettingsTab<?>> tabs = new ArrayList<>();
        for (EmbeddedUpgradePanel panel : embeddedPanels) {
            if (panel != null) {
                tabs.add(panel.getTab());
            }
        }
        return tabs;
    }
}
