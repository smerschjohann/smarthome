/**
 * Copyright (c) 2015-2016 Simon Merschjohann and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.automation.module.script.loader.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.smarthome.automation.module.script.ScriptEngineProvider;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enables loading of scripts from local directory and bundles
 *
 * @author Simon Merschjohann
 *
 */
public class ScriptLoaderActivator implements BundleActivator {
    private final Logger logger = LoggerFactory.getLogger(ScriptLoaderActivator.class);

    private ArrayList<ServiceRegistration<?>> regs = new ArrayList<>();

    private ScriptManager scriptManager;

    private Thread scriptUpdateWatcher;

    @Override
    public void start(BundleContext context) throws Exception {
        List<String> languages = ScriptEngineProvider.getScriptLanguages();
        logger.info("available languages: " + languages);

        scriptManager = new ScriptManager();
        ScriptResourceImporter importer = new ScriptResourceImporter(context, scriptManager);

        ScriptAutomationResourceBundlesEventQueue queue = new ScriptAutomationResourceBundlesEventQueue(context,
                importer);

        importer.setQueue(queue);

        queue.open();

        File folder = getFolder("scripts");

        if (folder.exists() && folder.isDirectory()) {
            scriptUpdateWatcher = new Thread(new ScriptUpdateWatcher(scriptManager, folder));
            scriptUpdateWatcher.start();
        } else {
            logger.warn("script directory ({}): scripts missing, no scripts will be added!", folder.getAbsolutePath());
        }

    }

    private File getFolder(String foldername) {
        File folder = new File(ConfigConstants.getConfigFolder() + File.separator + foldername);
        return folder;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        for (ServiceRegistration<?> reg : regs) {
            reg.unregister();
        }

        regs.clear();
    }

}
