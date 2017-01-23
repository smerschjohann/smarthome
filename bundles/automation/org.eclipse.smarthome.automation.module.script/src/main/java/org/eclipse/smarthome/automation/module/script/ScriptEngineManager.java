package org.eclipse.smarthome.automation.module.script;

import java.io.InputStreamReader;

public interface ScriptEngineManager {

    /**
     * Checks if a given fileExtension is supported
     *
     * @param fileExtension
     * @return true if supported
     */
    boolean isSupported(String fileExtension);

    /**
     * Creates a new ScriptEngine based on the given fileExtension
     *
     * @param fileExtension
     * @param scriptIdentifier
     * @return
     */
    ScriptEngineContainer createScriptEngine(String fileExtension, String scriptIdentifier);

    /**
     * Loads a script and initializes its scope variables
     *
     * @param fileExtension
     * @param scriptIdentifier
     * @param scriptData
     * @return
     */
    void loadScript(String scriptIdentifier, InputStreamReader scriptData);

    /**
     * Unloads the ScriptEngine loaded with the scriptIdentifer
     *
     * @param scriptIdentifier
     */
    void removeEngine(String scriptIdentifier);

}
