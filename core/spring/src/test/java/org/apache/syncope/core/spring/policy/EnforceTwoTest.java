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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.syncope.common.lib.types.CipherAlgorithm;
import org.apache.syncope.core.spring.security.Encryptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.passay.PasswordValidator;
import org.passay.Rule;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.LAPlainAttr;


import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;

@RunWith(value = Parameterized.class)

public class EnforceTwoTest {
public enum PassRuleType {
   DUMMY,
   REAL
}

private boolean isUserAllowed = false;
private DefaultPasswordRuleConf defConf;
private List<Character> illegalList = Arrays.asList('!');
private DefaultPasswordRule dpr;
private List<Character> special = Arrays.asList('@');
private String pass;
private String exception;
private String username;
private boolean toDecode;
private boolean isSchemaPermitted;
private String value;
private String ex = null;
private List<String> retList = new ArrayList<String>();
private List<String> ret;
private boolean encrypt;
private int timesUser = 1;
private int timesDecode = 1;
private int timesCipher = 1;
private String defNotPermittedSchema = "birthdate";
private LinkedAccount account;
//                                         | minLen | maxLen | alpha | lower | upper | digit | special | same |
private List<Integer> param = Arrays.asList( 8      , 8      , 4     , 2     , 2     , 3     , 1       , 8   );



    @Parameters
    public static Collection<Object[]> getTestParameters(){
        return Arrays.asList(new Object[][]{
//          | exception                 | pass              | username   | toDecode | encrypt | isSchemaPermitted | value            |
            { "PasswordPolicyException" , "AAaa234@"        , "AAaa234@" , true     , true    , false             , "01-01-19"       },
            { null                      , null              , "UserTest" , true     , true    , false             , null             },
            { null                      , "AAaa234@"        , "UserTest" , true     , true    , false             , ""               },
            { null                      , "AAaa234@"        , "UserTest" , true     , false   , true              , "PermittedValue" },
            { null                      , "AAaa234@"        , "UserTest" , true     , true    , true              , "PermittedValue" },
            { "PasswordPolicyException" , "AAaa234@"        , "UserTest" , true     , true    , false             , "AAaa234@"       },
            { null                      , "AAaa234@"        , "UserTest" , false    , true    , true              , "PermittedValue" },
        });  
    }

    public EnforceTwoTest(String exception, String pass, String username, boolean toDecode, boolean encrypt, boolean isSchemaPermitted, String value) {
        this.exception = exception;
        this.pass = pass;
        this.toDecode = toDecode;
        this.encrypt = encrypt;
        this.username = username;
        this.isSchemaPermitted = isSchemaPermitted;
        this.value = value;
    }

    @Before
    public void enforceTwoSetUp() throws InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException{
        List<String> valueList = new ArrayList<String>();
        dpr = new DefaultPasswordRule();
        String ret = pass;
        timesDecode = 1;
        timesUser = 1;


        //Creazione lista di regole da passare al validator
        Utility.createListRule(param, 0, 1, illegalList, special, isUserAllowed);
        dpr.passwordValidator = new PasswordValidator(Utility.getRule().toArray(new Rule[0]));

        //Viene creata una lista di stringhe che rappresentano gli attributi i cui valori non possono essere utilizzati nela password
        List<String> notPermittedSchema = Arrays.asList(defNotPermittedSchema);
        defConf = spy(DefaultPasswordRuleConf.class);
        when(defConf.getSchemasNotPermitted()).thenReturn(notPermittedSchema);
        dpr.conf = defConf;

        //Mock di account e stub dei metodi necessari
        account = mock(LinkedAccount.class);
        if(toDecode){
            if(encrypt){
                Encryptor ENCRYPTOR = Encryptor.getInstance();
                ret = ENCRYPTOR.encode(pass, CipherAlgorithm.AES);
            }else{
                timesUser = 0;
            }
        }else{
            timesUser = 0;
            timesCipher = 0;
        }
        when(account.getPassword()).thenReturn(ret);
        when(account.getUsername()).thenReturn(username);
        when(account.canDecodeSecrets()).thenReturn(toDecode);
        when(account.getCipherAlgorithm()).thenReturn(CipherAlgorithm.AES);

        if(!isSchemaPermitted){
            if(value != ""){
                valueList.add(value);
            }
            //Costruzione del plainAttr da cui prendere i valori degli attributi
            LAPlainAttr mockedPlainAttr = mock(LAPlainAttr.class);
            if(value!=null){
                when(mockedPlainAttr.getValuesAsStrings()).thenReturn(valueList);
            }else{
                when(mockedPlainAttr.getValuesAsStrings()).thenReturn(null);
            }
            Optional<? extends LAPlainAttr> optionalPlainAttr = Optional.of(mockedPlainAttr);
            doReturn(optionalPlainAttr).when(account).getPlainAttr(anyString());
            if(value != null && value != ""){
                retList.add(value);
            }
        }else{
            when(account.getPlainAttr(anyString())).thenReturn(Optional.empty());
        }
        if(pass == null){
            timesDecode = 0;
            timesUser = 0;
            timesCipher = 0;
        }
    }

    @Test
    public void enforceTwoTest(){
        try{
            dpr.enforce(account);
        }catch(Exception e){
            ex = e.getClass().getSimpleName();
        }finally{
            ret = dpr.conf.getWordsNotPermitted();
            if(retList.size() > 0 && !isSchemaPermitted){
                assertEquals(ret.get(0), retList.stream().findFirst().orElse(null));
            }else{
                assertTrue(ret.isEmpty());
            }
            assertEquals(exception, ex);
            verify(account, times(timesUser)).getUsername();
            verify(account, times(timesDecode)).canDecodeSecrets();
            verify(account, times(timesCipher)).getCipherAlgorithm();
        }
    }
}
