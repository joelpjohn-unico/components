
package org.talend.components.snowflake;

import org.talend.components.api.component.AbstractComponentDefinition;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.daikon.properties.property.Property;

/**
 * The SnowflakeDefinition acts as an entry point for all of services that 
 * a component provides to integrate with the Studio (at design-time) and other 
 * components (at run-time).
 */
public abstract class SnowflakeDefinition extends AbstractComponentDefinition {

    public SnowflakeDefinition(String componentName) {
        super(componentName);
    }

    @Override
    public String[] getFamilies() {
        return new String[] { "Cloud/Snowflake" }; //$NON-NLS-1$
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends ComponentProperties>[] getNestedCompatibleComponentPropertiesClass() {
        return new Class[] { SnowflakeConnectionProperties.class };
    }
    
    @Override
    public Property[] getReturnProperties() {
    	return new Property[] { RETURN_ERROR_MESSAGE_PROP, RETURN_TOTAL_RECORD_COUNT_PROP };
    }

    /*@Override //Unico TODO: Delegate to Subclasses
    public String getPngImagePath(ComponentImageType imageType) {
        switch (imageType) {
        case PALLETE_ICON_32X32:
            return "fileReader_icon32.png"; //$NON-NLS-1$
        default:
            return "fileReader_icon32.png"; //$NON-NLS-1$
        }
    }*/

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
