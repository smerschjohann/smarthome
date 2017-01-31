package org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple;

import java.util.Map;

import org.eclipse.smarthome.automation.handler.RuleEngineCallback;

public interface SimpleRuleEngineCallback extends RuleEngineCallback {
    public void triggered(Map<String, ?> context);
}
