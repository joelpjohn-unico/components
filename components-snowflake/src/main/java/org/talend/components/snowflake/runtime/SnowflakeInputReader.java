package org.talend.components.snowflake.runtime;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.NoSuchElementException;

import org.apache.avro.Schema;
import org.apache.avro.generic.IndexedRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.components.api.component.runtime.AbstractBoundedReader;
import org.talend.components.api.component.runtime.BoundedSource;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.daikon.avro.converter.IndexedRecordConverter;
import org.talend.daikon.properties.Properties;

/**
 * Simple implementation of a reader.
 */
public abstract class SnowflakeInputReader extends
		AbstractBoundedReader<String> {

	private static final Logger LOG = LoggerFactory
			.getLogger(SnowflakeReader.class);

	protected DBInputProperties properties;

	protected RuntimeContainer adaptor;

	protected Connection conn;

	protected ResultSet resultSet;

	protected DBTemplate dbTemplate;

	private transient ResultSetAdapterFactory factory;

	private transient Schema querySchema;

	public DBReader(RuntimeContainer adaptor, DBSource source, DBInputProperties props) {
		super(adaptor, source);
		this.adaptor = adaptor;
		this.properties = props;
	}

	public void setDBTemplate(DBTemplate template) {
		this.dbTemplate = template;
	}

	private Schema getSchema() throws IOException {
		if (null == querySchema) {
			querySchema = new Schema.Parser().parse(properties.schema.schema
					.getStringValue());
		}
		return querySchema;
	}

	private ResultSetAdapterFactory getFactory() throws IOException {
		if (null == factory) {
			factory = new ResultSetAdapterFactory();
			factory.setSchema(getSchema());
		}
		return factory;
	}

	@Override
	public boolean start() throws IOException {
		try {
			conn = dbTemplate.connect(Properties.getConnectionProperties());
			Statement statement = conn.createStatement();
			resultSet = statement.executeQuery(properties.sql.getStringValue());
			return resultSet.next();
		} catch (Exception e) {
			e.printStackTrace();
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
	public IndexedRecordConverter<SpecificT, IndexedRecord> getCurrent()
			throws NoSuchElementException {
		try {
			return getFactory().convertToAvro(resultSet);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Instant getCurrentTimestamp() throws NoSuchElementException {
		return null;
	}

	@Override
	public void close() throws IOException {
		try {
			resultSet.close();
			conn.close();
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
