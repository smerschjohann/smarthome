package org.eclipse.smarthome.automation.module.script.internal;

import java.util.List;

import org.eclipse.smarthome.automation.module.script.ScriptEngineContainer;
import org.eclipse.smarthome.automation.module.script.ScriptExtensionProvider;

public class ScriptExtensionManagerWrapper {
    private ScriptEngineContainer container;
    private ScriptExtensionManager manager;

    public ScriptExtensionManagerWrapper(ScriptEngineContainer container, ScriptExtensionManager manager) {
        this.container = container;
        this.manager = manager;
    }

    public void addScriptExtensionProvider(ScriptExtensionProvider provider) {
        manager.addScriptExtensionProvider(provider);
    }

    public void removeScriptExtensionProvider(ScriptExtensionProvider provider) {
        manager.removeScriptExtensionProvider(provider);
    }

    public List<String> getTypes() {
        return manager.getTypes();
    }

    public List<String> getPresets() {
        return manager.getPresets();
    }

    public Object get(String type) {
        return manager.get(type, container.getIdentifier());
    }

    public List<String> getDefaultPresets() {
        return manager.getDefaultPresets();
    }

    public void importPreset(String preset) {
        manager.importPreset(preset, container.getProvider(), container.getScriptEngine(), container.getIdentifier());
    }
}
