package org.eclipse.smarthome.automation.module.script.loader.internal.file;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.ScriptEngine;

import org.eclipse.smarthome.automation.module.script.ScriptManager;
import org.eclipse.smarthome.automation.module.script.loader.internal.ScriptContainer;
import org.eclipse.smarthome.core.service.file.AbstractFileProvider;

public abstract class ScriptFileProvider extends AbstractFileProvider<ScriptContainer> {

    protected static final long RECHECK_INTERVAL = 20 * 1000;

    private long earliestStart = System.currentTimeMillis() + 20 * 1000;

    private ScriptManager manager;

    private Map<String, Set<URL>> urls = new ConcurrentHashMap<String, Set<URL>>();

    private Thread engineChecker;

    public ScriptFileProvider(String root) {
        super(root, new String[] { "automation" });
    }

    public void setScriptManager(ScriptManager manager) {
        this.manager = manager;
    }

    @Override
    protected synchronized void importFile(URL url) {
        if (providerPortfolio.containsKey(url)) {
            // scripts should only be loaded once
            return;
        }

        String scriptType = getScriptType(url);
        if (scriptType != null) {
            if (System.currentTimeMillis() < earliestStart) {
                enqueueUrl(url, scriptType);
                startEngineChecker();
            } else {
                if (manager.isSupported(scriptType)) {
                    try (InputStreamReader reader = new InputStreamReader(new BufferedInputStream(url.openStream()))) {
                        logger.info("script loading: {}", url.toString());
                        ScriptEngine engine = manager.loadScript(url.toString(), reader);

                        if (engine != null) {
                            logger.debug("script successfully loaded: {}", url.toString());
                            updateProvidedObjectsHolder(url, Collections.singleton(new ScriptContainer(url, engine)));
                        } else {
                            logger.error("script ERROR, ignore file: {}", url);
                        }
                    } catch (IOException e) {
                        logger.error("url=" + url, e);
                    }
                } else {
                    enqueueUrl(url, scriptType);

                    logger.info("ScriptEngine for {} not available", scriptType);
                    startEngineChecker();
                }
            }
        }
    }

    private void enqueueUrl(URL url, String scriptType) {
        synchronized (urls) {
            Set<URL> value = urls.get(scriptType);
            if (value == null) {
                value = new HashSet<URL>();
                urls.put(scriptType, value);
            }
            value.add(url);
            logger.info("in queue: {}", urls);
        }
    }

    @Override
    protected void notifyListeners(ScriptContainer removedObject) {
        if (removedObject != null) {
            manager.unloadScript(removedObject.getScriptEngine());
        }
        super.notifyListeners(removedObject);
    }

    private void startEngineChecker() {
        if (engineChecker == null) {
            engineChecker = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!urls.isEmpty()) {
                        HashSet<URL> reimportUrls = new HashSet<URL>();
                        synchronized (urls) {
                            HashSet<String> newlySupported = new HashSet<>();
                            for (String key : urls.keySet()) {
                                if (manager.isSupported(key)) {
                                    newlySupported.add(key);
                                }
                            }

                            for (String key : newlySupported) {
                                reimportUrls.addAll(urls.remove(key));
                            }
                        }

                        for (URL url : reimportUrls) {
                            importFile(url);
                        }

                        try {
                            Thread.sleep(RECHECK_INTERVAL);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }

                    engineChecker = null;
                }
            });
            engineChecker.start();
        }
    }

    @Override
    protected String getUID(ScriptContainer providedObject) {
        return providedObject.getUrl().toString();
    }

    @Override
    public Collection<ScriptContainer> getAll() {
        return providedObjectsHolder.values();
    }

    private String getScriptType(URL url) {
        String fileName = url.getPath();
        int idx = fileName.lastIndexOf(".");
        if (idx == -1) {
            return null;
        }
        String fileExtension = fileName.substring(idx + 1);

        // ignore known file extensions for "temp" files
        if (fileExtension.equals("txt") || fileExtension.endsWith("~") || fileExtension.endsWith("swp")) {
            return null;
        }
        return fileExtension;
    }
}
