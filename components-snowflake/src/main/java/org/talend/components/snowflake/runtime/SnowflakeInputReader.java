package org.talend.components.snowflake.runtime;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.components.api.component.runtime.BoundedSource;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.snowflake.tsnowflakeinput.TSnowflakeInputProperties;
import org.talend.daikon.avro.AvroUtils;

/**
 * Simple implementation of a reader.
 */
public class SnowflakeInputReader extends SnowflakeReader<IndexedRecord> {

	private static final Logger LOG = LoggerFactory
			.getLogger(SnowflakeReader.class);

	private Connection con;
	protected ResultSet resultSet;

	//protected DBTemplate dbTemplate;

	private transient SnowflakeResultSetAdapterFactory factory;

	private transient Schema querySchema;

	public SnowflakeInputReader(RuntimeContainer container, SnowflakeSource source, TSnowflakeInputProperties props) {
		super(container, source);
		this.properties = props;
	}

	/*public void setDBTemplate(DBTemplate template) {
		this.dbTemplate = template;
	}*/

	/*private Schema getSchema() throws IOException {
		if (null == querySchema) {
			querySchema = new Schema.Parser().parse(properties.getSchema().schema
					.getStringValue());
		}
		return querySchema;
	}*/
	
	@Override
	protected Schema getSchema() throws IOException {
		TSnowflakeInputProperties inProperties = (TSnowflakeInputProperties) properties;
        if (querySchema == null) {
            querySchema = super.getSchema();
            if (inProperties.manualQuery.getValue()) {
                if (AvroUtils.isIncludeAllFields(properties.table.main.schema.getValue())) {
                    //TODO: remove the below commented code after verification
                	/*SObject currentSObject = getCurrentSObject();
                    Iterator<XmlObject> children = currentSObject.getChildren();*/
                    List<String> columnsName = new ArrayList<>();
                    /*while (children.hasNext()) {
                        columnsName.add(children.next().getName().getLocalPart());
                    }*/

                    List<Schema.Field> copyFieldList = new ArrayList<>();
                    for (Schema.Field se : querySchema.getFields()) {
                        if (columnsName.contains(se.name())) {
                            Schema.Field field = new Schema.Field(se.name(), se.schema(), se.doc(), se.defaultVal());
                            Map<String, Object> fieldProps = se.getObjectProps();
                            for (String propName : fieldProps.keySet()) {
                                Object propValue = fieldProps.get(propName);
                                if (propValue != null) {
                                    field.addProp(propName, propValue);
                                }
                            }
                            copyFieldList.add(field);
                        }
                    }
                    Map<String, Object> objectProps = querySchema.getObjectProps();
                    querySchema = Schema.createRecord(querySchema.getName(), querySchema.getDoc(), querySchema.getNamespace(),
                            querySchema.isError());
                    querySchema.getObjectProps().putAll(objectProps);
                    querySchema.setFields(copyFieldList);
                }
            }
        }
        return querySchema;
	}

	@Override
	protected SnowflakeResultSetAdapterFactory getFactory() throws IOException {
		if (null == factory) {
			factory = new SnowflakeResultSetAdapterFactory();
			factory.setSchema(getSchema());
		}
		return factory;
	}

	@Override
	public boolean start() throws IOException {
		try {
			con = getConnection().getConnection();
			Statement statement = con.createStatement();
			resultSet = statement.executeQuery(getQueryString(properties));
			return resultSet.next();
		} catch (Exception e) {
			e.printStackTrace();
			//TODO: anything else here?
			return false;
		}
	}

	@Override
	public boolean advance() throws IOException {
		try {
			return resultSet.next();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public IndexedRecord getCurrent()
			throws NoSuchElementException {
		try {
			return getFactory().convertToAvro(resultSet);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() throws IOException {
		try {
			resultSet.close();
			con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Double getFractionConsumed() {
		return null;
	}

	@Override
	public BoundedSource splitAtFraction(double fraction) {
		return null;
	}
}
