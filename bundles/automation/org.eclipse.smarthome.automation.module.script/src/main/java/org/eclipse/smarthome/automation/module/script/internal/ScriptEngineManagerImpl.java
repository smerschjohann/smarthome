/**
 * Copyright (c) 2015-2016 Simon Merschjohann and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.automation.module.script.internal;

import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.smarthome.automation.module.script.ScriptEngineContainer;
import org.eclipse.smarthome.automation.module.script.ScriptEngineManager;
import org.eclipse.smarthome.automation.module.script.ScriptEngineProvider;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ScriptManager allows to load and unloading of script files using a script engines script type
 *
 * @author Simon Merschjohann
 *
 */
public class ScriptEngineManagerImpl implements ScriptEngineManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private Set<ScriptEngineProvider> scriptEngineProviders = new HashSet<>();
    private HashMap<String, ScriptEngineContainer> loadedScriptEngineInstances = new HashMap<>();
    private HashMap<String, ScriptEngineProvider> supportedLanguages = new HashMap<>();
    private GenericScriptEngineProvider genericProvider = new GenericScriptEngineProvider();

    public ScriptEngineManagerImpl() {
        logger.info("ScriptManager loading...");
    }

    public void activate(BundleContext bundleContext) {
    }

    public void addScriptEngineProvider(ScriptEngineProvider provider) {
        this.scriptEngineProviders.add(provider);

        for (String language : provider.getLanguages()) {
            this.supportedLanguages.put(language, provider);
        }
    }

    public void removeScriptEngineProvider(ScriptEngineProvider provider) {
        this.scriptEngineProviders.remove(provider);
    }

    @Override
    public boolean isSupported(String fileExtension) {
        return findProvider(fileExtension) != null;
    }

    /*
     * @Override
     * public ScriptEngine loadScript(File file) {
     * try (InputStreamReader streamReader = new InputStreamReader(new FileInputStream(file), UTF_8)) {
     * return loadScript(uniqueId(file), streamReader);
     * } catch (IOException ex) {
     * logger.error("", ex);
     * }
     *
     * return null;
     * }
     */

    @Override
    public ScriptEngineContainer createScriptEngine(String fileExtension, String scriptIdentifier) {
        ScriptEngineContainer result = null;
        ScriptEngineProvider engineProvider = findProvider(fileExtension);

        if (engineProvider == null) {
            logger.error("loadScript(): scriptengine for language '{}' could not be found for identifier: {}",
                    fileExtension, scriptIdentifier);
        } else {
            try {
                ScriptEngine engine = engineProvider.createScriptEngine(fileExtension);
                HashMap<String, Object> scriptExManager = new HashMap<>();
                result = new ScriptEngineContainer(engine, engineProvider, scriptIdentifier);
                scriptExManager.put("ScriptExtension", new ScriptExtensionManagerWrapper(result));
                engineProvider.scopeValues(engine, scriptExManager);
                ScriptExtensionManager.importDefaultPresets(engineProvider, engine, scriptIdentifier);

                loadedScriptEngineInstances.put(scriptIdentifier, result);
            } catch (Exception ex) {
                logger.error("Error while creating ScriptEngine", ex);
                removeScriptExtensions(scriptIdentifier);
            }
        }

        return result;
    }

    @Override
    public void loadScript(String scriptIdentifier, InputStreamReader scriptData) {
        ScriptEngineContainer container = loadedScriptEngineInstances.get(scriptIdentifier);

        if (container == null) {
            logger.error("could not load script as no engine is created");
        } else {
            ScriptEngine engine = container.getScriptEngine();
            try {
                engine.eval(scriptData);

                if (engine instanceof Invocable) {
                    Invocable inv = (Invocable) engine;
                    try {
                        inv.invokeFunction("scriptLoaded", scriptIdentifier);
                    } catch (NoSuchMethodException e) {
                        logger.trace("scriptLoaded() not defined in script: " + scriptIdentifier);
                    }
                } else {
                    logger.trace("engine does not support Invocable interface");
                }
            } catch (Exception ex) {
                logger.error("Error during Script evaluation {}", scriptIdentifier, ex);
            }
        }
    }

    @Override
    public void removeEngine(String scriptIdentifier) {
        ScriptEngineContainer container = loadedScriptEngineInstances.get(scriptIdentifier);

        if (container != null) {
            if (container.getScriptEngine() instanceof Invocable) {
                Invocable inv = (Invocable) container.getScriptEngine();
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

            removeScriptExtensions(scriptIdentifier);
        }
    }

    private void removeScriptExtensions(String pathIdentifier) {
        try {
            ScriptExtensionManager.dispose(pathIdentifier);
        } catch (Exception ex) {
            logger.error("error removing engine", ex);
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

    private ScriptEngineProvider findProvider(String fileExtension) {
        ScriptEngineProvider engineProvider = supportedLanguages.get(fileExtension);

        if (engineProvider != null) {
            return engineProvider;
        }

        for (ScriptEngineProvider provider : supportedLanguages.values()) {
            if (provider.isSupported(fileExtension)) {
                return provider;
            }
        }

        if (genericProvider.isSupported(fileExtension)) {
            return genericProvider;
        }

        return null;
    }

}
