package org.talend.components.common.serialize;


import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.daikon.properties.Properties;
import org.talend.daikon.serialize.SerializerDeserializer.Deserialized;

public class ComponentsSerializeTest {

    @Test
    public void testSerialize() throws IOException {
        TestProperties props = (TestProperties) new TestProperties("test").init(); //$NON-NLS-1$
        props.userId.setValue("1"); //$NON-NLS-1$
        TestNestedProperties nestedProps = (TestNestedProperties) props.getProperty("nestedProps"); //$NON-NLS-1$
        nestedProps.userName.setValue("testUserName"); //$NON-NLS-1$
        nestedProps.userPassword.setValue("testUserPassword"); //$NON-NLS-1$
        String serializedString = props.toSerialized();

        Deserialized<ComponentProperties> deserialized = Properties.Helper.fromSerializedPersistent(serializedString, ComponentProperties.class, null);
        ComponentProperties deserializedProperties = deserialized.object;
        Assert.assertEquals("1", deserializedProperties.getValuedProperty("userId").getValue());
        Assert.assertEquals("testUserName", deserializedProperties.getValuedProperty("nestedProps.userName").getValue());
        Assert.assertEquals("testUserPassword", deserializedProperties.getValuedProperty("nestedProps.userPassword").getValue());
    }
}
