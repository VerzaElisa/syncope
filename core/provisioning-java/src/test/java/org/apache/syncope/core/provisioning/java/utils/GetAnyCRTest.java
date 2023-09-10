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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Stream;

import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.GroupTO;
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
import org.mockito.Answers;
import org.mockito.Mock;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.h2.engine.User;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.common.security.GuardedByteArray;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.core.provisioning.api.DerAttrHandler;
import org.apache.syncope.core.provisioning.api.IntAttrName;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCache;
import org.apache.syncope.core.provisioning.api.cache.VirAttrCacheKey;
import org.apache.syncope.core.provisioning.java.DefaultMappingManager;
import org.apache.syncope.core.provisioning.api.IntAttrNameParser;






@RunWith(value=Parameterized.class)
public class GetAnyCRTest {
    public enum ObjType {
        GUARDEDSTRING,
        GUARDEDBYTEARRAY,
        STRING,
        BYTE
    }
    
    //Parametri
    private String exception;
    private String attrName;

    //Vere
    private ConnObjectUtils cou;
    private ConnObjectUtils couToSpy;

    private String attrVal = "testVal";
        //provision richiede sempre un attributo può o meno essere in obj
    private String provisionAttrName = "commonObj";
    private String path = "/file"; //parametro
    private Attribute attr;

    //Mocked
    private GroupDAO groupDAO;
    private UserDAO userDAO;
    private AnyObjectDAO anyObjectDAO;
    private ConnectorObject obj;
    private PullTask pullTask;
    private AnyUtilsFactory anyUtilsFactory;
    private TemplateUtils templateUtils;
    private RealmDAO realmDAO;
    private Provision provision;
    private PasswordGenerator passwordGenerator;
    private MappingManager mappingManager;
    private ExternalResourceDAO resourceDAO;
    private AnyTypeDAO anyTypeDAO;
    private RelationshipTypeDAO relationshipTypeDAO;
    private ApplicationDAO applicationDAO;
    private ImplementationDAO implementationDAO;
    private DerAttrHandler derAttrHandler;
    private VirAttrHandler virAttrHandler;
    private VirAttrCache virAttrCache;
    private IntAttrNameParser intAttrNameParser;
    private String type;
    private String auxClasses;
    private String realm;
    private boolean attributes;
    private boolean password;
    private AnyTypeKind kind;
    private String username;
    private String pass;
    private boolean mustChangePassword;
    private boolean generatePass;
    private String ex;
    private String name;
    private String owner;
    private Set<Attr> attrSet;

    @Parameters
    public static Collection<Object[]> getTestParameters(){
        return Arrays.asList(new Object[][]{
//              | type             | auxClasses                  | realm          | attributes | password | kind                   | generatePass |
                { "testType"       , "testClassOne testClassTwo" , "path/to/realm", true       , false    , AnyTypeKind.USER       , false        },
                { "testType"       , "testClassOne testClassTwo" , "path/to/realm", true       , false    , AnyTypeKind.GROUP      , false        },
                { "testType"       , "testClassOne testClassTwo" , "path/to/realm", true       , false    , AnyTypeKind.ANY_OBJECT , false        },

        });
    }

    public GetAnyCRTest(String type, String auxClasses, String realm, boolean attributes, boolean password, AnyTypeKind kind, boolean generatePass) {
        this.type = type;
        this.auxClasses = auxClasses;
        this.realm = realm;
        this.attributes = attributes;
        this.password = password;
        this.kind = kind;
        this.generatePass = generatePass;
    }

    @Before
    public void anyCRSetUp() throws UnsupportedEncodingException, ParseException{
        mockGenerator();
        //Viene fatta la spy della classe per ridefinire il comportamento del metodo getAnyTOFromConnObject
        couToSpy = new ConnObjectUtils(templateUtils, realmDAO, userDAO, resourceDAO, passwordGenerator, mappingManager, anyUtilsFactory);
        cou = spy(couToSpy);

        //Stub del metodo getAnyTOFromConnObject
        AnyTO myAnyTO = myGetAnyTOFromConnObject();
        doReturn(myAnyTO).when(cou).getAnyTOFromConnObject(any(ConnectorObject.class), any(PullTask.class), any(AnyTypeKind.class), any(Provision.class));
    }


    @Test
    public void anyURTest(){
        try{
            AnyCR ret = cou.getAnyCR(obj, pullTask, kind, provision, generatePass);
            assertEquals(new HashSet<>(Arrays.asList(auxClasses.split(" "))), ret.getAuxClasses());
            assertEquals(realm, ret.getRealm());
            switch(kind){
                case USER:
                    assertTrue(ret instanceof UserCR);
                    UserCR userRet = (UserCR) ret;
                    assertEquals(username, userRet.getUsername());
                    assertEquals(pass, userRet.getPassword());
                    assertEquals(mustChangePassword, userRet.isMustChangePassword());
                    break;
                case GROUP:
                    assertTrue(ret instanceof GroupCR);
                    GroupCR groupRet = (GroupCR) ret;
                    assertEquals(name, groupRet.getName());
                    assertEquals(attrSet, groupRet.getPlainAttrs());
                    break;
                case ANY_OBJECT:
                    assertTrue(ret instanceof AnyObjectCR);
                    AnyObjectCR anyObjectRet = (AnyObjectCR) ret;
                    assertEquals(name, anyObjectRet.getName());
                default:
            }
        }catch(Exception e){
            ex = e.getClass().getSimpleName();
            e.printStackTrace();
        }finally{
            assertNull(ex);
            verify(cou).getAnyTOFromConnObject(obj, pullTask, kind, provision);
        }
    }




    public <T extends AnyTO> T createAnyTO(){
        T myAnyTO = null;

        switch (kind) {
            case USER:
                myAnyTO = (T) new UserTO();
                break;

            case GROUP:
                myAnyTO = (T) new GroupTO();
                break;

            case ANY_OBJECT:
                myAnyTO = (T) new AnyObjectTO();
                break;

            default:
        }
        return myAnyTO;
    }

    public <C extends AnyCR> C createAnyCR() {
    C result = null;
    switch (kind) {
        case USER:
            result = (C) new UserCR();
            break;

        case GROUP:
            result = (C) new GroupCR();
            break;

        case ANY_OBJECT:
            result = (C) new AnyObjectCR();
            break;

        default:
    }
    return result;
    }


    public AnyTO myGetAnyTOFromConnObject(){
        //Costruzione anyTO con parametri
        AnyTO myAnyTO = createAnyTO();
        myAnyTO.setType(type); //Settato solo se l'oggetto è un anyObject
        myAnyTO.getAuxClasses().addAll(new HashSet<>(Arrays.asList(auxClasses.split(" "))));
        myAnyTO.setRealm(realm);
            switch(kind){
                case USER:
                    if(attributes){
                        UserTO myUserTO = (UserTO) myAnyTO;
                        username = "testUser";
                        mustChangePassword = false;
                        myUserTO.setUsername(username);
                        myUserTO.setMustChangePassword(mustChangePassword);
                        if(password){
                            pass = "testPass";
                            myUserTO.setPassword(pass);
                        }
                    }
                    break;
                case GROUP:
                    if(attributes){
                        GroupTO myGroupTO = (GroupTO) myAnyTO;
                        attrSet = new HashSet<Attr>();
                        owner = "testOwner";
                        name = "testName";
                        myGroupTO.setName(name);
                        Attr attrOwner = new Attr();
                        attrOwner.getValues().add(owner);
                        attrSet.add(attrOwner);
                        myGroupTO.getPlainAttrs().add(attrOwner);
                    }
                    break;
                case ANY_OBJECT:
                    AnyObjectTO myAnyObjectTO = (AnyObjectTO) myAnyTO;
                    name = "testName";
                    myAnyObjectTO.setName(name);
                    break;
                default:
            }
        return myAnyTO;
    }

    public void mockGenerator(){
        templateUtils = mock(TemplateUtils.class);
        realmDAO = mock(RealmDAO.class);
        userDAO = mock(UserDAO.class);
        passwordGenerator = mock(PasswordGenerator.class);
        resourceDAO = mock(ExternalResourceDAO.class);
        mappingManager = mock(MappingManager.class);
        anyUtilsFactory = mock(AnyUtilsFactory.class, RETURNS_DEEP_STUBS);
        when(anyUtilsFactory.getInstance(any(AnyTypeKind.class)).newAnyCR()).thenReturn(createAnyCR());
        obj = mock(ConnectorObject.class);
        pullTask = mock(PullTask.class);
        provision = mock(Provision.class);

    }
}
