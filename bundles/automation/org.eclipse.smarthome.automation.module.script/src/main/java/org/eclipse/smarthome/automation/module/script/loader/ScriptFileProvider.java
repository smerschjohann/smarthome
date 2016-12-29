package org.eclipse.smarthome.automation.module.script.loader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.ScriptEngine;

import org.eclipse.smarthome.automation.module.script.ScriptContainer;
import org.eclipse.smarthome.automation.module.script.ScriptManager;
import org.eclipse.smarthome.automation.provider.file.AbstractFileProvider;

public abstract class ScriptFileProvider extends AbstractFileProvider<ScriptContainer>
        implements org.eclipse.smarthome.automation.module.script.ScriptProvider {

    protected static final long RECHECK_INTERVAL = 30 * 1000;

    private ScriptManager manager;

    private Map<String, List<URL>> urls = new ConcurrentHashMap<String, List<URL>>();

    private Thread engineChecker;

    public ScriptFileProvider(String root) {
        super(root);
    }

    public void setScriptManager(ScriptManager manager) {
        this.manager = manager;
    }

    @Override
    protected void importFile(URL url) {
        String scriptType = getScriptType(url);
        if (scriptType != null) {
            if (manager.isSupported(scriptType)) {
                try (InputStreamReader reader = new InputStreamReader(new BufferedInputStream(url.openStream()))) {
                    ScriptEngine engine = manager.loadScript(url.toString(), reader);

                    if (engine != null) {
                        updateProvidedObjectsHolder(url, Collections.singleton(new ScriptContainer(url, engine)));
                    } else {
                        logger.error("error in script, ignore file: {}", url);
                    }
                } catch (IOException e) {
                    logger.error("url=" + url, e);
                }
            } else {
                synchronized (urls) {
                    List<URL> value = urls.get(scriptType);
                    if (value == null) {
                        value = new ArrayList<URL>();
                        urls.put(scriptType, value);
                    }
                    value.add(url);
                }

                logger.debug("ScriptEngine for {} not available", scriptType, new Exception());
                startEngineChecker();
            }
        } else {
            logger.error("cannot determine type of script");
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
        if (fileName.lastIndexOf(".") == -1) {
            return null;
        }
        String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
        if (fileExtension.equals("txt")) {
            return null;
        }
        return fileExtension;
    }
}
