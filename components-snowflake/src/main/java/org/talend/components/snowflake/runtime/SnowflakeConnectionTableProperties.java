package org.talend.components.snowflake.runtime;

import org.apache.avro.Schema;
import org.talend.components.api.component.Connector;
import org.talend.components.api.component.PropertyPathConnector;
import org.talend.components.common.FixedConnectorsComponentProperties;
import org.talend.components.snowflake.SnowflakeConnectionProperties;
import org.talend.components.snowflake.SnowflakeProvideConnectionProperties;
import org.talend.components.snowflake.SnowflakeTableProperties;
import org.talend.daikon.properties.presentation.Form;

public abstract class SnowflakeConnectionTableProperties extends FixedConnectorsComponentProperties
implements SnowflakeProvideConnectionProperties {
	
    // Collections
    //
    public SnowflakeConnectionProperties connection = new SnowflakeConnectionProperties("connection"); //$NON-NLS-1$

    public SnowflakeTableProperties table;

    protected transient PropertyPathConnector MAIN_CONNECTOR = new PropertyPathConnector(Connector.MAIN_NAME, "table.main");

    public SnowflakeConnectionTableProperties(String name) {
        super(name);
    }

    @Override
    public void setupProperties() {
        super.setupProperties();
        // Allow for subclassing
        table = new SnowflakeTableProperties("table");
        table.connection = connection;
    }

    public Schema getSchema() {
        return table.main.schema.getValue();
    }

    @Override
    public void setupLayout() {
        super.setupLayout();
        Form mainForm = new Form(this, Form.MAIN);
        mainForm.addRow(connection.getForm(Form.REFERENCE));
        mainForm.addRow(table.getForm(Form.REFERENCE));

        Form advancedForm = new Form(this, Form.ADVANCED);
        advancedForm.addRow(connection.getForm(Form.ADVANCED));
    }

    @Override
    public SnowflakeConnectionProperties getConnectionProperties() {
        return connection;
    }

    @Override
    public void refreshLayout(Form form) {
        super.refreshLayout(form);
        for (Form childForm : connection.getForms()) {
            connection.refreshLayout(childForm);
        }
    }


}
