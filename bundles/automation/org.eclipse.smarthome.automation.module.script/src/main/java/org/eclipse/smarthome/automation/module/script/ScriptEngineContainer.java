package org.eclipse.smarthome.automation.module.script;

import javax.script.ScriptEngine;

public class ScriptEngineContainer {
    private ScriptEngine scriptEngine;
    private ScriptEngineProvider provider;
    private String identifier;

    public ScriptEngineContainer(ScriptEngine scriptEngine, ScriptEngineProvider provider, String identifier) {
        super();
        this.scriptEngine = scriptEngine;
        this.provider = provider;
        this.identifier = identifier;
    }

    public ScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    public ScriptEngineProvider getProvider() {
        return provider;
    }

    public String getIdentifier() {
        return identifier;
    }
}
