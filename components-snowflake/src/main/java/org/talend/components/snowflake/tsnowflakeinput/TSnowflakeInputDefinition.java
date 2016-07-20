import aQute.bnd.annotation.component.Component;
import org.talend.components.api.Constants;
import org.talend.components.api.component.ComponentDefinition;
import org.talend.components.api.component.InputComponentDefinition;
import org.talend.components.api.component.runtime.Source;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.salesforce.SalesforceDefinition;
import org.talend.components.salesforce.SalesforceModuleProperties;
import org.talend.components.salesforce.runtime.SalesforceSource;

/**
 * Component that can connect to a snowflake system and get some data out of it.
 */

@Component(name = Constants.COMPONENT_BEAN_PREFIX
        + TSnowflakeInputDefinition.COMPONENT_NAME, provide = ComponentDefinition.class)

public class TSnowflakeInputDefinition extends SalesforceDefinition implements InputComponentDefinition {
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
	        return TSnowflakeInputProperties.class;
	    }

	    @SuppressWarnings("unchecked")
	    @Override
	    public Class<? extends ComponentProperties>[] getNestedCompatibleComponentPropertiesClass() {
	        return concatPropertiesClasses(super.getNestedCompatibleComponentPropertiesClass(),
	                new Class[] { SnowflakeModuleProperties.class });
	    }

	    @Override
	    public Source getRuntime() {
	        return new SnowflakeSource();
	    }

}
