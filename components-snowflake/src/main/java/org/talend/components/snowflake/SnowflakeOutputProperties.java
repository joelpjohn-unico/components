package org.talend.components.snowflake;

import org.apache.avro.Schema;
import org.talend.components.api.component.Connector;
import org.talend.components.api.component.PropertyPathConnector;
import org.talend.components.common.SchemaProperties;
import org.talend.components.snowflake.runtime.SnowflakeConnectionTableProperties;
import org.talend.daikon.properties.ValidationResult;
import org.talend.daikon.properties.presentation.Form;
import org.talend.daikon.properties.presentation.Widget;
import org.talend.daikon.properties.property.Property;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.talend.daikon.properties.presentation.Widget.widget;
import static org.talend.daikon.properties.property.PropertyFactory.newEnum;
import static org.talend.daikon.properties.property.PropertyFactory.newString;

public class SnowflakeOutputProperties extends SnowflakeConnectionTableProperties {

    public enum OutputAction {
        INSERT,
        UPDATE,
        UPSERT,
        DELETE
    }

    public Property<OutputAction> outputAction = newEnum("outputAction", OutputAction.class); // $NON-NLS-1$

    public Property<String> upsertKeyColumn = newString("upsertKeyColumn"); //$NON-NLS-1$

    //
    // Advanced
    //
    public SnowflakeUpsertRelationTable sfUpsertRelationTable = new SnowflakeUpsertRelationTable("sfUpsertRelationTable");

    //
    // Collections
    //
    protected transient PropertyPathConnector FLOW_CONNECTOR = new PropertyPathConnector(Connector.MAIN_NAME, "schemaFlow");

    protected transient PropertyPathConnector REJECT_CONNECTOR = new PropertyPathConnector(Connector.REJECT_NAME, "schemaReject");

    public SchemaProperties schemaFlow = new SchemaProperties("schemaFlow"); //$NON-NLS-1$

    public SchemaProperties schemaReject = new SchemaProperties("schemaReject"); //$NON-NLS-1$

    public SnowflakeOutputProperties(String name) {
        super(name);
    }

    // Have to use an explicit class to get the override of afterTableName(), an anonymous
    // class cannot be public and thus cannot be called.
    public class TableSubclass extends SnowflakeTableProperties {

        public TableSubclass(String name) {
            super(name);
        }

        @Override
        public ValidationResult afterTableName() throws Exception {
            ValidationResult validationResult = super.afterTableName();
            List<String> fieldNames = getFieldNames(main.schema);

            if (isUpsertKeyColumnClosedList()) {
                upsertKeyColumn.setPossibleValues(fieldNames);
            }

            sfUpsertRelationTable.columnName.setPossibleValues(fieldNames);
            return validationResult;
        }
    }

    protected boolean isUpsertKeyColumnClosedList() {
        return true;
    }

    public static final boolean POLY = true;

    public void beforeUpsertKeyColumn() {
        if (isUpsertKeyColumnClosedList()) {
            upsertKeyColumn.setPossibleValues(getFieldNames(table.main.schema));
        }
    }

    public void beforeUpsertRelationTable() {
    	sfUpsertRelationTable.columnName.setPossibleValues(getFieldNames(table.main.schema));
    }

    @Override
    public void setupProperties() {
        super.setupProperties();

        outputAction.setValue(OutputAction.INSERT);

        setupRejectSchema();

        table = new TableSubclass("table");
        table.connection = connection;
        table.setupProperties();
        sfUpsertRelationTable.setUsePolymorphic(false);
    }

    @Override
    public void setupLayout() {
        super.setupLayout();
        Form mainForm = getForm(Form.MAIN);
        mainForm.addRow(outputAction);

        if (isUpsertKeyColumnClosedList()) {
            mainForm.addColumn(widget(upsertKeyColumn).setWidgetType(Widget.ENUMERATION_WIDGET_TYPE));
        } else {
            mainForm.addColumn(upsertKeyColumn);
        }

        Form advancedForm = getForm(Form.ADVANCED); //TODO: check if this has already been defined.
        advancedForm.addRow(widget(sfUpsertRelationTable).setWidgetType(Widget.TABLE_WIDGET_TYPE));
        // check
        // I18N
    }

    public void afterOutputAction() {
        refreshLayout(getForm(Form.MAIN));
        refreshLayout(getForm(Form.ADVANCED)); //TODO:??
    }

    @Override
    public void refreshLayout(Form form) {
        super.refreshLayout(form);

        if (form.getName().equals(Form.MAIN)) {
            Form advForm = getForm(Form.ADVANCED);
            if (advForm != null) {
                boolean isUpsert = OutputAction.UPSERT.equals(outputAction.getValue());
                form.getWidget(upsertKeyColumn.getName()).setHidden(!isUpsert);
                advForm.getWidget(sfUpsertRelationTable.getName()).setHidden(!isUpsert);
                if (isUpsert) {
                    beforeUpsertKeyColumn();
                    beforeUpsertRelationTable();
                }
            }
        }
    }

    @Override
    protected Set<PropertyPathConnector> getAllSchemaPropertiesConnectors(boolean isOutputConnection) {
        HashSet<PropertyPathConnector> connectors = new HashSet<>();
        if (isOutputConnection) {
            connectors.add(FLOW_CONNECTOR);
            connectors.add(REJECT_CONNECTOR);
        } else {
            connectors.add(MAIN_CONNECTOR);
        }
        return connectors;
    }

    protected List<String> getFieldNames(Property schema) {
        Schema s = (Schema) schema.getValue();
        List<String> fieldNames = new ArrayList<>();
        for (Schema.Field f : s.getFields()) {
            fieldNames.add(f.name());
        }
        return fieldNames;
    }

    protected void setupRejectSchema() {
        // left empty for subclass to override
    }

}
