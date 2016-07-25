package org.talend.components.snowflake.tsnowflakeinput;

import aQute.bnd.annotation.component.Component;

import org.talend.components.api.Constants;
import org.talend.components.api.component.ComponentDefinition;
import org.talend.components.api.component.InputComponentDefinition;
import org.talend.components.api.component.runtime.Source;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.snowflake.SnowflakeConnectionProperties;
import org.talend.components.snowflake.SnowflakeDefinition;
import org.talend.components.snowflake.runtime.SnowflakeSource;

/**
 * Component that can connect to a snowflake system and get some data out of it.
 */

@Component(name = Constants.COMPONENT_BEAN_PREFIX
        + TSnowflakeInputDefinition.COMPONENT_NAME, provide = ComponentDefinition.class)

public class TSnowflakeInputDefinition extends SnowflakeDefinition implements InputComponentDefinition {
	 public static final String COMPONENT_NAME = "tSnowflakeInput"; //$NON-NLS-1$

	    public TSnowflakeInputDefinition() {
	        super(COMPONENT_NAME);
	    }

	    @Override
	    public boolean isStartable() {
	        return true;
	    }

	    @Override
	    public Class<? extends ComponentProperties> getPropertyClass() {
	        //return TSnowflakeInputProperties.class; TODO: implement this class
	    	return SnowflakeConnectionProperties.class; //TODO: replace this
	    }

	    @SuppressWarnings("unchecked")
	    @Override
	    public Class<? extends ComponentProperties>[] getNestedCompatibleComponentPropertiesClass() { //TODO: Check for redundant properties
	        return concatPropertiesClasses(super.getNestedCompatibleComponentPropertiesClass(),
	                new Class[] { /*SnowflakeModuleProperties.class*/ SnowflakeConnectionProperties.class});
	    }

	    @Override
	    public Source getRuntime() {
	        return new SnowflakeSource();
	    }

}
