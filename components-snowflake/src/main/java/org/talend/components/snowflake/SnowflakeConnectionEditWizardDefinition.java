package org.talend.components.snowflake;

import org.talend.components.api.Constants;
import org.talend.components.api.wizard.ComponentWizardDefinition;

import aQute.bnd.annotation.component.Component;

@Component(name = Constants.COMPONENT_WIZARD_BEAN_PREFIX
        + SnowflakeConnectionEditWizardDefinition.COMPONENT_WIZARD_NAME, provide = ComponentWizardDefinition.class)
public class SnowflakeConnectionEditWizardDefinition extends SnowflakeConnectionWizardDefinition {

    public static final String COMPONENT_WIZARD_NAME = "snowflake.edit"; //$NON-NLS-1$

    @Override
    public String getName() {
        return COMPONENT_WIZARD_NAME;
    }

    @Override
    public boolean isTopLevel() {
        return false;
    }

}
