package org.eclipse.smarthome.automation.module.script.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.eclipse.smarthome.automation.module.script.ScriptEngineProvider;

public class GenericScriptEngineProvider implements ScriptEngineProvider {
    private ScriptEngineManager engineManager = new ScriptEngineManager();

    @Override
    public List<String> getLanguages() {
        ArrayList<String> languages = new ArrayList<>();

        for (ScriptEngineFactory f : engineManager.getEngineFactories()) {
            languages.addAll(f.getExtensions());
        }

        return languages;
    }

    @Override
    public void scopeValues(ScriptEngine scriptEngine, Map<String, Object> scopeValues) {
        for (Entry<String, Object> entry : scopeValues.entrySet()) {
            scriptEngine.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public ScriptEngine createScriptEngine(String fileExtension) {
        ScriptEngine engine = engineManager.getEngineByExtension(fileExtension);

        if (engine == null) {
            engine = engineManager.getEngineByName(fileExtension);
        }

        if (engine == null) {
            engine = engineManager.getEngineByMimeType(fileExtension);
        }

        return engine;
    }

    @Override
    public boolean isSupported(String fileExtension) {
        for (ScriptEngineFactory f : engineManager.getEngineFactories()) {
            if (f.getExtensions().contains(fileExtension)) {
                return true;
            }
        }

        return false;
    }

}
