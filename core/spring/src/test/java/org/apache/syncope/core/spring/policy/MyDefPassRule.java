package org.apache.syncope.core.spring.policy;

import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;

public class MyDefPassRule extends DefaultPasswordRuleConf{

    private int upper;
    private int lower;

    public int getUpperNum() {
        return upper;
    }

    public void setUpperNum(final int upper) {
        this.upper = upper;
    }

    public int getLowerNum() {
        return lower;
    }

    public void setLowerNum(final int lower) {
        this.lower = lower;
    }

}
