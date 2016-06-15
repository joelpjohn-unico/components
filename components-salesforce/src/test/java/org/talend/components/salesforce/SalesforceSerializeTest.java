// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.salesforce;

import org.junit.Assert;
import org.junit.Test;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.api.test.ComponentTestUtils;
import org.talend.components.salesforce.tsalesforceconnection.TSalesforceConnectionDefinition;


/**
 * created by nrousseau on Jun 15, 2016
 * Detailled comment
 *
 */
public class SalesforceSerializeTest extends SalesforceTestBase {

    @Test
    public void testSerializeUserPassword() {
        ComponentProperties props;

        props = new TSalesforceConnectionDefinition().createProperties();
        props.setValue("userPassword.userId", "myUser");
        props.setValue("userPassword.password", "myPassword");
        ComponentProperties newProps = (ComponentProperties) ComponentTestUtils.checkSerialize(props, errorCollector);
        
        Assert.assertEquals("myUser", newProps.getValuedProperty("userPassword.userId").getValue());
        Assert.assertEquals("myPassword", newProps.getValuedProperty("userPassword.password").getValue());
    }
}
