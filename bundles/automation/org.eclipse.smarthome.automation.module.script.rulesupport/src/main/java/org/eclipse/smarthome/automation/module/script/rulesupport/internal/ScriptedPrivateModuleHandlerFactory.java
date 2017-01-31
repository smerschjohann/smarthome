/**
 * Copyright (c) 2015-2016 Simon Merschjohann and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.automation.module.script.rulesupport.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.handler.BaseModuleHandlerFactory;
import org.eclipse.smarthome.automation.handler.ModuleHandler;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.delegates.SimpleActionHandlerDelegate;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.delegates.SimpleConditionHandlerDelegate;
import org.eclipse.smarthome.automation.module.script.rulesupport.internal.delegates.SimpleTriggerHandlerDelegate;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.ScriptedHandler;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.factories.ScriptedActionHandlerFactory;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.factories.ScriptedConditionHandlerFactory;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.factories.ScriptedTriggerHandlerFactory;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple.SimpleActionHandler;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple.SimpleConditionHandler;
import org.eclipse.smarthome.automation.module.script.rulesupport.shared.simple.SimpleTriggerHandler;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ModuleHandlerFactory to provide types for "private" scripted Actions and Conditions
 *
 * @author Simon Merschjohann
 *
 */
public class ScriptedPrivateModuleHandlerFactory extends BaseModuleHandlerFactory {
    private static final String PRIV_ID = "privId";

    private Logger logger = LoggerFactory.getLogger(ScriptedPrivateModuleHandlerFactory.class);

    private static final Collection<String> TYPES = Arrays.asList("jsr223.ScriptedAction", "jsr223.ScriptedCondition",
            "jsr223.ScriptedTrigger");

    private final HashMap<String, ScriptedHandler> privateTypes = new HashMap<>();

    private int nextId = 0;

    private static ScriptedPrivateModuleHandlerFactory instance;

    public static ScriptedPrivateModuleHandlerFactory get() {
        return instance;
    }

    @Override
    public void activate(BundleContext bundleContext) {
        ScriptedPrivateModuleHandlerFactory.instance = this;
    }

    @Override
    public Collection<String> getTypes() {
        return TYPES;
    }

    @Override
    protected ModuleHandler internalCreate(Module module, String ruleUID) {
        ModuleHandler moduleHandler = null;

        ScriptedHandler scriptedHandler = null;
        try {
            scriptedHandler = privateTypes.get(module.getConfiguration().get(PRIV_ID));
        } catch (Exception e) {
            logger.warn("ScriptedHandler {} for ruleUID {} not found", module.getConfiguration().get(PRIV_ID), ruleUID);
        }

        if (scriptedHandler instanceof SimpleActionHandler) {
            moduleHandler = new SimpleActionHandlerDelegate((Action) module, (SimpleActionHandler) scriptedHandler);
        } else if (scriptedHandler instanceof SimpleConditionHandler) {
            moduleHandler = new SimpleConditionHandlerDelegate((Condition) module,
                    (SimpleConditionHandler) scriptedHandler);
        } else if (scriptedHandler instanceof SimpleTriggerHandler) {
            moduleHandler = new SimpleTriggerHandlerDelegate((Trigger) module, (SimpleTriggerHandler) scriptedHandler);
        } else if (scriptedHandler instanceof ScriptedActionHandlerFactory) {
            moduleHandler = ((ScriptedActionHandlerFactory) scriptedHandler).get((Action) module);
        } else if (scriptedHandler instanceof ScriptedTriggerHandlerFactory) {
            moduleHandler = ((ScriptedTriggerHandlerFactory) scriptedHandler).get((Trigger) module);
        } else if (scriptedHandler instanceof ScriptedConditionHandlerFactory) {
            moduleHandler = ((ScriptedConditionHandlerFactory) scriptedHandler).get((Condition) module);
        } else {
            logger.error("Not supported moduleHandler: {}", module.getTypeUID());
        }

        return moduleHandler;
    }

    public String addHandler(String privId, ScriptedHandler scriptedHandler) {
        privateTypes.put(privId, scriptedHandler);
        return privId;
    }

    public String addHandler(ScriptedHandler scriptedHandler) {
        String privId = "i" + (nextId++);
        privateTypes.put(privId, scriptedHandler);
        return privId;
    }

    public void removeHandler(String privId) {
        privateTypes.remove(privId);
    }
}
