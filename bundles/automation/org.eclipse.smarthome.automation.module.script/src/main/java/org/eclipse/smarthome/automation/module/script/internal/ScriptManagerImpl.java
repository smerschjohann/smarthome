/**
 * Copyright (c) 2015-2016 Simon Merschjohann and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.automation.module.script.internal;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.smarthome.automation.module.script.ScriptEngineProvider;
import org.eclipse.smarthome.automation.module.script.ScriptManager;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ScriptManager allows to load and unloading of script files using a script engines script type
 *
 * @author Simon Merschjohann
 *
 */
public class ScriptManagerImpl implements ScriptManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private BundleContext bundleContext;
    private ScriptEngineProvider scriptEngineProvider;

    public ScriptManagerImpl() {
        logger.info("ScriptManager loading...");
    }

    public void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setScriptEngineProvider(ScriptEngineProvider provider) {
        this.scriptEngineProvider = provider;
    }

    @Override
    public boolean isSupported(String scriptType) {
        return scriptEngineProvider.getScriptEngine(scriptType) != null;
    }

    @Override
    public ScriptEngine loadScript(File file) {
        try (InputStreamReader streamReader = new InputStreamReader(new FileInputStream(file), UTF_8)) {
            return loadScript(uniqueId(file), streamReader);
        } catch (IOException ex) {
            logger.error("", ex);
        }

        return null;
    }

    @Override
    public ScriptEngine loadScript(String identifier, InputStreamReader scriptData) {
        String scriptType = scriptType(identifier);
        ScriptEngine engine = scriptEngineProvider.getScriptEngine(scriptType);

        if (engine == null) {
            logger.error("loadScript(): script language '{}' could not be found for: {}", scriptType, identifier);
        } else {
            try {
                engine.eval(scriptData);
                if (engine instanceof Invocable) {
                    Invocable inv = (Invocable) engine;
                    try {
                        inv.invokeFunction("scriptLoaded", identifier);
                    } catch (NoSuchMethodException e) {
                        logger.trace("scriptLoaded() not definied in script: " + identifier);
                    }
                } else {
                    logger.trace("engine does not support Invocable interface");
                }
            } catch (ScriptException e) {
                logger.error("Error while executing script", e);

                scriptEngineProvider.removeEngine(engine);
                engine = null;
            }
        }

        return engine;
    }

    @Override
    public void unloadScript(ScriptEngine engine) {
        if (engine != null) {
            if (engine instanceof Invocable) {
                Invocable inv = (Invocable) engine;
                try {
                    inv.invokeFunction("scriptUnloaded");
                } catch (NoSuchMethodException e) {
                    logger.trace("scriptUnloaded() not defined in script");
                } catch (ScriptException e) {
                    logger.error("Error while executing script", e);
                }
            } else {
                logger.trace("engine does not support Invocable interface");
            }

            scriptEngineProvider.removeEngine(engine);
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
