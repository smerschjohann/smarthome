/**
 * Copyright (c) 2015-2016 Simon Merschjohann and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.automation.module.script.rulesupport.shared;

import java.util.List;

import org.eclipse.smarthome.automation.Action;
import org.eclipse.smarthome.automation.Condition;
import org.eclipse.smarthome.automation.Trigger;
import org.eclipse.smarthome.automation.Visibility;

/**
 * This class should be implemented by a script to provide a Rule
 *
 * @author Simon Merschjohann
 *
 */
public interface RuleClassInterface {
    List<Trigger> getTriggers();

    List<Condition> getConditions();

    List<Action> getActions();

    String getName();

    String getUid();

    String getDescription();

    Visibility getVisibility();
}
