package org.eclipse.smarthome.automation.module.script.rulesupport.internal.delegates;

import java.util.Map;

import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.handler.RuleEngineCallback;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple.SimpleRuleEngineCallback;

public class SimpleRuleEngineCallbackDelegate implements SimpleRuleEngineCallback {
    private Trigger trigger;
    private RuleEngineCallback callback;

    public SimpleRuleEngineCallbackDelegate(Trigger trigger, RuleEngineCallback callback) {
        this.trigger = trigger;
        this.callback = callback;
    }

    @Override
    public void triggered(Trigger trigger, Map<String, ?> context) {
        callback.triggered(trigger, context);
    }

    @Override
    public void triggered(Map<String, ?> context) {
        callback.triggered(this.trigger, context);
    }
}
