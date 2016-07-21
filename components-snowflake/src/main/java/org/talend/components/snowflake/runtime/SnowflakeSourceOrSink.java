/**
 * 
 */
package org.talend.components.snowflake.runtime;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.talend.components.api.component.runtime.SourceOrSink;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.api.exception.ComponentException;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.snowflake.SnowflakeConnectionProperties;
import org.talend.components.snowflake.SnowflakeProvideConnectionProperties;
import org.talend.components.snowflake.connection.SnowflakeNativeConnection;
import org.talend.daikon.NamedThing;
import org.talend.daikon.SimpleNamedThing;
import org.talend.daikon.properties.ValidationResult;
import org.talend.daikon.properties.ValidationResult.Result;

/**
 * @author user
 *
 */
public class SnowflakeSourceOrSink implements SourceOrSink {

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
        	if (false) throw new IOException(); //TODO: remove this
        } catch (IOException ex) {
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
    
    protected SnowflakeNativeConnection connect(RuntimeContainer container) {
    	SnowflakeNativeConnection nativeConn = new SnowflakeNativeConnection();
    	Connection conn = null;
		String queryString = "";
		
		SnowflakeConnectionProperties connProps = properties.getConnectionProperties();
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
			return null;
		}
		
		nativeConn.setConnection(conn);

		if (container != null) {
            container.setComponentData(container.getCurrentComponentId(), KEY_CONNECTION, nativeConn);
        }

		return nativeConn;
    }

    
	/* (non-Javadoc)
	 * @see org.talend.components.api.component.runtime.SourceOrSink#getSchemaNames(org.talend.components.api.container.RuntimeContainer)
	 */
	@Override
	public List<NamedThing> getSchemaNames(RuntimeContainer container) throws IOException {
		SnowflakeConnectionProperties connProps = properties.getConnectionProperties();
		String catalog = connProps.db.getStringValue();
		return getSchemaNames(connect(container).getConnection(), catalog);
	}

	protected List<NamedThing> getSchemaNames(Connection connection, String catalog) throws IOException {

		//Returns the list with a db name found in the metadata
		List<NamedThing> returnList = new ArrayList<>();
		try {
			DatabaseMetaData metaData = connection.getMetaData();
			
			ResultSet resultIter =  metaData.getCatalogs();
			String catalogName = null;
			while (resultIter.next()) {
				catalogName = resultIter.getString(1);
				if (catalog.equalsIgnoreCase(catalogName)) { //Only add the user-selected db to the list
					returnList.add(new SimpleNamedThing(catalogName, catalogName));
					break;
				}
			}
		} catch (SQLException se) {
			//TODO: Handle this
		}
		return returnList;
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
	 * @see org.talend.components.api.component.runtime.SourceOrSink#getEndpointSchema(org.talend.components.api.container.RuntimeContainer, java.lang.String)
	 */
	@Override
	public Schema getEndpointSchema(RuntimeContainer container, String schemaName) throws IOException {
		return getSchema(connect(container).getConnection(), schemaName);
	}
	
    protected Schema getSchema(Connection connection, String schemaName) throws IOException {
        String catalog = properties.getConnectionProperties().db.getStringValue();
		try {
			DatabaseMetaData metaData = connection.getMetaData();
			
			//Fetch all tables in the db and schema provided
			ResultSet resultIter =  metaData.getTables(catalog, schemaName, null, null);
			String catalogName = null;
			while (resultIter.next()) {
				catalogName = resultIter.getString(1);
				if (catalog.equalsIgnoreCase(catalogName)) { //Only add the user-selected db to the list
					//returnList.add(new SimpleNamedThing(catalogName, catalogName));
					break;
				}
			}
		} catch (SQLException se) {
			//TODO: Handle this
		}
    	
    	
    	return null; //TODO:
    	
    	//TODO: implement once the registry class is ready
    	/*try {

            return SnowflakeAvroRegistry.get().inferSchema(describeSObjectResults[0]);
        } catch (SQLException e) {
            throw new IOException(e);
        }*/
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
