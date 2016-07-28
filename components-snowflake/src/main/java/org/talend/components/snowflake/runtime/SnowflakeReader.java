package org.talend.components.snowflake.runtime;

import java.io.IOException;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.api.component.runtime.AbstractBoundedReader;
import org.talend.components.api.component.runtime.BoundedSource;
import org.talend.components.api.component.runtime.Result;
import org.talend.components.snowflake.SnowflakeDefinition;
import org.talend.components.snowflake.connection.SnowflakeNativeConnection;
import org.talend.components.snowflake.tsnowflakeinput.TSnowflakeInputProperties;
import org.talend.daikon.avro.AvroUtils;
import org.talend.daikon.avro.converter.IndexedRecordConverter;

/**
 * Simple implementation of a reader.
 */
public abstract class SnowflakeReader<T> extends AbstractBoundedReader<T> {

    /** Default serial version UID. */
    //private static final long serialVersionUID = 1L;

    //private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeDefinition.class);
    
	private transient SnowflakeNativeConnection connection;
	
	private transient IndexedRecordConverter<?, IndexedRecord> factory;
	
	protected transient Schema querySchema;
	
	protected SnowflakeConnectionTableProperties properties; 
	
	protected int dataCount;

    private RuntimeContainer container;

    //private final String filename;
    private final String tableName; //TODO: should come from SnowflakeConnectionTableProperties

    /*private boolean started = false; No need for now

    private BufferedReader reader = null;

    private transient String current;*/

    public SnowflakeReader(RuntimeContainer container, 
    							BoundedSource source, String tableName) {
    	super(source);
        this.container = container;
        this.tableName = tableName;
    }

    protected SnowflakeNativeConnection getConnection() throws IOException {
    	if (null == connection) {
    		connection = ((SnowflakeSource) getCurrentSource()).connect(container);
    	}
    	return connection;
    }
    
    protected IndexedRecordConverter<?, IndexedRecord> getFactory() throws IOException {
    	if (null == factory) {
    		factory = new SnowflakeResultSetAdapterFactory();
    	}
    	return factory;
    }
    
    protected Schema getSchema() throws IOException {
    	if (querySchema == null) {
            querySchema = properties.table.main.schema.getValue();
            if (AvroUtils.isIncludeAllFields(querySchema)) {
                String tableName = null;
                if (properties instanceof SnowflakeConnectionTableProperties) {
                    tableName = properties.table.tableName.getStringValue();
                }
                querySchema = getCurrentSource().getEndpointSchema(container, tableName);
            }
    	}
        return querySchema;
    }

    //TODO: switch commented and uncommented lines below
    //protected String getQueryString(SnowflakeConnectionTableProperties properties) throws IOException {
    protected String getQueryString(TSnowflakeInputProperties properties) throws IOException {
        String condition = null;
        if (properties instanceof TSnowflakeInputProperties) {
        	TSnowflakeInputProperties inProperties = (TSnowflakeInputProperties) properties;
            if (inProperties.manualQuery.getValue()) {
                return inProperties.query.getStringValue();
            } else {
                condition = inProperties.condition.getStringValue();
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("select "); //$NON-NLS-1$
        int count = 0;
        for (Schema.Field se : getSchema().getFields()) {
            if (count++ > 0) {
                sb.append(", "); //$NON-NLS-1$
            }
            sb.append(se.name());
        }
        sb.append(" from "); //$NON-NLS-1$
        //sb.append(properties.table.tableName.getStringValue()); //TODO: remove commenting
        if (condition != null && condition.trim().length() > 0) {
            sb.append(" where ");
            sb.append(condition);
        }
        return sb.toString();
    }

/*    @Override
    public boolean start() throws IOException {
        started = true;
        LOGGER.debug("open: " + filename); //$NON-NLS-1$
        reader = new BufferedReader(new FileReader(filename));
        current = reader.readLine();
        return current != null;
    }

    @Override
    public boolean advance() throws IOException {
        current = reader.readLine();
        return current != null;
    }

    @Override
    public String getCurrent() throws NoSuchElementException {
        if (!started) {
            throw new NoSuchElementException();
        }
        return current;
    }
*/
    @Override
    public void close() throws IOException {
        /*reader.close();
        LOGGER.debug("close: " + filename); //$NON-NLS-1$
*/    }

    @Override
    public Map<String, Object> getReturnValues() {
        Result result = new Result();
        result.totalCount = dataCount;
        return result.toMap();
    }

}
