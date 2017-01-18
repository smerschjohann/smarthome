/**
 * Copyright (c) 2015-2016 Simon Merschjohann and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.automation.module.script.rulesupport.internal.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.eclipse.smarthome.automation.Rule;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.osgi.framework.ServiceRegistration;

/**
 * This RuleProvider keeps Rules at added by scripts during the runtime. This ensures that Rules are not kept on reboot,
 * but have to be added by the scripts again.
 *
 * @author Simon Merschjohann
 *
 */
public class ScriptedRuleProvider implements IScriptedRuleProvider {
    @SuppressWarnings("rawtypes")
    private ServiceRegistration providerReg;
    private Collection<ProviderChangeListener<Rule>> listeners = new ArrayList<ProviderChangeListener<Rule>>();

    HashMap<String, Rule> rules = new HashMap<>();

    @Override
    public void addProviderChangeListener(ProviderChangeListener<Rule> listener) {
        listeners.add(listener);
    }

    @Override
    public Collection<Rule> getAll() {
        return rules.values();
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<Rule> listener) {
        listeners.remove(listener);
    }

    @Override
    public void addRule(Rule rule) {
        rules.put(rule.getUID(), rule);

        for (ProviderChangeListener<Rule> providerChangeListener : listeners) {
            providerChangeListener.added(this, rule);
        }
    }

    @Override
    public void removeRule(String ruleUID) {
        removeRule(rules.get(ruleUID));
    }

    @Override
    public void removeRule(Rule rule) {
        for (ProviderChangeListener<Rule> providerChangeListener : listeners) {
            providerChangeListener.removed(this, rule);
        }
    }

}
