/**
 * Copyright (c) 2015-2016 Simon Merschjohann and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.automation.module.script;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ScriptManager allows to load and unloading of script files using a script engines script type
 *
 * @author Simon Merschjohann
 *
 */
public class ScriptManager {
    private final Logger logger = LoggerFactory.getLogger(ScriptManager.class.getName());

    /**
     * checks if the given script type (file extension) is supported by any scriptengine.
     *
     * @param scriptType
     * @return
     */
    public boolean isSupported(String scriptType) {
        return ScriptEngineProvider.getScriptEngine(scriptType) != null;
    }

    public ScriptEngine loadScript(File file) {
        try (InputStreamReader streamReader = new InputStreamReader(new FileInputStream(file), UTF_8)) {
            return loadScript(uniqueId(file), streamReader);
        } catch (IOException ex) {
            logger.error("", ex);
        }

        return null;
    }

    /**
     * loads a script by evaluating its content. After successful loading the scriptLoaded method gets invoked to allow
     * scripts to react on script load/unload.
     *
     * @param identifier: Script file identifier (will be passed to the scriptLoaded method)
     * @param scriptData: file content
     */
    public ScriptEngine loadScript(String identifier, InputStreamReader scriptData) {
        String scriptType = scriptType(identifier);
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
            } catch (ScriptException e) {
                logger.error("Error while executing script", e);

                ScriptEngineProvider.removeEngine(engine);
                engine = null;
            }
        }

        return engine;
    }

    /**
     * This method should be called when a script needs to be unloaded (removed from directory or updated)
     *
     * @param identifier: the unique file identifier
     */
    public void unloadScript(ScriptEngine engine) {
        if (engine != null) {
            Invocable inv = (Invocable) engine;
            try {
                inv.invokeFunction("scriptUnloaded");
            } catch (NoSuchMethodException e) {
                logger.trace("scriptUnloaded() not defined in script");
            } catch (ScriptException e) {
                logger.error("Error while executing script", e);
            }

            ScriptEngineProvider.removeEngine(engine);
        }
    }

    private String scriptType(String path) {
        int fileExtensionStartIndex = path.lastIndexOf(".") + 1;
        if (fileExtensionStartIndex == -1) {
            return null;
        }

        return path.substring(fileExtensionStartIndex);
    }

    private String uniqueId(File file) {
        return file.getAbsolutePath();
    }
}
