/**
 * Copyright (c) 2015-2016 Simon Merschjohann and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.automation.module.script.loader.internal;

import java.io.InputStreamReader;
import java.util.HashMap;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.smarthome.automation.module.script.ScriptEngineProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ScriptManager allows to load and unloading of script files using a script engines script type
 *
 * @author Simon Merschjohann
 *
 */
public class ScriptManager {
    private Logger logger = LoggerFactory.getLogger(ScriptManager.class);

    HashMap<String, ScriptEngine> loadedScripts = new HashMap<>();

    /**
     * checks if the given script type (file extension) is supported by any scriptengine.
     *
     * @param scriptType
     * @return
     */
    public boolean isSupported(String scriptType) {
        return ScriptEngineProvider.getScriptEngine(scriptType) != null;
    }

    /**
     * loads a script by evaluating its content. After successful loading the scriptLoaded method gets invoked to allow
     * scripts to react on script load/unload.
     *
     * @param identifier: Script file identifier (will be passed to the scriptLoaded method)
     * @param scriptType: the script engines type
     * @param scriptData: file content
     */
    public void loadScript(String identifier, String scriptType, InputStreamReader scriptData) {
        ScriptEngine engine = ScriptEngineProvider.getScriptEngine(scriptType);

        if (engine == null) {
            logger.error("loadScript(): script language '{}' could not be found for: {}", scriptType, identifier);
        } else {
            try {
                engine.eval(scriptData);
                Invocable inv = (Invocable) engine;
                try {
                    inv.invokeFunction("scriptLoaded", identifier);
                } catch (NoSuchMethodException e) {
                    logger.trace("scriptLoaded() not definied in script: " + identifier);
                }

                loadedScripts.put(identifier, engine);
            } catch (ScriptException e) {
                logger.error("Error while executing script", e);

                ScriptEngineProvider.removeEngine(engine);
            }
        }
    }

    /**
     * This method should be called when a script needs to be unloaded (removed from directory or updated)
     *
     * @param identifier: the unique file identifier
     */
    public void unloadScript(String identifier) {
        ScriptEngine engine = loadedScripts.remove(identifier);

        if (engine != null) {
            Invocable inv = (Invocable) engine;
            try {
                inv.invokeFunction("scriptUnloaded", identifier);
            } catch (NoSuchMethodException e) {
                logger.trace("scriptUnloaded() not defined in script: " + identifier);
            } catch (ScriptException e) {
                logger.error("Error while executing script", e);
            }

            ScriptEngineProvider.removeEngine(engine);
        }
    }

}
