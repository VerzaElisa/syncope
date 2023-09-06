package org.apache.syncope.core.spring.policy;

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.CharacterData;
import org.passay.IllegalCharacterRule;
import org.passay.LengthRule;
import org.passay.NumberRangeRule;
import org.passay.RepeatCharactersRule;
import org.passay.Rule;
import org.passay.UsernameRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utility {
    private static Set<Rule> myRules = new HashSet<>();

    public static DefaultPasswordRuleConf createDef(DefaultPasswordRuleConf defConf, int len, int lower, int upper, boolean user, boolean newPol, Character illegal, List<Character> special){
        List<Character> charList = new ArrayList<Character>();
        List<Integer> paramList = new ArrayList<Integer>();
        defConf = spy(defConf);

        if(illegal != null){
             charList.add(illegal);
             when(defConf.getIllegalChars()).thenReturn(charList);
        }
        defConf.setMinLength(len);
        defConf.setMaxLength(len);
        defConf.setAlphabetical(len);
        defConf.setUppercase(len);
        defConf.setLowercase(len);
        defConf.setDigit(len);
        defConf.setSpecial(len);
        when(defConf.getSpecialChars()).thenReturn(special);
        defConf.setRepeatSame(len);
        defConf.setUsernameAllowed(user);
        if(newPol){
            for(int i = 0; i<8; i++){
                paramList.add(len);
            }
            createListRule(paramList, lower, upper, charList, special, user);
            MyDefPassRule mydpr = new MyDefPassRule();
            mydpr = (MyDefPassRule) defConf;
            mydpr.setLowerNum(lower);
            mydpr.setUpperNum(upper);
            return mydpr;
        }
        return defConf;
    }

    public static void createListRule(List<Integer> len, int lower, int upper, List<Character> illegalList, List<Character> specialList, boolean user){
        LengthRule lengthRule = new LengthRule();
        lengthRule.setMinimumLength(len.get(0));
        lengthRule.setMaximumLength(len.get(1));
        myRules.add(lengthRule);
        myRules.add(new CharacterRule(EnglishCharacterData.Alphabetical, len.get(2)));
        myRules.add(new CharacterRule(EnglishCharacterData.UpperCase, len.get(3)));
        myRules.add(new CharacterRule(EnglishCharacterData.LowerCase, len.get(4)));
        myRules.add(new CharacterRule(EnglishCharacterData.Digit, len.get(5)));
        myRules.add(new CharacterRule(new CharacterData() {

                @Override
                public String getErrorCode() {
                    return "INSUFFICIENT_SPECIAL";
                }

                @Override
                public String getCharacters() {
                    return new String(ArrayUtils.toPrimitive(specialList.toArray(Character[]::new)));
                }
            }, len.get(6)));
        myRules.add(new IllegalCharacterRule(ArrayUtils.toPrimitive(illegalList.toArray(Character[]::new))));
        myRules.add(new RepeatCharactersRule(len.get(7)));
        if((!user)){
            myRules.add(new UsernameRule(true, true));
        }
        myRules.add(new NumberRangeRule(lower, upper));
    }

    public static Set<Rule> getRule(){
        return myRules;
    }

    public static Set<String> getSetWords(String words){
        String[] listRet = words.split(" ");
        return new HashSet<String>(Arrays.asList(listRet));
    }
}
    
