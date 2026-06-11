package net.p3pp3rf1y.sophisticatedbackpacksomegacrafting.client;

import net.p3pp3rf1y.sophisticatedcore.client.gui.SettingsTabBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.CompositeWidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.WidgetBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.common.gui.UpgradeContainerBase;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class SubUpgradeTabHelper {
    private static final Method MOVE_SLOTS_TO_TAB;
    private static final Method MOVE_SLOTS_OUT_OF_VIEW;
    private static final Method ON_TAB_OPEN;
    private static final Method ON_TAB_CLOSE;
    private static final Method GET_CONTAINER;
    private static final Field OPEN_TAB_DIMENSION;
    private static final Field TAB_IS_OPEN;

    static {
        Method moveToTab = null;
        Method moveOut = null;
        Method onOpen = null;
        Method onClose = null;
        Method getContainer = null;
        Field openDim = null;
        Field tabIsOpen = null;
        try {
            moveToTab = UpgradeSettingsTab.class.getDeclaredMethod("moveSlotsToTab");
            moveToTab.setAccessible(true);
            moveOut = UpgradeSettingsTab.class.getDeclaredMethod("moveSlotsOutOfView");
            moveOut.setAccessible(true);
            onOpen = UpgradeSettingsTab.class.getDeclaredMethod("onTabOpen");
            onOpen.setAccessible(true);
            onClose = UpgradeSettingsTab.class.getDeclaredMethod("onTabClose");
            onClose.setAccessible(true);
            getContainer = UpgradeSettingsTab.class.getDeclaredMethod("getContainer");
            getContainer.setAccessible(true);
            openDim = SettingsTabBase.class.getDeclaredField("openTabDimension");
            openDim.setAccessible(true);
            tabIsOpen = SettingsTabBase.class.getDeclaredField("isOpen");
            tabIsOpen.setAccessible(true);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        MOVE_SLOTS_TO_TAB = moveToTab;
        MOVE_SLOTS_OUT_OF_VIEW = moveOut;
        ON_TAB_OPEN = onOpen;
        ON_TAB_CLOSE = onClose;
        GET_CONTAINER = getContainer;
        OPEN_TAB_DIMENSION = openDim;
        TAB_IS_OPEN = tabIsOpen;
    }

    private SubUpgradeTabHelper() {}

    public static void moveSlotsToTab(UpgradeSettingsTab<?> tab) {
        invoke(tab, MOVE_SLOTS_TO_TAB);
    }

    public static void moveSlotsOutOfView(UpgradeSettingsTab<?> tab) {
        invoke(tab, MOVE_SLOTS_OUT_OF_VIEW);
    }

    public static void openEmbeddedTab(UpgradeSettingsTab<?> tab) {
        setEmbeddedOpenState(tab, true);
        invoke(tab, ON_TAB_OPEN);
    }

    public static void closeEmbeddedTab(UpgradeSettingsTab<?> tab) {
        invoke(tab, ON_TAB_CLOSE);
        setEmbeddedOpenState(tab, false);
    }

    /**
     * Native upgrade tabs gate slot backgrounds and widgets on container/tab open state.
     * Embedded tabs bypass UpgradeSettingsTab#setOpen, so both must be set manually.
     */
    private static void setEmbeddedOpenState(UpgradeSettingsTab<?> tab, boolean open) {
        try {
            TAB_IS_OPEN.setBoolean(tab, open);
            UpgradeContainerBase<?, ?> container = (UpgradeContainerBase<?, ?>) GET_CONTAINER.invoke(tab);
            container.setIsOpen(open);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set embedded tab open state", e);
        }
    }

    public static Dimension getOpenTabDimension(UpgradeSettingsTab<?> tab) {
        try {
            return (Dimension) OPEN_TAB_DIMENSION.get(tab);
        } catch (IllegalAccessException e) {
            return new Dimension(0, 0);
        }
    }

    public static void setTabScreenPosition(UpgradeSettingsTab<?> tab, Position position) {
        int dx = position.x() - tab.getX();
        int dy = position.y() - tab.getY();
        if (dx == 0 && dy == 0) {
            return;
        }
        tab.setPosition(position);
        offsetWidgetChildren(tab, dx, dy);
    }

    private static void offsetWidgetChildren(CompositeWidgetBase<?> composite, int dx, int dy) {
        for (var child : composite.children()) {
            if (child instanceof WidgetBase widget) {
                widget.setPosition(new Position(widget.getX() + dx, widget.getY() + dy));
                if (widget instanceof CompositeWidgetBase<?> nested) {
                    offsetWidgetChildren(nested, dx, dy);
                }
            }
        }
    }

    private static void invoke(UpgradeSettingsTab<?> tab, Method method) {
        try {
            method.invoke(tab);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to invoke " + method.getName() + " on embedded upgrade tab", e);
        }
    }
}
