package org.eclipse.smarthome.automation.module.script.loader.internal;

import java.net.URL;

import javax.script.ScriptEngine;

public class ScriptContainer {
    private URL url;
    private ScriptEngine scriptEngine;

    public ScriptContainer(URL url, ScriptEngine scriptEngine) {
        super();
        this.url = url;
        this.scriptEngine = scriptEngine;
    }

    public URL getUrl() {
        return url;
    }

    public ScriptEngine getScriptEngine() {
        return scriptEngine;
    }
}
