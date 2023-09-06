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

public class SetConfTest {
public enum PassRuleType {
   DUMMY,
   REAL
}

private int len;
private Character illegalChar;
private boolean isUserAllowed;
private DefaultPasswordRuleConf defConf;
private int count = 9;
private CharacterRule cr;
private LengthRule lr;
private IllegalCharacterRule icr;
private UsernameRule ur;
private RepeatCharactersRule rcr;
private List<Character> charList;
private char[] charToRet = new char[1];
private DefaultPasswordRule dpr = new DefaultPasswordRule();;
private List<Character> special = Arrays.asList('@');
private PassRuleType type;
private String ex = null;
private String exception;

//                                         | minLen | maxLen | alpha | lower | upper | digit | special | same |
private List<Integer> param = Arrays.asList( 8      , 8      , 4     , 2     , 2     , 3     , 1       , 8    );



    @Parameters
    public static Collection<Object[]> getTestParameters(){
        return Arrays.asList(new Object[][]{
//          | len | illegalChar | isUserAllowed | type               | exception                 |
            { 2   , '!'         , false         , PassRuleType.REAL  , null                      },
            { 2   , '!'         , false         , PassRuleType.DUMMY , "IllegalArgumentException"},
        });
    }

    public SetConfTest(int len, Character illegalChar, boolean isUserAllowed, PassRuleType type, String exception) {
        this.len = len;
        this.illegalChar = illegalChar;
        this.isUserAllowed = isUserAllowed;
        this.type = type;
        this.exception = exception;
    }

    @Before
    public void setConfSetUp(){
        switch(type){
            case REAL:
                defConf = new MyDefPassRule();
                defConf = Utility.createDef(defConf, len, 0, 5, isUserAllowed, true, illegalChar, special);
                dpr.setConf(defConf);
                break;
            case DUMMY:
                DummyPasswordRuleConf dummyDefConf = new DummyPasswordRuleConf();
                try{
                   dpr.setConf(dummyDefConf);
                }catch(Exception e){
                    ex = e.getClass().getSimpleName();
                    e.printStackTrace();
                }
                break;
        }
    }

    @Test
    public void setConfTest(){
        assertEquals(defConf, dpr.conf);
        assertEquals(ex, exception);
//        assertEquals(Utility.getRule().size(), dpr.passwordValidator.getRules().size());
    }
}
