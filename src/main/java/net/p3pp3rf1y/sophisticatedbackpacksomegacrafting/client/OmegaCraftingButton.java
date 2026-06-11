package net.p3pp3rf1y.sophisticatedbackpacksomegacrafting.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;

import java.util.function.IntConsumer;

public class OmegaCraftingButton extends ButtonBase {

    private final ItemStack item;
    private boolean active = false;

    public OmegaCraftingButton(Position position, Dimension dimension, ItemStack item, IntConsumer onClick) {
        super(position, dimension, onClick);
        this.item = item;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, Minecraft minecraft, int mouseX, int mouseY) {
        int s = getWidth();
        int bg = isHovered ? 0x88FFFFFF : (active ? 0x882D6A2D : 0x88444444);
        int border = isHovered ? 0xFFFFFFFF : (active ? 0xFF4CAF50 : 0xFF777777);

        guiGraphics.fill(x, y, x + s, y + s, bg);
        guiGraphics.fill(x, y, x + s, y + 1, border);
        guiGraphics.fill(x, y + s - 1, x + s, y + s, border);
        guiGraphics.fill(x, y, x + 1, y + s, border);
        guiGraphics.fill(x + s - 1, y, x + s, y + s, border);

        if (!item.isEmpty()) {
            int ix = x + (s - 16) / 2;
            int iy = y + (s - 16) / 2;
            guiGraphics.renderItem(item, ix, iy);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {}
}
