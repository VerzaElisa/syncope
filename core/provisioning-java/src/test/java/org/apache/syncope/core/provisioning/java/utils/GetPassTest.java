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

import java.util.*;

import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.UserCR;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.spring.security.PasswordGenerator;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.GuardedByteArray;
import org.apache.syncope.common.lib.request.AnyCR;


@RunWith(value=Parameterized.class)
public class GetPassTest {
    public enum ObjType {
        GUARDEDSTRING,
        GUARDEDBYTEARRAY,
        OTHER,
        BYTE
    }

    private GuardedByteArray gbaPass;
    private String strPass;
    private final Object passToWrite;
    private Object objPass;
    private ConnObjectUtils cou;
    private static ObjType actualObj;
    private final String fiql = "TestName";
    private final String attrName = "TestAttrName";
    private final boolean isSetNull;
    private Set<Attribute> set = new HashSet<Attribute>();
    private Attr attrRet;
    private ConnectorObject obj;
    private PullTask pullTask;
    private final String anyCR;
    private AnyUtilsFactory anyUtilsFactory;
    private final UserDAO userDAO = mock(JPAUserDAO.class);
    private final TemplateUtils templateUtils = mock(TemplateUtils.class);
    private final boolean generatePass;
    private final boolean randomPass;
    private RealmDAO realmDAO;
    private final boolean realmIsNull;
    private final String passToSet = "PasswordToSet";

    @Mock
    ExternalResourceDAO resourceDAO;
    private PasswordGenerator passwordGenerator;
    @Mock
    MappingManager mappingManager;



    @Parameters
    public static Collection<Object[]> getTestParameters(){
        return Arrays.asList(new Object[][]{
//              | actualObj            | passToWrite | anyCR   | isSetNull | generatePass | randomPass | realmIsNull |
                {ObjType.GUARDEDSTRING , "Test"      , "uCR"   , false     , true         , true       , false       },
                {ObjType.OTHER         , "Test"      , "uCR"   , false     , true         , true       , true        },
                {ObjType.OTHER         , 1           , "other" , true      , true         , true       , false       },
                {ObjType.OTHER         , null        , "uCR"   , false     , false        , true       , false       },
        });
    }

    public GetPassTest(ObjType actualObj, Object passToWrite, String anyCR ,boolean isSetNull, boolean generatePass, boolean randomPass, boolean realmIsNull) {
        this.actualObj = actualObj;
        this.passToWrite = passToWrite;
        this.anyCR = anyCR;
        this.isSetNull = isSetNull;
        this.generatePass = generatePass;
        this.randomPass = randomPass;
        this.realmIsNull = realmIsNull;
    }

    @Before
    public void getPassSetUp(){
        cou = new ConnObjectUtils(templateUtils, realmDAO, userDAO, resourceDAO, passwordGenerator, mappingManager, anyUtilsFactory);
        Attribute attr;
        if(!isSetNull){
            if(passToWrite!=null){
                attr = AttributeBuilder.build(attrName, objPass);
            }
            else{
                attr = AttributeBuilder.build(attrName);
            }
            set.add(attr);
            Attr[] attrArray = ConnObjectUtils.getConnObjectTO(fiql, set).getAttrs().toArray(new Attr[set.size()]);
            attrRet = attrArray[0];
        }
        else{
            set = null;
        }
    }


    @Test
    public void setPassTest(){
        if(passToWrite!=null){
            assertEquals(passToWrite.toString(), ConnObjectUtils.getPassword(objPass));
        }
    }

}
