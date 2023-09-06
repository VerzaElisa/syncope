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
import org.passay.PasswordData;
import org.passay.PasswordValidator;
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

public class EnforcesTest {
public enum PassRuleType {
   DUMMY,
   REAL
}

private int len;
private boolean isUserAllowed = false;
private DefaultPasswordRuleConf defConf;
private int count = 9;
private CharacterRule cr;
private LengthRule lr;
private IllegalCharacterRule icr;
private UsernameRule ur;
private RepeatCharactersRule rcr;
private List<Character> illegalList = Arrays.asList('!');
private char[] charToRet = new char[1];
private DefaultPasswordRule dpr;
private List<Character> special = Arrays.asList('@');
private Set<String> notPermittedList;
private PassRuleType type;
private String ex = null;
private String exception;
private String username;
private String clear;
private String notPermitted;
private int times = 0;
//                                         | minLen | maxLen | alpha | lower | upper | digit | special | same |
private List<Integer> param = Arrays.asList( 8      , 8      , 4     , 2     , 2     , 3     , 1       , 8    );



    @Parameters
    public static Collection<Object[]> getTestParameters(){
        return Arrays.asList(new Object[][]{
//          | exception                  | clear      | notPermitted | username   |
            { "PasswordPolicyException"  , "o"        , ""           , null       },
            { "PasswordPolicyException"  , "AAaa234@" , "AAaa234@"   , "testUser" },
            { null                       , "AAaa234@" , "test"       , "testUser" },
            { "PasswordPolicyException"  , "AAaa234@" , "test"       , "AAaa234@" },
        });
    }

    public EnforcesTest(String exception, String clear, String notPermitted, String username) {
        this.exception = exception;
        this.clear = clear;
        this.notPermitted = notPermitted;
        this.username = username;
    }

    @Before
    public void enforceOneSetUp(){
        dpr = new DefaultPasswordRule();
        Utility.createListRule(param, 0, 1, illegalList, special, isUserAllowed);
        dpr.passwordValidator = new PasswordValidator(Utility.getRule().toArray(new Rule[0]));
        dpr.passwordValidator = spy(dpr.passwordValidator);
        notPermittedList = Utility.getSetWords(notPermitted);
        notPermittedList = spy(notPermittedList);
        if(clear != username && clear.equals("AAaa234@")){
            times = 1;
        }
    }

    @Before
    public void enforceTwoSetUp(){
        List<String> np = Arrays.asList("ciao", "test", "java");
        defConf = spy(DefaultPasswordRuleConf.class);
        when(defConf.getSchemasNotPermitted()).thenReturn(np);
    }

    @Test
    public void enforceTest(){
        try{
            dpr.enforce(clear, username, notPermittedList);
        }catch(Exception e){
            ex = e.getClass().getSimpleName();
            e.printStackTrace();
        }finally{
            verify(dpr.passwordValidator).validate(any(PasswordData.class));
            verify(notPermittedList, times(times)).stream();
            assertEquals(exception, ex);
        }
    }
}
