
package org.talend.components.snowflake;

import java.io.InputStream;

import org.talend.components.api.Constants;
import org.talend.components.api.component.AbstractComponentDefinition;
import org.talend.components.api.component.ComponentDefinition;
import org.talend.components.api.component.ComponentImageType;
import org.talend.components.api.component.InputComponentDefinition;
import org.talend.components.api.component.runtime.Source;
import org.talend.components.api.properties.ComponentProperties;

import org.talend.daikon.properties.property.Property;

import aQute.bnd.annotation.component.Component;

/**
 * The SnowflakeDefinition acts as an entry point for all of services that 
 * a component provides to integrate with the Studio (at design-time) and other 
 * components (at run-time).
 */
@Component(name = Constants.COMPONENT_BEAN_PREFIX + SnowflakeDefinition.COMPONENT_NAME, provide = ComponentDefinition.class)
public class SnowflakeDefinition extends AbstractComponentDefinition implements InputComponentDefinition {

    //Unico TODO: Will be defined by the subclass
	//public static final String COMPONENT_NAME = "Snowflake"; //$NON-NLS-1$

    public SnowflakeDefinition(String componentName) {
        super(componentName);
    }

    @Override
    public String[] getFamilies() {
        return new String[] { "Cloud/Snowflake" }; //$NON-NLS-1$
    }
    
    /*@Override //TODO: write  SnowflakeConnectionProperties
    public Class<? extends ComponentProperties>[] getNestedCompatibleComponentPropertiesClass() {
        return new Class[] { SnowflakeConnectionProperties.class };
    }*/
    
    @Override
    public Property[] getReturnProperties() {
        //return new Property[] { };
    	return new Property[] { RETURN_ERROR_MESSAGE_PROP, RETURN_TOTAL_RECORD_COUNT_PROP };
    }

    @Override
    public String getPngImagePath(ComponentImageType imageType) {
        switch (imageType) {
        case PALLETE_ICON_32X32:
            return "fileReader_icon32.png"; //$NON-NLS-1$
        default:
            return "fileReader_icon32.png"; //$NON-NLS-1$
        }
    }

    public String getMavenGroupId() {
        return "org.talend.components";
    }

    @Override
    public String getMavenArtifactId() {
        return "components-snowflake";
    }
    
/*    @Override //Unico TODO: delegate to subclass
    public Class<? extends ComponentProperties> getPropertyClass() {
        return SnowflakeProperties.class;
    }*/

/*    @Override
    public Source getRuntime() { //Unico TODO: delegate to subclass
        return new SnowflakeSource();
    }*/
}
