package org.eclipse.smarthome.automation.module.script.loader.internal.file;

import org.eclipse.smarthome.core.service.file.WatchServiceUtil;

public class ScriptFileProviderWatcher extends ScriptFileProvider {
    public ScriptFileProviderWatcher() {
        super("jsr223");
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
