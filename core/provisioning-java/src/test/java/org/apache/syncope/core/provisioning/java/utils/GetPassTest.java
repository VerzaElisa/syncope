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

package org.apache.syncope.core.provisioning.java.utils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
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
import org.apache.syncope.core.persistence.jpa.dao.JPARealmDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAUserDAO;
import org.apache.syncope.core.persistence.jpa.entity.*;
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
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.common.security.GuardedByteArray;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.to.Provision;


@RunWith(value=Parameterized.class)
public class GetPassTest {
    public enum ObjType {
        GUARDEDSTRING,
        GUARDEDBYTEARRAY,
        STRING,
        BYTE
    }

    private final String passToWrite;
    private Object objPass;
    private Object setPass;
    private ObjType actualObj;
    private String fiql;
    private final String attrName = "TestAttrName";
    private Set<Attribute> set = new HashSet<Attribute>();
    private AnyUtilsFactory anyUtilsFactory;
    private final UserDAO userDAO = mock(JPAUserDAO.class);
    private final TemplateUtils templateUtils = mock(TemplateUtils.class);
    private RealmDAO realmDAO;
    private String exception;
    private String ret;
    private List<String> attrVal = new ArrayList<String>();
    private String value;
    private Attribute attr;
    private boolean empty = false;
    private ConnObjectUtils cou;

    @Mock
    ExternalResourceDAO resourceDAO;
    private PasswordGenerator passwordGenerator;
    @Mock
    MappingManager mappingManager;



    @Parameters
    public static Collection<Object[]> getTestParameters(){
        return Arrays.asList(new Object[][]{
//              | actualObj               | passToWrite | exception              | fiql       | value            |
                {ObjType.STRING           , null        , "NullPointerException" , ""         , ""               },
                {ObjType.STRING           , null        , "NullPointerException" , ""         , null             },
                {ObjType.GUARDEDSTRING    , "testPass"  , null                   , "TestFiql" , "testGuardedStr" },
                {ObjType.STRING           , "testPass"  , null                   , "TestFiql" , "testStr"        },
                {ObjType.BYTE             , "testPass"  , null                   , "TestFiql" , "testByte"       },
                //{ObjType.GUARDEDBYTEARRAY , "testPass"  , null                   , "TestFiql" , "testGuardedArr" },

        });
    }

    public GetPassTest(ObjType actualObj, String passToWrite, String exception, String fiql, String value) {
        this.actualObj = actualObj;
        this.passToWrite = passToWrite;
        this.exception = exception;
        this.fiql = fiql;
        this.value = value;
    }

    @Before
    public void getPassSetUp() throws UnsupportedEncodingException{
        //Viene creato l'oggetto
        cou = new ConnObjectUtils(templateUtils, realmDAO, userDAO, resourceDAO, passwordGenerator, mappingManager, anyUtilsFactory);
        if(value == null){
            return;
        }
        ret = passToWrite;
        attrVal.add(value);
        //Viene invocato il metodo per mettere in objPass il valore della password 
        objPass = switchCase(objPass, passToWrite);
        setPass = switchCase(setPass, value);

        if(!value.equals("")){
            attr = AttributeBuilder.build(attrName, setPass);
            set.add(attr);
        }
        else{
            empty = true;
            attr = AttributeBuilder.build(attrName);
            set.add(attr);
        }
    }


    @Test
    public void setPassTest(){
        try{
            Assert.assertEquals(ret, ConnObjectUtils.getPassword(objPass));
        }catch(Exception e){    
            Assert.assertEquals(exception, e.getClass().getSimpleName());
        }
    }

    @Test
    public void getConnObjectTOTest(){
        ConnObject retVal = ConnObjectUtils.getConnObjectTO(fiql, set);
        if(empty){
            assertEquals(true, retVal.getAttr(attrName).get().getValues().isEmpty());
        }else if(retVal.getAttr(attrName).isPresent()){
            assertEquals(attrVal, retVal.getAttr(attrName).get().getValues());
        }else{
            assertEquals(false, retVal.getAttr(attrName).isPresent());
        }
        assertEquals(fiql, retVal.getFiql());
    }

    public Object switchCase(Object toSet, String valueToWrite) throws UnsupportedEncodingException{
        switch(actualObj) {
            case GUARDEDSTRING:
                char[] passwordToChar = String.valueOf(valueToWrite).toCharArray();
                toSet = new GuardedString(passwordToChar);
                break;
            case GUARDEDBYTEARRAY:
                byte[] passwordToByte = valueToWrite.getBytes(StandardCharsets.UTF_8);
                toSet = new GuardedByteArray(passwordToByte);
                break;
            case BYTE:
                toSet = valueToWrite.getBytes();
                if(objPass!=null){
                    ret = objPass.toString();
                }
                if(value!=null){
                    attrVal.clear();
                    byte[] byteVal = value.getBytes();
                    attrVal.add(Base64.getEncoder().encodeToString(byteVal));
                }
                break;
            case STRING:
                toSet = valueToWrite;
                break;
            default:

        }
        return toSet;
    }
}
