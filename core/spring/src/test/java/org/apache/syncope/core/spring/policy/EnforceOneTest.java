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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.passay.PasswordData;
import org.passay.PasswordValidator;
import org.passay.Rule;

@RunWith(value=Parameterized.class)

public class EnforceOneTest {
public enum PassRuleType {
   DUMMY,
   REAL
}


private boolean isUserAllowed = false;

private List<Character> illegalList = Arrays.asList('!');
private DefaultPasswordRule dpr;
private List<Character> special = Arrays.asList('@');
private Set<String> notPermittedList;
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

    public EnforceOneTest(String exception, String clear, String notPermitted, String username) {
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

    @Test
    public void enforceOneTest(){
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

