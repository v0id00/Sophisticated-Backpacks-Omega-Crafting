package net.p3pp3rf1y.sophisticatedbackpacksomegacrafting;

import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapperAccessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OmegaVirtualWrapperAccessor implements IUpgradeWrapperAccessor {

    private final CraftingUpgradeOmegaTierWrapper omegaWrapper;
    private final IUpgradeWrapperAccessor delegate;
    private final Map<Class<?>, List<?>> cache = new HashMap<>();

    public OmegaVirtualWrapperAccessor(CraftingUpgradeOmegaTierWrapper omegaWrapper, IUpgradeWrapperAccessor delegate) {
        this.omegaWrapper = omegaWrapper;
        this.delegate = delegate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> getWrappersThatImplement(Class<T> upgradeClass) {
        List<T> cached = (List<T>) cache.get(upgradeClass);
        if (cached != null) {
            return cached;
        }
        List<T> result = new ArrayList<>(delegate.getWrappersThatImplement(upgradeClass));
        for (int i = 0; i < ConstituentUpgrade.COUNT; i++) {
            IUpgradeWrapper virtualWrapper = omegaWrapper.getOrCreateVirtualWrapper(i);
            if (virtualWrapper.isEnabled() && upgradeClass.isInstance(virtualWrapper)) {
                result.add(upgradeClass.cast(virtualWrapper));
            }
        }
        cache.put(upgradeClass, result);
        return result;
    }

    @Override
    public <T> List<T> getWrappersThatImplementFromMainStorage(Class<T> upgradeClass) {
        return delegate.getWrappersThatImplementFromMainStorage(upgradeClass);
    }

    @Override
    public void clearCache() {
        cache.clear();
        delegate.clearCache();
    }

    @Override
    public void onBeforeDeconstruct() {
        cache.clear();
    }
}
