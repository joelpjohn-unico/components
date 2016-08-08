/**
 * 
 */
package org.talend.components.snowflake.runtime;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.components.api.component.runtime.SourceOrSink;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.api.exception.ComponentException;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.snowflake.SnowflakeConnectionProperties;
import org.talend.components.snowflake.SnowflakeProvideConnectionProperties;
import org.talend.components.snowflake.connection.Column;
import org.talend.components.snowflake.connection.ColumnExtension;
import org.talend.components.snowflake.connection.SnowflakeNativeConnection;
import org.talend.components.snowflake.connection.SnowflakeTableMetaData;
import org.talend.daikon.NamedThing;
import org.talend.daikon.SimpleNamedThing;
import org.talend.daikon.avro.SchemaConstants;
import org.talend.daikon.properties.ValidationResult;
import org.talend.daikon.properties.ValidationResult.Result;

/**
 * @author user
 *
 */
public class SnowflakeSourceOrSink implements SourceOrSink {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private transient static final Logger LOG = LoggerFactory.getLogger(SnowflakeSourceOrSink.class);

	protected SnowflakeProvideConnectionProperties properties;
	
    protected static final String KEY_CONNECTION = "Connection";
	
	/* (non-Javadoc)
	 * @see org.talend.components.api.component.runtime.SourceOrSink#initialize(org.talend.components.api.container.RuntimeContainer, org.talend.components.api.properties.ComponentProperties)
	 */
	@Override
	public void initialize(RuntimeContainer container, ComponentProperties properties) {
		this.properties = (SnowflakeProvideConnectionProperties) properties;
	}

	/* (non-Javadoc)
	 * @see org.talend.components.api.component.runtime.SourceOrSink#validate(org.talend.components.api.container.RuntimeContainer)
	 */
	@Override
	public ValidationResult validate(RuntimeContainer container) {
       ValidationResult vr = new ValidationResult();
        try {
        	if (null != connect(container)) {
        		vr.setStatus(Result.OK);
        		vr.setMessage("Connection Successful");
        	} else {
        		vr.setStatus(Result.ERROR);
        		vr.setMessage("Could not establish connection to the Snowflake DB");
        	}
        } catch (Exception ex) {
            return exceptionToValidationResult(ex);
        }
        return vr;	
     }
	
    protected static ValidationResult exceptionToValidationResult(Exception ex) {
        ValidationResult vr = new ValidationResult();
        vr.setMessage(ex.getMessage());
        vr.setStatus(ValidationResult.Result.ERROR);
        return vr;
    }
	
    public static ValidationResult validateConnection(SnowflakeProvideConnectionProperties properties) {
    	SnowflakeSourceOrSink sss = new SnowflakeSourceOrSink();
        sss.initialize(null, (ComponentProperties) properties);
        return sss.validate(null);
    }
    
    public SnowflakeConnectionProperties getConnectionProperties() {
    	return this.properties.getConnectionProperties();
    }
    
    protected SnowflakeNativeConnection connect(RuntimeContainer container) throws IOException{
    	
    	SnowflakeNativeConnection nativeConn = null;
        
    	SnowflakeConnectionProperties connProps = properties.getConnectionProperties();
        String refComponentId = connProps.getReferencedComponentId();
        Object sharedConn = null;
        // Using another component's connection
        if (refComponentId != null) {
            // In a runtime container
            if (container != null) {
                sharedConn = container.getComponentData(refComponentId, KEY_CONNECTION);
                if (sharedConn != null) {
                    if (sharedConn instanceof SnowflakeNativeConnection) {
                    	nativeConn = (SnowflakeNativeConnection) sharedConn;
                    } 
                    return nativeConn;
                }
                throw new IOException("Referenced component: " + refComponentId + " not connected");
            }
            // Design time
            connProps = connProps.getReferencedConnectionProperties();
        }

    	//Establish a new connection
        nativeConn = new SnowflakeNativeConnection();
    	Connection conn = null;
		String queryString = "";
		
		String warehouse = connProps.warehouse.getStringValue();
		String db = connProps.db.getStringValue();
		String schema = connProps.schema.getStringValue();
		String authenticator = connProps.authenticator.getStringValue();
		String user = connProps.userPassword.userId.getStringValue();
		String password = connProps.userPassword.password.getStringValue();
		String role = connProps.role.getStringValue();
		String tracing = connProps.tracing.getStringValue();
		String passcode = connProps.passcode.getStringValue();
		String passcodeInPassword = connProps.passcodeInPassword.getStringValue();
		String account = connProps.account.getStringValue();
		
		/* warehouse, db & schema are mandatory parameters. Still checking to be sure */
		if (null != warehouse  && !"".equals(warehouse)){
			queryString = queryString + "warehouse=" + warehouse;
		}
		if (null != db  && !"".equals(db)){
			queryString = queryString + "&db=" + db;
		}
		if (null != schema  && !"".equals(schema)){
			queryString = queryString + "&schema=" + schema;
		}		
		
		if (null != authenticator  && !"".equals(authenticator)){
			queryString = queryString + "&authenticator=" + authenticator;
		}		
		if (null != role  && !"".equals(role)){
			queryString = queryString + "&role=" + role;
		}		
		if (null != tracing  && !"".equals(tracing)){
			queryString = queryString + "&tracing=" + tracing;
		}		
		if (null != passcode  && !"".equals(passcode)){
			queryString = queryString + "&passcode=" + passcode;
		}		
		queryString = queryString + "&passcodeInPassword=" + passcodeInPassword;

		String connectionURL = "jdbc:snowflake://" + account + ".snowflakecomputing.com" + "/?" 
														+ queryString;
			
		String JDBC_DRIVER = "com.snowflake.client.jdbc.SnowflakeDriver";

		try {
			//DriverClassLoader driverClassLoader = new DriverClassLoader(com.snowflake.client.jdbc.SnowflakeConnection.class.getClassLoader());
			Driver driver = (Driver) Class.forName(JDBC_DRIVER).newInstance();
			DriverManager.registerDriver(new DriverWrapper(driver));

			conn = DriverManager.getConnection(connectionURL, user, password);
		} catch (Exception e) {
			e.printStackTrace();
			if (e.getMessage().contains("Communications link failure")) {
				//TODO: fill this in
			}
			//TODO: Log error
			throw new IOException(e);
		}
		
		nativeConn.setConnection(conn);

		if (container != null) {
            container.setComponentData(container.getCurrentComponentId(), KEY_CONNECTION, nativeConn);
        }

		return nativeConn;
    }

    public static List<NamedThing> getSchemaNames(RuntimeContainer container, SnowflakeProvideConnectionProperties properties)
            throws IOException {
        SnowflakeSourceOrSink ss = new SnowflakeSourceOrSink();
        ss.initialize(null, (ComponentProperties) properties);
        try {
            SnowflakeNativeConnection connection = ss.connect(container);
            return ss.getSchemaNames(container);
        } catch (Exception ex) {
            throw new ComponentException(exceptionToValidationResult(ex));
        }
    }
    
	/* (non-Javadoc)
	 * @see org.talend.components.api.component.runtime.SourceOrSink#getSchemaNames(org.talend.components.api.container.RuntimeContainer)
	 */
	@Override
	public List<NamedThing> getSchemaNames(RuntimeContainer container) throws IOException {
		return getSchemaNames(connect(container).getConnection());
	}

	/**
	 * Fetches the list of tables names in a database connection
	 * @param connection
	 * @return
	 * @throws IOException
	 */
	protected List<NamedThing> getSchemaNames(Connection connection) throws IOException {

		SnowflakeConnectionProperties connProps = properties.getConnectionProperties();
		String catalog = connProps.db.getStringValue();
		String dbSchema = connProps.schema.getStringValue();

		//Returns the list with a table names (for the wh, db and schema)
		List<NamedThing> returnList = new ArrayList<>();
		try {
			DatabaseMetaData metaData = connection.getMetaData();
			
			//Fetch all tables in the db and schema provided
			String[] types = {"TABLE"};

			ResultSet resultIter =  metaData.getTables(catalog, 
														dbSchema, 
														null, 
														types);
			
			//ResultSet resultIter =  metaData.getCatalogs();
			String tableName = null;
			while (resultIter.next()) {
				tableName = resultIter.getString("TABLE_NAME");
				returnList.add(new SimpleNamedThing(tableName, tableName));
			}
		} catch (SQLException se) {
			//TODO: Handle this
		}
		return returnList;
	}
	
	
    public static Schema getSchema(RuntimeContainer container, SnowflakeProvideConnectionProperties properties, String table)
            throws IOException {
        SnowflakeSourceOrSink ss = new SnowflakeSourceOrSink();
        ss.initialize(null, (ComponentProperties) properties);
        Connection connection = null;
        try {
            connection = ss.connect(container).getConnection();
        } catch (Exception ex) {
            throw new ComponentException(exceptionToValidationResult(ex));
        }
        return ss.getSchema(connection, table);
    }

    /* (non-Javadoc)
	 * @see org.talend.components.api.component.runtime.SourceOrSink#getEndpointSchema(org.talend.components.api.container.RuntimeContainer, java.lang.String)
	 */
	@Override
	public Schema getEndpointSchema(RuntimeContainer container, String schemaName) throws IOException {
		
		return getSchema(connect(container).getConnection(), schemaName);
	}
	

	protected Schema getSchema(Connection connection, String tableName) throws IOException {
        String catalog = properties.getConnectionProperties().db.getStringValue();
        String dbSchema = properties.getConnectionProperties().schema.getStringValue();
        
        Schema tableSchema = null;
        
		try {
			DatabaseMetaData metaData = connection.getMetaData();
			
			//List<SnowflakeTableMetaData> tableMDList = new ArrayList<>();
			
			ResultSet resultIter =  metaData.getColumns(catalog, 
														dbSchema, 
														tableName, 
														null);

			/*while (resultIter.next()) {
				SnowflakeTableMetaData tableMD = new SnowflakeTableMetaData();
				List<Column> columnsList = new ArrayList<>();
				
				tableMD.setTableName(resultIter.getString(3)); //resultIter.getString("TABLE_NAME")
				
				ResultSet columnsIter = metaData.getColumns(catalog, 
															schemaName, 
															tableMD.getTableName(), 
															null);
				while (columnsIter.next()) {
					Column column = new Column();
					ColumnExtension colExt = new ColumnExtension();
					
					String name = columnsIter.getString(4);
					String dType = columnsIter.getString(6);
					int length = columnsIter.getInt(16);
					int scale = columnsIter.getInt(9);
					int precision = columnsIter.getInt(7);
					String[] datatype = dType.split(" ");
	
					if (datatype.length > 1) {
						if ((datatype[1].equalsIgnoreCase("UNSIGNED"))) {
							dType = datatype[0];
						}
					}

					column.setName(name);
					column.setdType(dType);

					colExt.setScale(scale);
					colExt.setLength(length);
					
					if (dType.equalsIgnoreCase("DOUBLE")) {
						colExt.setPrecision(38);
						colExt.setScale(5);
					} else if (dType.startsWith("TIMESTAMP")) {
						colExt.setPrecision(38);
					} else if (dType.startsWith("DATE")) {
						colExt.setPrecision(38);
					} else if (dType.startsWith("TIME")) {
						colExt.setPrecision(38);
					} else if (dType.equalsIgnoreCase("BOOLEAN")) {
						colExt.setPrecision(7);
					} else if (dType.equalsIgnoreCase("OBJECT")) {
						colExt.setPrecision(65536);
					} else if (dType.equalsIgnoreCase("ARRAY")) {
						colExt.setPrecision(65536);
					} else if (dType.equalsIgnoreCase("VARIANT")) {
						colExt.setPrecision(65536);
					} else {
						colExt.setPrecision(precision);
					}
	
					// Populating the field extensions: Default value, isNullable
					String defValue = columnsIter.getString(13);
					boolean isNullable = "0".equals(columnsIter.getString(11)) ? false : true;
	
					colExt.setNullable(isNullable);
					colExt.setDefColValue(defValue);
					
					//TODO: isPrimaryKey, isUnique

					List<ColumnExtension> colExtList = new ArrayList<ColumnExtension>();
					colExtList.add(colExt);

					column.setExtensions(colExtList);
					columnsList.add(column);
				}
				
				tableMD.setColumn(columnsList);
				tableMDList.add(tableMD);
				
			}*/

			if (resultIter.next()) {
				
				tableSchema = SnowflakeAvroRegistry.get().inferSchema(resultIter);
				
				//Update the schema with Primary Key details
				if (null != tableSchema) {
					ResultSet keysIter =  metaData.getPrimaryKeys(catalog, 
																	dbSchema, 
																	tableName);

					List<String> pkColumns = new ArrayList<>(); //List of Primary Key columns for this table
					while(keysIter.next()) {
						pkColumns.add(keysIter.getString("COLUMN_NAME"));
					}
					
					for(Field f : tableSchema.getFields()) {
						if (pkColumns.contains(f.schema().getName())) {
							f.schema().addProp(SchemaConstants.TALEND_COLUMN_IS_KEY, true);
						}
					}
				}
				
			}
			
		} catch (SQLException se) {
			//TODO: Handle this.. logger
		}
    	
    	return tableSchema; 
    	
    }
	
	
	//-------------------------------------------------------------------------------------
	
	/**
	 * Inner class.<br>
	 * Custom driver wrapper class
	 * 
	 */

	public class DriverWrapper implements Driver {
		private Driver driver;

		public DriverWrapper(Driver d) {
			this.driver = d;
		}

		public boolean acceptsURL(String u) throws SQLException {
			return this.driver.acceptsURL(u);
		}

		public Connection connect(String u, Properties p) throws SQLException {
			return this.driver.connect(u, p);
		}

		public int getMajorVersion() {
			return this.driver.getMajorVersion();
		}

		public int getMinorVersion() {
			return this.driver.getMinorVersion();
		}

		public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
			return this.driver.getPropertyInfo(u, p);
		}

		public boolean jdbcCompliant() {
			return this.driver.jdbcCompliant();
		}

		@Override
		public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
			return this.driver.getParentLogger();
		}
	}
	
}
