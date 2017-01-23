/**
 * Copyright (c) 1997, 2015 by ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.automation.module.script.internal.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.handler.ActionHandler;
import org.eclipse.smarthome.automation.module.script.ScriptEngineContainer;
import org.eclipse.smarthome.automation.module.script.ScriptEngineManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This handler can execute script actions.
 *
 * @author Kai Kreuzer - Initial contribution and API
 *
 */
public class ScriptActionHandler extends AbstractScriptModuleHandler<Action> implements ActionHandler {

    private final Logger logger = LoggerFactory.getLogger(ScriptActionHandler.class);

    public static final String SCRIPT_ACTION_ID = "script.ScriptAction";

    /**
     * constructs a new ScriptActionHandler
     *
     * @param module
     */
    public ScriptActionHandler(Action module, ScriptEngineManager scriptEngineManager) {
        super(module, scriptEngineManager);
    }

    @Override
    public void dispose() {
    }

    @Override
    public Map<String, Object> execute(Map<String, ?> context) {
        Object type = module.getConfiguration().get(SCRIPT_TYPE);
        Object script = module.getConfiguration().get(SCRIPT);
        if (type instanceof String) {
            if (script instanceof String) {
                String identifier = UUID.randomUUID().toString();
                ScriptEngineContainer container = scriptEngineManager.createScriptEngine((String) type, identifier);
                if (container != null) {
                    ScriptEngine engine = container.getScriptEngine();
                    ScriptContext executionContext = getExecutionContext(engine, context);
                    try {
                        Object result = engine.eval((String) script, executionContext);
                        HashMap<String, Object> resultMap = new HashMap<String, Object>();
                        resultMap.put("result", result);
                        return resultMap;
                    } catch (ScriptException e) {
                        logger.error("Script execution failed: {}", e.getMessage());
                    }

                    scriptEngineManager.removeEngine(identifier);
                } else {
                    logger.debug("No engine available for script type '{}' in action '{}'.",
                            new Object[] { type, module.getId() });
                }
            } else {
                logger.debug("Script is missing in the configuration of action '{}'.", module.getId());
            }
        } else {
            logger.debug("Script type is missing in the configuration of action '{}'.", module.getId());
        }
        return null;
    }

}
