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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.passay.CharacterRule;
import org.passay.LengthRule;
import org.passay.Rule;

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
private char[] charToRet = new char[1];
private List<Character> special = Arrays.asList('@');
private String error = "INSUFFICIENT_SPECIAL";




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
        defConf = spy(DefaultPasswordRuleConf.class);
        defConf = Utility.createDef(defConf, len, 0, 0, isUserAllowed, false, illegalChar, special);
        if(illegalChar != null){
             count = 9;
        }else{
            count = 1;
        }
    }

    @Test
    public void rulesTest(){
        List<Rule> ret = DefaultPasswordRule.conf2Rules(defConf);
        lr = (LengthRule)ret.get(0);
        assertEquals(count, ret.size());
        assertEquals(len, lr.getMinimumLength());
        if(len == 0){
            verify(defConf).getMinLength();
            assertEquals(2147483647, lr.getMaximumLength());
        }else{
            verify(defConf, times(2)).getMinLength();
            assertEquals(len, lr.getMinimumLength());
            assertEquals(len, lr.getMaximumLength());
            
            for(int i = 1; i<6; i++){
                cr = (CharacterRule)ret.get(i);
                assertEquals(len, cr.getNumberOfCharacters());
                if(i == 5){
                    assertEquals(error, cr.getCharacterData().getErrorCode());
                    assertEquals(special.get(0).toString(), cr.getCharacterData().getCharacters());
                }
            }
            icr = (IllegalCharacterRule)ret.get(6);
            ur = (UsernameRule)ret.get(8);
            charToRet = icr.getIllegalCharacters();
            assertEquals(illegalChar.charValue(), charToRet[0]);
            assertTrue(ur.isMatchBackwards());
            assertTrue(ur.isIgnoreCase());

        }
    }
}
