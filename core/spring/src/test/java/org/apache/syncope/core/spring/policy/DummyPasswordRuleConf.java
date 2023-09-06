package org.apache.syncope.core.spring.policy;

import org.apache.syncope.common.lib.policy.PasswordRuleConf;

public class DummyPasswordRuleConf implements PasswordRuleConf{
    
    private String rule;

    @Override
    public String getName() {
        return rule;
    }

    public void setName(String ruleName){
        this.rule = ruleName;
    }
    
}
