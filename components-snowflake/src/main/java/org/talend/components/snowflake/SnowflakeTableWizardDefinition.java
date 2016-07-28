package org.talend.components.snowflake;

import org.talend.components.api.Constants;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.api.wizard.AbstractComponentWizardDefintion;
import org.talend.components.api.wizard.ComponentWizard;
import org.talend.components.api.wizard.ComponentWizardDefinition;
import org.talend.components.api.wizard.WizardImageType;

import aQute.bnd.annotation.component.Component;

@Component(name = Constants.COMPONENT_WIZARD_BEAN_PREFIX
        + SnowflakeTableWizardDefinition.COMPONENT_WIZARD_NAME, provide = ComponentWizardDefinition.class)
public class SnowflakeTableWizardDefinition extends AbstractComponentWizardDefintion {

    public static final String COMPONENT_WIZARD_NAME = "snowflake.table"; //$NON-NLS-1$

    @Override
    public String getName() {
        return COMPONENT_WIZARD_NAME;
    }

    @Override
    public ComponentWizard createWizard(String location) {
        return new SnowflakeTableWizard(this, location);
    }

    @Override
    public boolean supportsProperties(Class<? extends ComponentProperties> propertiesClass) {
        return propertiesClass.isAssignableFrom(SnowflakeConnectionProperties.class);
    }

    @Override
    public ComponentWizard createWizard(ComponentProperties properties, String location) {
        SnowflakeTableWizard wizard = (SnowflakeTableWizard) createWizard(location);
        wizard.setupProperties((SnowflakeConnectionProperties) properties);
        return wizard;
    }

    @Override
    public String getPngImagePath(WizardImageType imageType) {
        switch (imageType) {
        case TREE_ICON_16X16:
            return "connectionWizardIcon.png"; //$NON-NLS-1$
        case WIZARD_BANNER_75X66:
            return "snowflakeWizardBanner.png"; //$NON-NLS-1$ //TODO: check this file

        default:
            // will return null
        }
        return null;
    }
}
