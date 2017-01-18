package org.eclipse.smarthome.automation.module.script.rulesupport.internal.shared;

import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.RuleProvider;

public interface IScriptedRuleProvider extends RuleProvider {

    void removeRule(String ruleUID);

    void addRule(Rule rule);

    void removeRule(Rule rule);

}
