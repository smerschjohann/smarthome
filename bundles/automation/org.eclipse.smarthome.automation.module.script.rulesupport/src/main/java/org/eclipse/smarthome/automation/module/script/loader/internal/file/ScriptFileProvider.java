package org.eclipse.smarthome.automation.module.script.loader.internal.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.smarthome.automation.module.script.ScriptEngineContainer;
import org.eclipse.smarthome.automation.module.script.ScriptEngineManager;
import org.eclipse.smarthome.core.service.file.AbstractFileProvider;

public abstract class ScriptFileProvider extends AbstractFileProvider<ScriptEngineContainer> {

    protected static final long RECHECK_INTERVAL = 20 * 1000;

    private long earliestStart = System.currentTimeMillis() + RECHECK_INTERVAL;

    private ScriptEngineManager manager;

    private Map<String, Set<URL>> urls = new ConcurrentHashMap<String, Set<URL>>();

    private Thread engineChecker;

    public ScriptFileProvider(String root) {
        super(root, new String[] { "automation" });
    }

    public void setScriptEngineManager(ScriptEngineManager manager) {
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

                        ScriptEngineContainer container = manager.createScriptEngine(scriptType, url.toString());

                        if (container != null) {
                            manager.loadScript(container.getIdentifier(), reader);

                            logger.debug("script successfully loaded: {}", url.toString());
                            updateProvidedObjectsHolder(url, Collections.singleton(container));
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
            Set<URL> set = urls.get(scriptType);
            if (set == null) {
                set = new HashSet<URL>();
                urls.put(scriptType, set);
            }
            set.add(url);
            logger.debug("in queue: {}", urls);
        }
    }

    private void dequeueUrl(URL url) {
        String scriptType = getScriptType(url);

        if (scriptType != null) {
            synchronized (urls) {
                Set<URL> set = urls.get(scriptType);
                if (set != null) {
                    set.remove(url);
                    if (set.isEmpty()) {
                        urls.remove(scriptType);
                    }
                }
                logger.debug("in queue: {}", urls);
            }
        }
    }

    @Override
    public void removeResources(File file) {
        // remove file from import queue as well
        try {
            URL url = file.toURI().toURL();
            dequeueUrl(url);
        } catch (MalformedURLException e) {
        }

        super.removeResources(file);
    }

    @Override
    protected void notifyListeners(ScriptEngineContainer removedObject) {
        if (removedObject != null) {
            manager.removeEngine(removedObject.getIdentifier());
        }
        super.notifyListeners(removedObject);
    }

    private void startEngineChecker() {
        if (engineChecker == null) {
            engineChecker = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!urls.isEmpty()) {
                        SortedSet<URL> reimportUrls = new TreeSet<URL>(new Comparator<URL>() {
                            @Override
                            public int compare(URL o1, URL o2) {
                                String f1 = o1.getPath();
                                String s1 = f1.substring(f1.lastIndexOf("/") + 1);
                                String f2 = o2.getPath();
                                String s2 = f2.substring(f2.lastIndexOf("/") + 1);

                                return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
                            }
                        });

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
    protected String getUID(ScriptEngineContainer providedObject) {
        return providedObject.getIdentifier();
    }

    @Override
    public Collection<ScriptEngineContainer> getAll() {
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
