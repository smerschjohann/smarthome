package org.eclipse.smarthome.automation.module.script.rulesupport.internal.factory;

import org.eclipse.smarthome.automation.module.script.rulesupport.internal.handler.ScriptedHandler;
import org.eclipse.smarthome.automation.type.ModuleType;

public interface IScriptedModuleHandlerFactory {

    void addModuleHandler(String uid, ScriptedHandler scriptedHandler);

    void addModule(ModuleType moduleType, ScriptedHandler scriptedHandler);

    void removeModule(String UID);

    String addHandler(String privId, ScriptedHandler scriptedHandler);

    String addHandler(ScriptedHandler scriptedHandler);

    void removeHandler(String privId);

    void addModuleType(ModuleType moduleType);

}
