package org.talend.components.snowflake.tsnowflakeoutput;

import org.talend.components.api.Constants;
import org.talend.components.api.component.ComponentDefinition;
import org.talend.components.api.component.OutputComponentDefinition;
import org.talend.components.api.component.runtime.Sink;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.snowflake.SnowflakeDefinition;
import org.talend.components.snowflake.SnowflakeTableProperties;
import org.talend.components.snowflake.runtime.SnowflakeSink;
import org.talend.daikon.properties.property.Property;

import aQute.bnd.annotation.component.Component;

/**
 * Component that can connect to a snowflake system and put some data into it.
 */

@Component(name = Constants.COMPONENT_BEAN_PREFIX
        + TSnowflakeOutputDefinition.COMPONENT_NAME, provide = ComponentDefinition.class)
public class TSnowflakeOutputDefinition extends SnowflakeDefinition implements OutputComponentDefinition {

    public static final String COMPONENT_NAME = "tSnowflakeOutput"; //$NON-NLS-1$

    public TSnowflakeOutputDefinition() {
        super(COMPONENT_NAME);
    }

    @Override
    public boolean isSchemaAutoPropagate() {
        return false;
    }

    @Override
    public boolean isConditionalInputs() {
        return true;
    }

    @Override
    public String getPartitioning() {
        return AUTO;
    }

    @Override
    public Class<? extends ComponentProperties> getPropertyClass() {
        return TSnowflakeOutputProperties.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends ComponentProperties>[] getNestedCompatibleComponentPropertiesClass() {
        return concatPropertiesClasses(super.getNestedCompatibleComponentPropertiesClass(),
                new Class[] { SnowflakeTableProperties.class });
    }

    @Override
    public Property[] getReturnProperties() {
        return new Property[] { RETURN_ERROR_MESSAGE_PROP, 
				        		RETURN_TOTAL_RECORD_COUNT_PROP, 
				        		RETURN_SUCCESS_RECORD_COUNT_PROP,
				                RETURN_REJECT_RECORD_COUNT_PROP };
    }

    @Override
    public Sink getRuntime() {
        return new SnowflakeSink();
    }

}
