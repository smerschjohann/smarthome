package org.eclipse.smarthome.automation.module.script.loader;

import org.eclipse.smarthome.automation.provider.file.WatchServiceUtil;

public class ScriptFileProviderWatcher extends ScriptFileProvider {
    public ScriptFileProviderWatcher(String root) {
        super(root);
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
