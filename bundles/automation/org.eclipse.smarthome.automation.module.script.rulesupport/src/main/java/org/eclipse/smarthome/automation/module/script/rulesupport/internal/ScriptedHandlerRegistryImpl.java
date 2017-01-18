/**
 * Copyright (c) 2015-2016 Simon Merschjohann and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.automation.module.script.rulesupport.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.automation.RuleRegistry;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.factory.IScriptedModuleHandlerFactory;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.shared.IScriptedRuleProvider;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.shared.LoaderRuleRegistry;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.shared.RuleClassInterface;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.shared.ScriptedHandlerRegistry;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.shared.modulehandler.ScriptedActionHandlerFactory;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.shared.modulehandler.ScriptedConditionHandlerFactory;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.shared.modulehandler.ScriptedTriggerHandlerFactory;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.shared.simple.SimpleActionHandler;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.shared.simple.SimpleConditionHandler;
import org.eclipse.smarthome.automation.type.ActionType;
import org.eclipse.smarthome.automation.type.ConditionType;
import org.eclipse.smarthome.automation.type.ModuleType;
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
public class ScriptedHandlerRegistryImpl implements ScriptedHandlerRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ScriptedHandlerRegistryImpl.class);

    private LoaderRuleRegistry ruleRegistry;

    private IScriptedModuleHandlerFactory scriptedModuleHandlerFactory;

    private HashSet<ModuleType> modules = new HashSet<>();
    private HashSet<String> moduleHandlers = new HashSet<>();
    private HashSet<String> privateHandlers = new HashSet<>();

    public ScriptedHandlerRegistryImpl(RuleRegistry ruleRegistry,
            IScriptedModuleHandlerFactory scriptedModuleHandlerFactory, IScriptedRuleProvider ruleProvider) {
        this.ruleRegistry = new LoaderRuleRegistry(ruleRegistry, ruleProvider);
        this.scriptedModuleHandlerFactory = scriptedModuleHandlerFactory;
    }

    @Override
    public void removeModule(String UID) {
        if (modules.remove(UID)) {
            scriptedModuleHandlerFactory.removeModule(UID);
        }
    }

    @Override
    public void removeHandler(String privId) {
        if (privateHandlers.remove(privId)) {
            scriptedModuleHandlerFactory.removeHandler(privId);
        }
    }

    public void removeAll() {
        logger.info("removeAll added handlers");

        for (ModuleType moduleType : modules) {
            scriptedModuleHandlerFactory.removeModule(moduleType.getUID());
        }
        modules.clear();

        for (String uid : moduleHandlers) {
            scriptedModuleHandlerFactory.removeModule(uid);
        }
        moduleHandlers.clear();

        for (String privId : privateHandlers) {
            scriptedModuleHandlerFactory.removeHandler(privId);
        }
        privateHandlers.clear();

        ruleRegistry.removeAllAddedByScript();
    }

    public LoaderRuleRegistry getRuleRegistry() {
        return ruleRegistry;
    }

    @Override
    public Rule addRule(RuleClassInterface element) {
        String uid = element.getUid() != null ? element.getUid()
                : element.getClass().getSimpleName() + "_" + UUID.randomUUID();
        Rule rule = new Rule(uid);

        try {
            rule.setConditions(element.getConditions());
        } catch (Exception ex) {
            // conditions are optional
        }

        rule.setTriggers(element.getTriggers());

        ArrayList<Action> actions = new ArrayList<>();
        actions.addAll(element.getActions());

        if (element instanceof SimpleActionHandler) {
            String privId = addPrivateActionHandler((SimpleActionHandler) element);

            Action scriptedAction = new Action(UUID.randomUUID().toString(), "ScriptedAction", new Configuration(),
                    null);
            scriptedAction.getConfiguration().put("privId", privId);
            actions.add(scriptedAction);
        }

        rule.setActions(actions);
        ruleRegistry.add(rule);

        return rule;
    }

    @Override
    public void addConditionType(ConditionType condititonType) {
        modules.add(condititonType);
        scriptedModuleHandlerFactory.addModuleType(condititonType);
    }

    @Override
    public void addConditionHandler(String uid, ScriptedConditionHandlerFactory conditionHandler) {
        moduleHandlers.add(uid);
        this.scriptedModuleHandlerFactory.addModuleHandler(uid, conditionHandler);
    }

    @Override
    public void addConditionHandler(String uid, SimpleConditionHandler conditionHandler) {
        moduleHandlers.add(uid);
        this.scriptedModuleHandlerFactory.addModuleHandler(uid, conditionHandler);
    }

    @Override
    public String addPrivateConditionHandler(SimpleConditionHandler conditionHandler) {
        String uid = this.scriptedModuleHandlerFactory.addHandler(conditionHandler);
        privateHandlers.add(uid);
        return uid;
    }

    @Override
    public void addActionType(ActionType actionType) {
        modules.add(actionType);
        scriptedModuleHandlerFactory.addModuleType(actionType);
    }

    @Override
    public void addActionHandler(String uid, ScriptedActionHandlerFactory actionHandler) {
        moduleHandlers.add(uid);
        this.scriptedModuleHandlerFactory.addModuleHandler(uid, actionHandler);
    }

    @Override
    public void addActionHandler(String uid, SimpleActionHandler actionHandler) {
        moduleHandlers.add(uid);
        this.scriptedModuleHandlerFactory.addModuleHandler(uid, actionHandler);
    }

    @Override
    public String addPrivateActionHandler(SimpleActionHandler actionHandler) {
        String uid = this.scriptedModuleHandlerFactory.addHandler(actionHandler);
        privateHandlers.add(uid);
        return uid;
    }

    @Override
    public void addTriggerType(TriggerType triggerType) {
        modules.add(triggerType);
        scriptedModuleHandlerFactory.addModuleType(triggerType);
    }

    @Override
    public void addTriggerHandler(String uid, ScriptedTriggerHandlerFactory triggerHandler) {
        moduleHandlers.add(uid);
        this.scriptedModuleHandlerFactory.addModuleHandler(uid, triggerHandler);
    }
}
