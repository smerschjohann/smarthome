package org.eclipse.smarthome.automation.module.script.rulesupport.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

import org.eclipse.smarthome.automation.type.ModuleType;
import org.eclipse.smarthome.automation.type.ModuleTypeProvider;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.osgi.framework.BundleContext;

public class ScriptedCustomModuleTypeProvider implements ModuleTypeProvider {
    private HashMap<String, ModuleType> modulesTypes = new HashMap<>();

    private HashSet<ProviderChangeListener<ModuleType>> listeners = new HashSet<>();

    private static ScriptedCustomModuleTypeProvider instance;

    public static ScriptedCustomModuleTypeProvider get() {
        return instance;
    }

    public void activate(BundleContext bundleContext) {
        ScriptedCustomModuleTypeProvider.instance = this;
    }

    @Override
    public Collection<ModuleType> getAll() {
        return modulesTypes.values();
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<ModuleType> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<ModuleType> listener) {
        this.listeners.remove(listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ModuleType> T getModuleType(String UID, Locale locale) {
        ModuleType handler = modulesTypes.get(UID);

        return (T) handler;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ModuleType> Collection<T> getModuleTypes(Locale locale) {
        return (Collection<T>) modulesTypes.values();
    }

    public void addModuleType(ModuleType moduleType) {
        modulesTypes.put(moduleType.getUID(), moduleType);

        for (ProviderChangeListener<ModuleType> listener : listeners) {
            listener.added(this, moduleType);
        }
    }

    public void removeModuleType(ModuleType moduleType) {
        removeModuleType(moduleType.getUID());
    }

    public void removeModuleType(String moduleTypeUID) {
        ModuleType element = modulesTypes.remove(moduleTypeUID);

        for (ProviderChangeListener<ModuleType> listener : listeners) {
            listener.removed(this, element);
        }
    }
}
