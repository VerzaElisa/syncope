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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.*;

import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.*;
import org.apache.syncope.core.persistence.api.entity.*;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.jpa.entity.*;
import org.apache.syncope.core.spring.security.DefaultPasswordGenerator;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.request.AnyObjectCR;

@RunWith(value=Parameterized.class)
public class GetAnyCRTest {
    
    private ConnObjectUtils cou;
    private ConnObjectUtils couToSpy;
    private UserDAO userDAO;
    private ConnectorObject obj;
    private PullTask pullTask;
    private AnyUtilsFactory anyUtilsFactory;
    private TemplateUtils templateUtils;
    private RealmDAO realmDAO;
    private Provision provision;
    private PasswordGenerator passwordGenerator;
    private MappingManager mappingManager;
    private ExternalResourceDAO resourceDAO;
    private String type;
    private String auxClasses;
    private boolean attributes;
    private String password;
    private AnyTypeKind kind;
    private String username;
    private boolean mustChangePassword;
    private boolean generatePass;
    private String ex;
    private String name;
    private boolean typeAux;
    private String owner;
    private Set<Attr> attrSet;
    private String myResource = "testResource";
    private AnyTO myAnyTO;
    private Realm r = null;
    private String realmPath = "path/to/realm";
    private boolean realmExists;
    private int findAncestors = 0;
    private Set<String> classes;
    private ExternalResource externalRes;
    private boolean passwordPolicy;
    private PasswordPolicy pp;
    private int timesGetPolRealm = 1;
    private int timesGetPolUser = 0;


    @Parameters
    public static Collection<Object[]> getTestParameters(){
        return Arrays.asList(new Object[][]{
//              | typeAux | realmExists | attributes | password   | kind                   | generatePass | externalRes                              | passwordPolicy |
                { true    , false       , true       , "passGen"  , AnyTypeKind.USER       , true         , mock(ExternalResource.class)             , false          },
                { true    , true        , true       , null       , AnyTypeKind.GROUP      , true         , null                                     , false          },
                { true    , true        , true       , null       , AnyTypeKind.ANY_OBJECT , true         , null                                     , false          },
                { false   , true        , false      , "passGen"  , AnyTypeKind.USER       , true         , null                                     , false          },
                { false   , true        , false      , null       , AnyTypeKind.USER       , false        , null                                     , false          },
                { false   , true        , false      , "testPass" , AnyTypeKind.USER       , false        , null                                     , false          },
                { true    , true        , false      , null       , AnyTypeKind.ANY_OBJECT , false        , null                                     , false          },
  
                { true    , false       , true       , "passGen"  , AnyTypeKind.USER       , true         , mock(ExternalResource.class)             , true           },
                { true    , true        , true       , "passGen"  , AnyTypeKind.USER       , true         , mock(ExternalResource.class)             , true           },
                { true    , true        , true       , "passGen"  , AnyTypeKind.USER       , true         , null                                     , true           },
        });
    }

    public GetAnyCRTest(boolean typeAux, boolean realmExists, boolean attributes, String password, AnyTypeKind kind, boolean generatePass, ExternalResource externalRes, boolean passwordPolicy) {
        this.typeAux = typeAux;
        this.realmExists = realmExists;
        this.attributes = attributes;
        this.password = password;
        this.kind = kind;
        this.generatePass = generatePass;
        this.externalRes = externalRes;
        this.passwordPolicy = passwordPolicy;
    }

    @Before
    public void anyCRSetUp() throws UnsupportedEncodingException, ParseException{
        mockGenerator();
        //Stub del metodo getAnyTOFromConnObject
        myAnyTO = myGetAnyTOFromConnObject();

        if(kind.equals(AnyTypeKind.USER)){
            //Aggiungo risorsa ad anyTO
            myAnyTO.getResources().add(myResource);
            myAnyTO = spy(myAnyTO);
            //Configuro un external resource con una password policy
            pp = mock(PasswordPolicy.class);
            if(externalRes != null && passwordPolicy){
                timesGetPolUser = 2;
                when(externalRes.getPasswordPolicy()).thenReturn(pp);
            }else if(!passwordPolicy){
                if(externalRes != null){
                    timesGetPolUser = 1;
                }
                pp = null;
            }
            when(resourceDAO.find(anyString())).thenReturn(externalRes);

            if(realmExists){
                findAncestors = 1;
                r = mock(Realm.class);
                if(!passwordPolicy){
                    timesGetPolRealm = 3;
                    pp = spy(PasswordPolicy.class);
                }else if(externalRes == null && passwordPolicy){
                    pp = null;
                }else{
                    timesGetPolRealm = 2;
                }
                when(r.getPasswordPolicy()).thenReturn(pp);
                List<Realm> realmList = Arrays.asList(r);
                when(realmDAO.findAncestors(any(Realm.class))).thenReturn(realmList);
            }
            when(realmDAO.findByFullPath(realmPath)).thenReturn(r);
        }

        //Viene fatta la spy della classe per ridefinire il comportamento del metodo getAnyTOFromConnObject
        couToSpy = new ConnObjectUtils(templateUtils, realmDAO, userDAO, resourceDAO, passwordGenerator, mappingManager, anyUtilsFactory);
        cou = spy(couToSpy);
        doReturn(myAnyTO).when(cou).getAnyTOFromConnObject(any(ConnectorObject.class), any(PullTask.class), any(AnyTypeKind.class), any(Provision.class));
    }


    @Test
    public void anyURTest(){
        try{
            AnyCR ret = cou.getAnyCR(obj, pullTask, kind, provision, generatePass);
            assertEquals(classes, ret.getAuxClasses());
            assertEquals(realmPath, ret.getRealm());
            switch(kind){
                case USER:
                    assertTrue(ret instanceof UserCR);
                    UserCR userRet = (UserCR) ret;
                    assertEquals(username, userRet.getUsername());
                    assertEquals(password, userRet.getPassword());
                    assertEquals(mustChangePassword, userRet.isMustChangePassword());
                    if(generatePass){
                        verify(resourceDAO).find(anyString());
                        verify(realmDAO).findByFullPath(anyString());
                        verify(realmDAO, times(findAncestors)).findAncestors(r);
                        if(externalRes != null){
                            verify(externalRes, times(timesGetPolUser)).getPasswordPolicy();
                        }
                        if(realmExists){
                            verify(r, times(timesGetPolRealm)).getPasswordPolicy();
                        }
                    }
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
                    break;
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
        classes = new HashSet<String>() ;
        myAnyTO.setType(type); //Settato solo se l'oggetto è un anyObject
        if(typeAux){
            type = "testType";
            auxClasses = "testClassOne testClassTwo";
            classes = new HashSet<>(Arrays.asList(auxClasses.split(" ")));
        }
        myAnyTO.getAuxClasses().addAll(classes);
        myAnyTO.setRealm(realmPath);
            switch(kind){
                case USER:
                    UserTO myUserTO = (UserTO) myAnyTO;
                    if(attributes){
                        username = "testUser";
                        mustChangePassword = false;
                        myUserTO.setUsername(username);
                        myUserTO.setMustChangePassword(mustChangePassword);
                    }
                    if(password != null && password.equals("testPass")){
                        myUserTO.setPassword(password);
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
        passwordGenerator = mock(DefaultPasswordGenerator.class);
        when(passwordGenerator.generate(anyList())).thenReturn(password);
        resourceDAO = mock(ExternalResourceDAO.class);
        mappingManager = mock(MappingManager.class);
        anyUtilsFactory = mock(AnyUtilsFactory.class, RETURNS_DEEP_STUBS);
        when(anyUtilsFactory.getInstance(any(AnyTypeKind.class)).newAnyCR()).thenReturn(createAnyCR());
        obj = mock(ConnectorObject.class);
        pullTask = mock(PullTask.class);
        provision = mock(Provision.class);

    }
}
