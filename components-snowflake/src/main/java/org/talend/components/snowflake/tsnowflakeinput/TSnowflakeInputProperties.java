/**
 * 
 */
package org.talend.components.snowflake.tsnowflakeinput;

import static org.talend.daikon.properties.property.PropertyFactory.newBoolean;
import static org.talend.daikon.properties.property.PropertyFactory.newProperty;

import java.util.Collections;
import java.util.Set;

import org.talend.components.snowflake.SnowflakeConnectionProperties;
import org.talend.components.snowflake.SnowflakeProvideConnectionProperties;
import org.talend.components.snowflake.runtime.SnowflakeConnectionTableProperties;
import org.talend.components.api.component.PropertyPathConnector;
import org.talend.daikon.properties.presentation.Form;
import org.talend.daikon.properties.property.Property;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author user
 *
 */
public class TSnowflakeInputProperties extends
		SnowflakeConnectionTableProperties implements
		SnowflakeProvideConnectionProperties {

    public Property<String> condition = newProperty("condition"); //$NON-NLS-1$

    public Property<Boolean> manualQuery = newBoolean("manualQuery"); //$NON-NLS-1$

    public Property<String> query = newProperty("query"); //$NON-NLS-1$
	
	
	public TSnowflakeInputProperties(@JsonProperty("name") String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.talend.components.snowflake.SnowflakeProvideConnectionProperties#getConnectionProperties()
	 */
	@Override
	public SnowflakeConnectionProperties getConnectionProperties() {
		return this.connection;
	}

	@Override
	protected Set<PropertyPathConnector> getAllSchemaPropertiesConnectors(
			boolean isOutputConnection) {
        if (isOutputConnection) {
            return Collections.singleton(MAIN_CONNECTOR);
        } else {
            return Collections.EMPTY_SET;
        }
	}
	
	@Override
	public void setupProperties() {
        super.setupProperties();
        manualQuery.setValue(false);
    }
	
	@Override
    public void setupLayout() {
        super.setupLayout();
        Form mainForm = getForm(Form.MAIN);
        mainForm.addRow(manualQuery);
        mainForm.addRow(condition);
        mainForm.addRow(query);
	}
	
	@Override
	public void refreshLayout(Form form) {
        super.refreshLayout(form);
        if (form.getName().equals(Form.MAIN)) {
            form.getWidget(query.getName()).setHidden(!manualQuery.getValue());
            form.getWidget(condition.getName()).setHidden(manualQuery.getValue());
        }
	}
 
}
