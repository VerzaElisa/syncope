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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.nimbusds.jose.util.IOUtils;

import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;



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
private DefaultPasswordRule dpr = new DefaultPasswordRule();;
private List<Character> special = Arrays.asList('@');
private PassRuleType type;
private String ex = null;
private String exception;


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
