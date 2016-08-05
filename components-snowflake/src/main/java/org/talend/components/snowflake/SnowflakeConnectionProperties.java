package org.talend.components.snowflake;

import static org.talend.daikon.properties.presentation.Widget.widget;
import static org.talend.daikon.properties.property.PropertyFactory.*;

import org.talend.components.api.properties.ComponentPropertiesImpl;
import org.talend.components.api.properties.ComponentReferenceProperties;
import org.talend.components.api.properties.ComponentReferencePropertiesEnclosing;
import org.talend.components.common.UserPasswordProperties;
import org.talend.components.snowflake.tsnowflakeconnection.TSnowflakeConnectionDefinition;
import org.talend.daikon.properties.PresentationItem;
import org.talend.daikon.properties.property.Property;
import org.talend.daikon.properties.presentation.Form;
import org.talend.daikon.properties.presentation.Widget;

/**
 * The ComponentProperties subclass provided by a component stores the 
 * configuration of a component and is used for:
 * 
 * <ol>
 * <li>Specifying the format and type of information (properties) that is 
 *     provided at design-time to configure a component for run-time,</li>
 * <li>Validating the properties of the component at design-time,</li>
 * <li>Containing the untyped values of the properties, and</li>
 * <li>All of the UI information for laying out and presenting the 
 *     properties to the user.</li>
 * </ol>
 * 
 * The SnowflakeProperties has two properties:
 * <ol>
 * <li>{code filename}, a simple property which is a String containing the 
 *     file path that this component will read.</li>
 * <li>{code schema}, an embedded property referring to a Schema.</li>
 * </ol>
 */
public class SnowflakeConnectionProperties extends ComponentPropertiesImpl
implements SnowflakeProvideConnectionProperties, ComponentReferencePropertiesEnclosing {

	private static final String USERPASSWORD = "userPassword";
	public static final String FORM_WIZARD = "Wizard";
	
    // Only for the wizard use
    public Property<String> name = newString("name").setRequired();

	public enum Authenticator {
		SNOWFLAKE("snowflake"),
		OKTA("okta");
		
		String value;
		
		private Authenticator(String val) {
			this.value = val;
		}
	}
	
	public enum Tracing {
		OFF("OFF"),
		SEVERE("SEVERE"),
		WARNING("WARNING"),
		INFO("INFO"),
		CONFIG("CONFIG"),
		FINE("FINE"),
		FINER("FINER"),
		FINEST("FINEST"),
		ALL("ALL");
		
		String value;
		
		private Tracing(String val) {
			this.value = val;
		}
	}
	
	public enum PasscodeInPassword {
		ON(true),
		OFF(false);
		
		boolean value;
		
		private PasscodeInPassword(boolean val) {
			this.value = val;
		}
	}
	
	//Ref: https://docs.snowflake.net/manuals/user-guide/jdbc-configure.html
	public UserPasswordProperties userPassword = new UserPasswordProperties(USERPASSWORD);
    public Property<String> account = newString("account").setRequired(); //$NON-NLS-1$
    public Property<String> warehouse = newString("warehouse").setRequired(); //$NON-NLS-1$
    public Property<String> db = newString("db").setRequired(); //$NON-NLS-1$
    public Property<String> schema = newString("schema").setRequired(); //$NON-NLS-1$
    public Property<Authenticator> authenticator = newEnum("authenticator", Authenticator.class); //$NON-NLS-1$
    public Property<String> role = newString("role"); //$NON-NLS-1$
    public Property<Tracing> tracing = newEnum("tracing", Tracing.class); //$NON-NLS-1$
    public Property<String> passcode = newString("passcode"); //$NON-NLS-1$
    public Property<PasscodeInPassword> passcodeInPassword = newEnum("passcodeInPassword", PasscodeInPassword.class); //$NON-NLS-1$
    
    // Presentation items
    public PresentationItem testConnection = new PresentationItem("testConnection", "Test connection");
    public PresentationItem advanced = new PresentationItem("advanced", "Advanced...");
    
    
    //protected transient PropertyPathConnector mainConnector = new PropertyPathConnector(Connector.MAIN_NAME, "schema");
 
    public ComponentReferenceProperties referencedComponent = new ComponentReferenceProperties("referencedComponent", this);

    public SnowflakeConnectionProperties(String name) {
        super(name);
    }

    @Override
    public void setupProperties() {
        super.setupProperties();
        // Code for property initialization goes here
        
        authenticator.setValue(Authenticator.SNOWFLAKE);
        tracing.setValue(Tracing.INFO);
        passcodeInPassword.setValue(PasscodeInPassword.OFF);
    }

    @Override
    public void setupLayout() {
        super.setupLayout();
        
        Form wizardForm = Form.create(this, FORM_WIZARD);
        wizardForm.addRow(name);
        wizardForm.addRow(userPassword.getForm(Form.MAIN));
        wizardForm.addRow(account);
        wizardForm.addRow(warehouse);
        wizardForm.addRow(db);
        wizardForm.addRow(schema);
        wizardForm.addRow(widget(advanced).setWidgetType(Widget.BUTTON_WIDGET_TYPE));
        wizardForm.addColumn(widget(testConnection).setLongRunning(true).setWidgetType(Widget.BUTTON_WIDGET_TYPE));
        
        Form mainForm = Form.create(this, Form.MAIN);
        mainForm.addRow(userPassword.getForm(Form.MAIN));
        mainForm.addRow(account);
        mainForm.addRow(warehouse);
        mainForm.addRow(db);
        mainForm.addRow(schema);
        
        Form advancedForm = Form.create(this, Form.ADVANCED);
        advancedForm.addRow(widget(authenticator).setWidgetType(Widget.ENUMERATION_WIDGET_TYPE));
        advancedForm.addRow(widget(tracing).setWidgetType(Widget.ENUMERATION_WIDGET_TYPE));
        advancedForm.addRow(widget(passcodeInPassword).setWidgetType(Widget.ENUMERATION_WIDGET_TYPE));
        advancedForm.addRow(role);
        advancedForm.addRow(passcode);
        advanced.setFormtoShow(advancedForm);
        
        //form.addRow(schema.getForm(Form.REFERENCE));
        //form.addRow(filename);
        
        // A form for a reference to a connection, used in a tSnowflakeInput for example
        Form refForm = Form.create(this, Form.REFERENCE);
        Widget compListWidget = widget(referencedComponent).setWidgetType(Widget.COMPONENT_REFERENCE_WIDGET_TYPE);
        referencedComponent.componentType.setValue(TSnowflakeConnectionDefinition.COMPONENT_NAME);
        refForm.addRow(compListWidget);
        refForm.addRow(mainForm);
        
    }
    
    @Override
    public void afterReferencedComponent() {
        refreshLayout(getForm(Form.MAIN));
        refreshLayout(getForm(Form.REFERENCE));
        refreshLayout(getForm(Form.ADVANCED));
    }
    
    
    //TODO: refreshLayout (currently handled in super)
    @Override
    public void refreshLayout(Form form) {
        super.refreshLayout(form);

        String refComponentIdValue = getReferencedComponentId();
        boolean useOtherConnection = refComponentIdValue != null
                && refComponentIdValue.startsWith(TSnowflakeConnectionDefinition.COMPONENT_NAME);
        if (form.getName().equals(Form.MAIN) || form.getName().equals(FORM_WIZARD)) {
            if (useOtherConnection) {
                form.getWidget(USERPASSWORD).setHidden(true);
                //TODO: Probably need to hide the other form elements
                form.getWidget(account.getName()).setHidden(true);
                form.getWidget(warehouse.getName()).setHidden(true);
                form.getWidget(db.getName()).setHidden(true);
                form.getWidget(schema.getName()).setHidden(true);

            } else {
            	//Do nothing
                form.setHidden(false);
            	form.getWidget(USERPASSWORD).setHidden(false);
                form.getWidget(account.getName()).setHidden(false);
                form.getWidget(warehouse.getName()).setHidden(false);
                form.getWidget(db.getName()).setHidden(false);
                form.getWidget(schema.getName()).setHidden(false);

            }
        }

        if (form.getName().equals(Form.ADVANCED)) {
            if (useOtherConnection) {
                form.setHidden(true);
            } else {
                form.setHidden(false);
                //TODO: check if anything else needs to be handled
            }
        }
    }
    

	@Override
	public SnowflakeConnectionProperties getConnectionProperties() {
		return this;
	}

    /*@Override
    protected Set<PropertyPathConnector> getAllSchemaPropertiesConnectors(boolean isOutputComponent) {
        if (isOutputComponent) {
            return Collections.singleton(mainConnector);
        }
        return Collections.emptySet();
    }*/
	
    public String getReferencedComponentId() {
        return referencedComponent.componentInstanceId.getStringValue();
    }

    public SnowflakeConnectionProperties getReferencedConnectionProperties() {
        SnowflakeConnectionProperties refProps = (SnowflakeConnectionProperties) referencedComponent.componentProperties;
        if (refProps != null) {
            return refProps;
        }
        return null;
    }
	

}
