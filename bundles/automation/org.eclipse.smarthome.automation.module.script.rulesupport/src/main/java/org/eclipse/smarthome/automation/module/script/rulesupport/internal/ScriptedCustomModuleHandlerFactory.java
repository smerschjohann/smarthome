/**
 * Copyright (c) 2015-2016 Simon Merschjohann and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.automation.module.script.rulesupport.internal;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Module;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.handler.BaseModuleHandlerFactory;
import org.eclipse.smarthome.automation.handler.ModuleHandler;
import org.eclipse.smarthome.automation.handler.ModuleHandlerFactory;
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
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ModuleHandlerFactory to provide types for "private" scripted Actions and Conditions
 *
 * @author Simon Merschjohann
 *
 */
public class ScriptedCustomModuleHandlerFactory extends BaseModuleHandlerFactory {
    private Logger logger = LoggerFactory.getLogger(ScriptedCustomModuleHandlerFactory.class);

    private final HashMap<String, ScriptedHandler> typesHandlers = new HashMap<>();

    private ServiceRegistration<?> bmhfReg;

    private static ScriptedCustomModuleHandlerFactory instance;

    public static ScriptedCustomModuleHandlerFactory get() {
        return instance;
    }

    @Override
    public void activate(BundleContext bundleContext) {
        super.activate(bundleContext);

        bmhfReg = bundleContext.registerService(ModuleHandlerFactory.class.getName(), this, null);

        instance = this;
    }

    @Override
    public Collection<String> getTypes() {
        return typesHandlers.keySet();
    }

    @Override
    protected ModuleHandler internalCreate(Module module, String ruleUID) {
        ModuleHandler moduleHandler = null;

        ScriptedHandler scriptedHandler = typesHandlers.get(module.getTypeUID());

        if (scriptedHandler != null) {
            if (scriptedHandler instanceof SimpleActionHandler) {
                moduleHandler = new SimpleActionHandlerDelegate((Action) module, (SimpleActionHandler) scriptedHandler);
            } else if (scriptedHandler instanceof SimpleConditionHandler) {
                moduleHandler = new SimpleConditionHandlerDelegate((Condition) module,
                        (SimpleConditionHandler) scriptedHandler);
            } else if (scriptedHandler instanceof SimpleTriggerHandler) {
                moduleHandler = new SimpleTriggerHandlerDelegate((Trigger) module,
                        (SimpleTriggerHandler) scriptedHandler);
            } else if (scriptedHandler instanceof ScriptedActionHandlerFactory) {
                moduleHandler = ((ScriptedActionHandlerFactory) scriptedHandler).get((Action) module);
            } else if (scriptedHandler instanceof ScriptedTriggerHandlerFactory) {
                moduleHandler = ((ScriptedTriggerHandlerFactory) scriptedHandler).get((Trigger) module);
            } else if (scriptedHandler instanceof ScriptedConditionHandlerFactory) {
                moduleHandler = ((ScriptedConditionHandlerFactory) scriptedHandler).get((Condition) module);
            } else {
                logger.error("Not supported moduleHandler: {}", module.getTypeUID());
            }
        }

        return moduleHandler;
    }

    public void addModuleHandler(String uid, ScriptedHandler scriptedHandler) {
        typesHandlers.put(uid, scriptedHandler);

        updateModuleHandlerOffer();
    }

    public void removeModuleHandler(String uid) {
        typesHandlers.remove(uid);

        updateModuleHandlerOffer();
    }

    private void updateModuleHandlerOffer() {
        Dictionary<String, Object> properties = new Hashtable<String, Object>();

        properties.put("handlers", typesHandlers.values());
        bmhfReg.setProperties(properties);
    }
}
