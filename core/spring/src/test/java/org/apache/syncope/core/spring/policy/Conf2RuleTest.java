/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.syncope.core.spring.policy;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.UnsupportedEncodingException;
import java.util.*;

import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.*;
import org.apache.syncope.core.persistence.api.entity.*;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.spring.security.DefaultPasswordGenerator;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.passay.CharacterRule;
import org.passay.LengthRule;
import org.passay.RepeatCharactersRule;
import org.passay.Rule;

import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.common.security.GuardedByteArray;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.passay.IllegalCharacterRule;
import org.passay.UsernameRule;



@RunWith(value=Parameterized.class)
public class Conf2RuleTest {
private int len;
private Character illegalChar;
private boolean isUserAllowed;
private DefaultPasswordRuleConf defConf;
private int count = 8;
private CharacterRule cr;
private LengthRule lr;
private IllegalCharacterRule icr;
private UsernameRule ur;
private RepeatCharactersRule rcr;
private List<Character> charList;
private char[] charToRet = new char[1];



    @Parameters
    public static Collection<Object[]> getTestParameters(){
        return Arrays.asList(new Object[][]{
//          | len | illegalChar | isUserAllowed |
            { 0   , null        , true           },
            { 2   , '!'         , false          },
        });
    }

    public Conf2RuleTest(int len, Character illegalChar, boolean isUserAllowed) {
        this.len = len;
        this.illegalChar = illegalChar;
        this.isUserAllowed = isUserAllowed;
    }

    @Before
    public void getPassSetUp(){
        defConf = new DefaultPasswordRuleConf();
        charList = new ArrayList<Character>();
        if(illegalChar != null){
             charList.add(illegalChar);
             defConf = spy(defConf);
             when(defConf.getIllegalChars()).thenReturn(charList);
             count = 9;
        }else{
            count = 1;
        }
        defConf.setMinLength(len);
        defConf.setMaxLength(len);
        defConf.setAlphabetical(len);
        defConf.setUppercase(len);
        defConf.setLowercase(len);
        defConf.setDigit(len);
        defConf.setSpecial(len);
        defConf.setRepeatSame(len);
        defConf.setUsernameAllowed(isUserAllowed);
    }


    @Test
    public void rulesTest(){
        List<Rule> ret = DefaultPasswordRule.conf2Rules(defConf);
        System.out.println(ret.toString());
        lr = (LengthRule)ret.get(0);
        assertEquals(count, ret.size());
        assertEquals(len, lr.getMinimumLength());
        if(len == 0){
            assertEquals(2147483647, lr.getMaximumLength());
        }else{
            assertEquals(len, lr.getMinimumLength());
            for(int i = 1; i<6; i++){
                cr = (CharacterRule)ret.get(i);
                assertEquals(len, cr.getNumberOfCharacters());
            }
            icr = (IllegalCharacterRule)ret.get(6);
            rcr = (RepeatCharactersRule)ret.get(7);
            ur = (UsernameRule)ret.get(8);
            charToRet = icr.getIllegalCharacters();
            assertEquals(illegalChar.charValue(), charToRet[0]);
            assertTrue(ur.isMatchBackwards());
            assertTrue(ur.isIgnoreCase());

        }

    }

}
