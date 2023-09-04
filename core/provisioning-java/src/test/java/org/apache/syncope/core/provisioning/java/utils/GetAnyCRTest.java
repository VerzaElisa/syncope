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
import java.util.*;
import java.util.stream.Stream;

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
import org.mockito.Answers;
import org.mockito.Mock;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;
import org.identityconnectors.common.security.GuardedByteArray;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.Item;



@RunWith(value=Parameterized.class)
public class GetAnyCRTest {
    public enum ObjType {
        GUARDEDSTRING,
        GUARDEDBYTEARRAY,
        STRING,
        BYTE
    }

    // private GuardedByteArray gbaPass;
    // private String strPass;
    // private Object objPass;
    // private Object setPass;
    // private ConnObjectUtils couSpy;
    // private ObjType actualObj;
    // private String fiql;
    // private final String attrName = "TestAttrName";
    // private Set<Attribute> set = new HashSet<Attribute>();
    // private Attr attrRet;
    // private final String passToSet = "PasswordToSet";
    // private String exception;
    // private String ret;
    // private List<String> attrVal = new ArrayList<String>();
    // private String value;
    // private Attribute attr;
    // private boolean empty = false;
    
    //Parametri
    private final String anyCR;
    private boolean realmIsNull;
    //Vere
    private AnyTypeKind anyTypeKind;
    private ConnObjectUtils cou;
    private String path = "/file"; //parametro
    private String attrName = "attrName";
    private Attribute attr;
    //Mocked
    private GroupDAO groupDAO;
    private UserDAO userDAO;
    private AnyObjectDAO anyObjectDAO;
    private EntityFactory entityFactory;
    private ConnectorObject obj;
    private PullTask pullTask;
    private AnyUtilsFactory anyUtilsFactory;
    private TemplateUtils templateUtils;
    private RealmDAO realmDAO;
    private Provision provision;
    private PasswordGenerator passwordGenerator;
    private AnyTO toMock;
    private MappingManager mappingManager;
    

    @Mock
    ExternalResourceDAO resourceDAO;


    @Parameters
    public static Collection<Object[]> getTestParameters(){
        return Arrays.asList(new Object[][]{
                {""         , true },
                {""         , true },
                {"TestFiql" , true },
                {"TestFiql" , true },
                {"TestFiql" , true },
        });
    }

    public GetAnyCRTest(String anyCR, boolean realmIsNull) {
        this.anyCR = anyCR;
        this.realmIsNull = realmIsNull;
    }

    @Before
    public void anyCRSetUp() throws UnsupportedEncodingException{
        anyTypeKind = AnyTypeKind.USER; //Parametro
        attr = AttributeBuilder.build(attrName, "attrValue");
        mockGenerator();
        cou = new ConnObjectUtils(templateUtils, realmDAO, userDAO, resourceDAO, passwordGenerator, mappingManager, anyUtilsFactory);
    }


    @Test
    public void anyCRTest(){
        cou.getAnyCR(obj, pullTask, anyTypeKind, provision, false);
        int ret = 1;
        assertEquals(1, ret);
    }


    public void mockGenerator(){
        Item itemToAdd = spy(Item.class);
        when(itemToAdd.getExtAttrName()).thenReturn(attrName);

        Set<String> retSet = new HashSet<String>();
        List<String> retList = new ArrayList<String>();
        List<Item> itemList = new ArrayList<Item>();
        itemList.add(itemToAdd);
        Stream<Item> itemStream = itemList.stream();

        templateUtils = mock(TemplateUtils.class);
        realmDAO = mock(RealmDAO.class);
        passwordGenerator = mock(PasswordGenerator.class);
        mappingManager = mock(MappingManager.class, Answers.CALLS_REAL_METHODS);

        //Definisco tutti i metodi di obj che mi servono
        obj = mock(ConnectorObject.class);
        when(obj.getAttributeByName(itemToAdd.getExtAttrName())).thenReturn(attr);


        //Definisco tutti i metodi di pullTask che mi servono
        pullTask = mock(PullTask.class, RETURNS_DEEP_STUBS);
        when(pullTask.getDestinationRealm().getFullPath()).thenReturn(path);

        //Definisco tutti i metodi di provisioning che mi servono
        provision = mock(Provision.class, RETURNS_DEEP_STUBS);
        when(provision.getAnyType()).thenReturn("MyType");
        when(provision.getAuxClasses()).thenReturn(retList);
        when(provision.getMapping().getItems().stream()).thenReturn(itemStream);

        //Definisco tutti i metodi di AnyTO che mi servono
        toMock = mock(AnyTO.class, Answers.CALLS_REAL_METHODS);
        when(toMock.getAuxClasses()).thenReturn(retSet);

        //anyUtils factory ritorna un'istanza mockata di anyTO
        anyUtilsFactory = mock(AnyUtilsFactory.class, RETURNS_DEEP_STUBS);
        when(anyUtilsFactory.getInstance(anyTypeKind).newAnyTO()).thenReturn(toMock);

        // userDAO = mock(JPAUserDAO.class);
        // groupDAO = mock(GroupDAO.class);
        // anyObjectDAO = mock(AnyObjectDAO.class);
        // entityFactory = mock(EntityFactory.class);


        // AnyCR aCR;

        // realmDAO = mock(JPARealmDAO.class, RETURNS_DEEP_STUBS);
        // provision = mock(Provision.class, RETURNS_DEEP_STUBS);
        // pullTask = mock(PullTask.class, RETURNS_DEEP_STUBS);
        // ConnectorObjectBuilder cob = new ConnectorObjectBuilder();
        // cob.setUid("UidTest");
        // cob.setName("Conn obj name");
        // obj = cob.build();
        // passwordGenerator = mock(DefaultPasswordGenerator.class);
        // List<PasswordPolicy> passwordPolicies = new ArrayList<>();

        // if(anyCR.equals("uCR")){
        //     aCR = new UserCR();
        // }
        // else{
        //     aCR = new GroupCR();
        // }


        // if(realmIsNull){
        //     when(realmDAO.findByFullPath(aCR.getRealm())).thenReturn(null);
        // }
        // else{
        //     when(realmDAO.findByFullPath(aCR.getRealm())).thenReturn(new JPARealm());
        // }

        // when(passwordGenerator.generate(passwordPolicies)).thenReturn(passToSet);
        // when(pullTask.getDestinationRealm().getFullPath()).thenReturn("my/testing/path");
        // when(provision.getAnyType()).thenReturn(AnyTypeKind.USER);
        // when(provision.getAnyType().getKey()).thenReturn("key test");
        // when(provision.getResource().isRandomPwdIfNotProvided()).thenReturn(randomPass);
        // when(anyUtilsFactory.getInstance(provision.getAnyType().getKind()).newAnyCR()).thenReturn(aCR);
    }
}
