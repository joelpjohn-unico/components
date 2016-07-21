package org.talend.components.snowflake.tsnowflakeconnection;

import org.talend.components.api.Constants;
import org.talend.components.api.component.ComponentDefinition;
import org.talend.components.api.component.EndpointComponentDefinition;
import org.talend.components.api.component.runtime.SourceOrSink;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.snowflake.SnowflakeConnectionProperties;
import org.talend.components.snowflake.SnowflakeDefinition;
import org.talend.components.snowflake.runtime.SnowflakeSourceOrSink;
import org.talend.daikon.properties.property.Property;

import aQute.bnd.annotation.component.Component;

@Component(name = Constants.COMPONENT_BEAN_PREFIX
        + TSnowflakeConnectionDefinition.COMPONENT_NAME, provide = ComponentDefinition.class)
public class TSnowflakeConnectionDefinition extends SnowflakeDefinition implements EndpointComponentDefinition{

    public static final String COMPONENT_NAME = "tSnowflakeConnection"; //$NON-NLS-1$

    public TSnowflakeConnectionDefinition() {
        super(COMPONENT_NAME);
    }

    @Override
    public Class<? extends ComponentProperties> getPropertyClass() {
        return SnowflakeConnectionProperties.class;
    }

    @Override
    public Property[] getReturnProperties() {
        return new Property[] { RETURN_ERROR_MESSAGE_PROP };
    }

    @Override
    public boolean isStartable() {
        return true;
    }

    @Override
    public SourceOrSink getRuntime() {
        return new SnowflakeSourceOrSink();
    }

}
