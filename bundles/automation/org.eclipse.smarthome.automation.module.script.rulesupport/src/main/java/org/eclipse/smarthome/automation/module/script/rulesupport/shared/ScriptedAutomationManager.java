/**
 * Copyright (c) 2015-2016 Simon Merschjohann and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.automation.module.script.rulesupport.shared;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.ScriptedCustomModuleHandlerFactory;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.ScriptedCustomModuleTypeProvider;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.ScriptedPrivateModuleHandlerFactory;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.factories.ScriptedActionHandlerFactory;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.factories.ScriptedConditionHandlerFactory;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.factories.ScriptedTriggerHandlerFactory;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple.SimpleActionHandler;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple.SimpleConditionHandler;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple.SimpleTriggerHandler;
import org.eclipse.smarthome.automation.type.ActionType;
import org.eclipse.smarthome.automation.type.ConditionType;
import org.eclipse.smarthome.automation.type.TriggerType;
import org.eclipse.smarthome.config.core.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Registry is used for a single ScriptEngine instance. It allows the adding and removing of handlers.
 * It allows the removal of previously added modules on unload.
 *
 * @author Simon Merschjohann
 *
 */
public class ScriptedAutomationManager {
    private static final Logger logger = LoggerFactory.getLogger(ScriptedAutomationManager.class);

    private RuleSupportRuleRegistryDelegate ruleRegistryDelegate;

    private HashSet<String> modules = new HashSet<>();
    private HashSet<String> moduleHandlers = new HashSet<>();
    private HashSet<String> privateHandlers = new HashSet<>();

    public ScriptedAutomationManager(RuleSupportRuleRegistryDelegate ruleRegistryDelegate) {
        this.ruleRegistryDelegate = ruleRegistryDelegate;
    }

    public void removeModuleType(String UID) {
        if (modules.remove(UID)) {
            ScriptedCustomModuleTypeProvider.get().removeModuleType(UID);
            removeHandler(UID);
        }
    }

    public void removeHandler(String typeUID) {
        if (moduleHandlers.remove(typeUID)) {
            ScriptedCustomModuleHandlerFactory.get().removeModuleHandler(typeUID);
        }
    }

    public void removePrivateHandler(String privId) {
        if (privateHandlers.remove(privId)) {
            ScriptedPrivateModuleHandlerFactory.get().removeHandler(privId);
        }
    }

    public void removeAll() {
        logger.info("removeAll added handlers");

        HashSet<String> types = new HashSet<>(modules);
        for (String moduleType : types) {
            removeModuleType(moduleType);
        }

        HashSet<String> moduleHandlers = new HashSet<>(this.moduleHandlers);
        for (String uid : moduleHandlers) {
            removeHandler(uid);
        }

        HashSet<String> privateHandlers = new HashSet<>(this.privateHandlers);
        for (String privId : privateHandlers) {
            removePrivateHandler(privId);
        }

        ruleRegistryDelegate.removeAllAddedByScript();
    }

    public Rule addRule(RuleClassInterface element) {
        String uid = element.getUid() != null ? element.getUid() : getUniqueId(element);
        Rule rule = new Rule(uid);

        String name = element.getName();
        if (name == null || name.isEmpty()) {
            name = element.getClass().toString();
        }

        rule.setName(name);
        rule.setDescription(element.getDescription());

        try {
            ArrayList<Condition> conditions = new ArrayList<>();
            for (Condition cond : element.getConditions()) {
                Condition toAdd = cond;
                if (cond.getId() == null || cond.getId().isEmpty()) {
                    toAdd = new Condition(getUniqueId(cond), cond.getTypeUID(), cond.getConfiguration(),
                            cond.getInputs());
                }

                conditions.add(toAdd);
            }

            rule.setConditions(conditions);
        } catch (Exception ex) {
            // conditions are optional
        }

        try {
            ArrayList<Trigger> triggers = new ArrayList<>();
            for (Trigger trigger : element.getTriggers()) {
                Trigger toAdd = trigger;
                if (trigger.getId() == null || trigger.getId().isEmpty()) {
                    toAdd = new Trigger(getUniqueId(trigger), trigger.getTypeUID(), trigger.getConfiguration());
                }

                triggers.add(toAdd);
            }

            rule.setTriggers(triggers);
        } catch (Exception ex) {
            // triggers are optional
        }

        ArrayList<Action> actions = new ArrayList<>();
        actions.addAll(element.getActions());

        if (element instanceof SimpleActionHandler) {
            String privId = addPrivateActionHandler((SimpleActionHandler) element);

            Action scriptedAction = new Action(getUniqueId(element), "jsr223.ScriptedAction", new Configuration(),
                    null);
            scriptedAction.getConfiguration().put("privId", privId);
            actions.add(scriptedAction);
        }

        rule.setActions(actions);
        ruleRegistryDelegate.add(rule);

        return rule;
    }

    private String getUniqueId(Object element) {
        return element.getClass().getSimpleName().replace("$", "_").replaceAll(" ", "") + "_" + UUID.randomUUID();
    }

    public void addConditionType(ConditionType condititonType) {
        modules.add(condititonType.getUID());
        ScriptedCustomModuleTypeProvider.get().addModuleType(condititonType);
    }

    public void addConditionHandler(String uid, ScriptedConditionHandlerFactory conditionHandler) {
        moduleHandlers.add(uid);
        ScriptedCustomModuleHandlerFactory.get().addModuleHandler(uid, conditionHandler);
    }

    public void addConditionHandler(String uid, SimpleConditionHandler conditionHandler) {
        moduleHandlers.add(uid);
        ScriptedCustomModuleHandlerFactory.get().addModuleHandler(uid, conditionHandler);
    }

    public String addPrivateConditionHandler(SimpleConditionHandler conditionHandler) {
        String uid = ScriptedPrivateModuleHandlerFactory.get().addHandler(conditionHandler);
        privateHandlers.add(uid);
        return uid;
    }

    public void addActionType(ActionType actionType) {
        modules.add(actionType.getUID());
        ScriptedCustomModuleTypeProvider.get().addModuleType(actionType);
    }

    public void addActionHandler(String uid, ScriptedActionHandlerFactory actionHandler) {
        moduleHandlers.add(uid);
        ScriptedCustomModuleHandlerFactory.get().addModuleHandler(uid, actionHandler);
    }

    public void addActionHandler(String uid, SimpleActionHandler actionHandler) {
        moduleHandlers.add(uid);
        ScriptedCustomModuleHandlerFactory.get().addModuleHandler(uid, actionHandler);
    }

    public String addPrivateActionHandler(SimpleActionHandler actionHandler) {
        String uid = ScriptedPrivateModuleHandlerFactory.get().addHandler(actionHandler);
        privateHandlers.add(uid);
        return uid;
    }

    public void addTriggerType(TriggerType triggerType) {
        modules.add(triggerType.getUID());
        ScriptedCustomModuleTypeProvider.get().addModuleType(triggerType);
    }

    public void addTriggerHandler(String uid, ScriptedTriggerHandlerFactory triggerHandler) {
        moduleHandlers.add(uid);
        ScriptedCustomModuleHandlerFactory.get().addModuleHandler(uid, triggerHandler);
    }

    public String addPrivateTriggerHandler(SimpleTriggerHandler triggerHandler) {
        String uid = ScriptedPrivateModuleHandlerFactory.get().addHandler(triggerHandler);
        privateHandlers.add(uid);
        return uid;
    }
}
