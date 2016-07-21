package org.talend.components.snowflake;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.talend.components.api.component.runtime.BoundedSource;
import org.talend.components.api.component.runtime.Reader;
import org.talend.components.api.component.runtime.Source;
import org.talend.components.api.exception.error.ComponentsErrorCode;
import org.talend.components.api.service.ComponentService;
import org.talend.components.api.service.internal.ComponentServiceImpl;
import org.talend.components.api.test.ComponentTestUtils;
import org.talend.components.api.test.SimpleComponentRegistry;
import org.talend.daikon.exception.TalendRuntimeException;

@SuppressWarnings("nls")
public class SnowflakeTest {

    @Rule
    public ErrorCollector errorCollector = new ErrorCollector();

    private ComponentServiceImpl componentService;

    @Before
    public void initializeComponentRegistryAndService() {
        // reset the component service
        componentService = null;
    }

    // default implementation for pure java test. 
    public ComponentService getComponentService() {
        if (componentService == null) {
            SimpleComponentRegistry testComponentRegistry = new SimpleComponentRegistry();
            //TODO: Build the classes below and remove comments
            //testComponentRegistry.addComponent(SnowflakeInputDefinition.COMPONENT_NAME, new SnowflakeInputDefinition());
            //testComponentRegistry.addComponent(SnowflakeOutputDefinition.COMPONENT_NAME, new SnowflakeOutDefinition());
            componentService = new ComponentServiceImpl(testComponentRegistry);
        }
        return componentService;
    }

    //TODO: Remove @Ignore once the classes have been re-factored
    //@Ignore
    //@Test
    /*public void testSnowflakeRuntime() throws Exception {
        SnowflakeDefinition def = (SnowflakeDefinition) getComponentService().getComponentDefinition("Snowflake");
        SnowflakeConnectionProperties props = (SnowflakeConnectionProperties) getComponentService().getComponentProperties("Snowflake");

        // Set up the test schema - not really used for anything now
        Schema schema = SchemaBuilder.builder().record("testRecord").fields().name("field1").type().stringType().noDefault().endRecord();
        props.schema.schema.setValue(schema);

        File temp = File.createTempFile("SnowflaketestFile", ".txt");
        try {
            PrintWriter writer = new PrintWriter(temp.getAbsolutePath(), "UTF-8");
            writer.println("The first line");
            writer.println("The second line");
            writer.close();

            props.filename.setValue(temp.getAbsolutePath());
            Source source = def.getRuntime();
            source.initialize(null, props);
            //assertThat(source, instanceOf(SnowflakeSource.class));

            Reader<?> reader = ((BoundedSource) source).createReader(null);
            assertThat(reader.start(), is(true));
            assertThat(reader.getCurrent(), is((Object) "The first line"));
            // No auto advance when calling getCurrent more than once.
            assertThat(reader.getCurrent(), is((Object) "The first line"));
            assertThat(reader.advance(), is(true));
            assertThat(reader.getCurrent(), is((Object) "The second line"));
            assertThat(reader.advance(), is(false));
        } finally {// remote the temp file
            temp.delete();
        }
    }*/

}
