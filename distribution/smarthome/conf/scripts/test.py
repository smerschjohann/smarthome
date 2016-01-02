print ScriptExtension.presets

ScriptExtension.importPreset("RuleSupport")
ScriptExtension.importPreset("RuleSimple")

print dir()
def itemStateChangeTrigger(triggername, itemName):
    return Trigger(triggername, "ItemStateChangeTrigger", Configuration({
        "itemName": itemName
    }))

import org.eclipse.smarthome.automation.handler.ActionHandler as ActionHandler

class MyAction(ActionHandler):
    def __init__(self, module):
        self.module = module

    def dispose(self):
        pass

    def execute(self, context):
        print "received context", context
        return {"result": "awesome"}


import org.eclipse.smarthome.automation.type.ActionType as ActionType
import org.eclipse.smarthome.config.core.ConfigDescriptionParameter as ConfigDescriptionParameter

HandlerRegistry.addActionType(ActionType("PyTestAction", [ConfigDescriptionParameter("string",ConfigDescriptionParameter.Type.TEXT)], []))
HandlerRegistry.addActionHandler("PyTestAction", lambda module: MyAction(module))

# SimpleRule can combine any TriggerType and ConditionType registered to the RuleEngine
class MySimpleRule(SimpleRule):
    def __init__(self):
        config = Configuration({"string": "working?"})

        config.put("string2", "working?")

        print config
        self.actions.add(Action("test", "PyTestAction", config, None))
        self.triggers = [itemStateChangeTrigger("trigger1", "DemoSwitch")]

    def execute(self, module, inputs):
        print "module:", module
        print "configuration:", module.configuration
        print "inputs:", inputs

        print dir(inputs)
        for k, v in inputs.iteritems():
            print "key:", k, "value:", v


HandlerRegistry.addRule(MySimpleRule())
