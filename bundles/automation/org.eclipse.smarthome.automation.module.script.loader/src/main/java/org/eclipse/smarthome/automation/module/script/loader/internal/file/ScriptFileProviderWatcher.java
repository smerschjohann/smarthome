package org.eclipse.smarthome.automation.module.script.loader.internal.file;

import org.eclipse.smarthome.automation.provider.file.WatchServiceUtil;

public class ScriptFileProviderWatcher extends ScriptFileProvider {
    public ScriptFileProviderWatcher(String root) {
        super("jsr223"); // TODO this should better be configured by a property file
    }

    @Override
    protected void initializeWatchService(String watchingDir) {
        WatchServiceUtil.initializeWatchService(watchingDir, this);
    }

    @Override
    protected void deactivateWatchService(String watchingDir) {
        WatchServiceUtil.deactivateWatchService(watchingDir, this);
    }
}
