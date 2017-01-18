package org.eclipse.smarthome.automation.module.script;

import java.io.File;
import java.io.InputStreamReader;

import javax.script.ScriptEngine;

public interface ScriptManager {

    /**
     * checks if the given script type (file extension) is supported by any scriptengine.
     *
     * @param scriptType
     * @return
     */
    boolean isSupported(String scriptType);

    ScriptEngine loadScript(File file);

    /**
     * loads a script by evaluating its content. After successful loading the scriptLoaded method gets invoked to allow
     * scripts to react on script load/unload.
     *
     * @param identifier: Script file identifier (will be passed to the scriptLoaded method)
     * @param scriptData: file content
     */
    ScriptEngine loadScript(String identifier, InputStreamReader scriptData);

    /**
     * This method should be called when a script needs to be unloaded (removed from directory or updated)
     *
     * @param identifier: the unique file identifier
     */
    void unloadScript(ScriptEngine engine);

}