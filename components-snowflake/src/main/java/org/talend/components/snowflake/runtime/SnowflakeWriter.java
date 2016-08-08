package org.talend.components.snowflake.runtime;

import static org.talend.components.snowflake.SnowflakeOutputProperties.OutputAction.UPDATE;
import static org.talend.components.snowflake.SnowflakeOutputProperties.OutputAction.UPSERT;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.talend.components.api.component.runtime.Result;
import org.talend.components.api.component.runtime.WriteOperation;
import org.talend.components.api.component.runtime.WriterWithFeedback;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.api.exception.ComponentException;
import org.talend.components.snowflake.SnowflakeOutputProperties.OutputAction;
import org.talend.components.snowflake.connection.SnowflakeNativeConnection;
import org.talend.components.snowflake.tsnowflakeoutput.TSnowflakeOutputProperties;
import org.talend.daikon.avro.AvroUtils;
import org.talend.daikon.avro.SchemaConstants;
import org.talend.daikon.avro.converter.IndexedRecordConverter;
import org.talend.daikon.exception.ExceptionContext;
import org.talend.daikon.exception.error.DefaultErrorCode;

final class SnowflakeWriter implements WriterWithFeedback<Result, IndexedRecord, IndexedRecord> {

    private final SnowflakeWriteOperation snowflakeWriteOperation;

    private SnowflakeNativeConnection connection;

    private String uId;

    private final SnowflakeSink sink;

    private final RuntimeContainer container;

    private final TSnowflakeOutputProperties sprops;

    private String upsertKeyColumn;

    protected final List<IndexedRecord> deleteItems;
    private String deleteSQL = null;
    private PreparedStatement deletePS = null;

    protected final List<IndexedRecord> insertItems;
    private String insertSQL = null;
    private PreparedStatement insertPS = null;

    protected final List<IndexedRecord> upsertItems;
    protected final List<IndexedRecord> insertAfterUpdateItems;

    protected final List<IndexedRecord> updateItems;
    private String updateSQL = null;
    private PreparedStatement updatePS = null;

    protected final int commitLevel;

    protected boolean exceptionForErrors;

    private int dataCount;

    private int successCount;

    private int rejectCount;

    private int deleteFieldId = -1;

    private transient IndexedRecordConverter<Object, ? extends IndexedRecord> factory;

    private transient Schema tableSchema;

    private transient Schema mainSchema;

    private final List<IndexedRecord> successfulWrites = new ArrayList<>();

    private final List<IndexedRecord> rejectedWrites = new ArrayList<>();

    private final List<String> nullValueFields = new ArrayList<>();
    
    private final String formatDate = "yyyy-MM-dd";
    private final String formatTime = "HH:mm:ss";
    private final String formatTimestamp = "yyyy-MM-dd HH:mm:ss.SSS";
    private Map<Integer, Integer> schemaToSQLPositionMap;
    

    public SnowflakeWriter(SnowflakeWriteOperation sfWriteOperation, RuntimeContainer container) {
        this.snowflakeWriteOperation = sfWriteOperation;
        this.container = container;
        sink = snowflakeWriteOperation.getSink();
        sprops = sink.getSnowflakeOutputProperties();
        if (sprops.extendInsert.getValue()) { //TODO: revisit extendInsert functionality
            commitLevel = sprops.commitLevel.getValue();
        } else {
            commitLevel = 1;
        }
        int arraySize = commitLevel * 2;
        deleteItems = new ArrayList<>(arraySize);
        insertItems = new ArrayList<>(arraySize);
        updateItems = new ArrayList<>(arraySize);
        upsertItems = new ArrayList<>(arraySize);
        insertAfterUpdateItems = new ArrayList<>(arraySize);
        upsertKeyColumn = "";
        exceptionForErrors = sprops.ceaseForError.getValue();
    }

    @Override
    public void open(String uId) throws IOException {
        this.uId = uId;
        connection = sink.connect(container);
        if (null == mainSchema) {
            mainSchema = sprops.table.main.schema.getValue();
            tableSchema = sink.getSchema(connection.getConnection(), sprops.table.tableName.getStringValue());
            if (AvroUtils.isIncludeAllFields(mainSchema)) {
                mainSchema = tableSchema;
            } // else schema is fully specified
        }
        upsertKeyColumn = sprops.upsertKeyColumn.getStringValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(Object datum) throws IOException {
        dataCount++;
        // Ignore empty rows.
        if (null == datum) {
            return;
        }

        // This is all we need to do in order to ensure that we can process the incoming value as an IndexedRecord.
        if (null == factory) {
            factory = (IndexedRecordConverter<Object, ? extends IndexedRecord>) SnowflakeAvroRegistry.get()
                    .createIndexedRecordConverter(datum.getClass());
        }
        IndexedRecord input = factory.convertToAvro(datum);

        switch (sprops.outputAction.getValue()) {
        case INSERT:
            insert(input);
            break;
        case UPDATE:
            update(input);
            break;
        case UPSERT:
            upsert(input);
            break;
        case DELETE:
            delete(input);
        }
    }

    private int[] insert(IndexedRecord input) throws IOException {
        insertItems.add(input);

    	if (null == insertSQL || insertSQL.equalsIgnoreCase("")) {
    		insertSQL = buildInsertSQL(input);
    	}
    	if (null == insertPS) {
            try {
            	Connection conn = connection.getConnection();
            	insertPS = conn.prepareStatement(insertSQL);
            } catch (Exception e) { //TODO: catch the correct exception
                throw new IOException(e);
            }    		
    	} 	
        
        if (insertItems.size() >= commitLevel) {
        	return doInsert();
        }
        return null;
    }
    
    private String buildInsertSQL(IndexedRecord input) {
    	
    	Schema metaData = input.getSchema(); //Assumption: this will not violate the target schema
    	
    	String tableName = metaData.getName();
    	List<Field> columns = metaData.getFields();
    	int colSize = columns.size();
    	Schema columnMataData;
    	
    	insertSQL = "INSERT INTO " + tableName + "(";
    	
    	//List the column names
    	for (Field f : columns) {
    		columnMataData = f.schema();
    		insertSQL += columnMataData.getName() + ", ";
    		//columnMataData.getType();
    	}
    	insertSQL = insertSQL.substring(0, insertSQL.lastIndexOf(", ")) + ") VALUES(";
    	
    	//List the place-holders (for the data to be set in PreparedStatement)
    	for(int i = 0; i < colSize; i++) {
    		insertSQL += "?, ";
    	}
    	insertSQL = insertSQL.substring(0, insertSQL.lastIndexOf(", ")) + ")";
    	
    	return insertSQL;
    }
    
    private void setDataToNativeSink(PreparedStatement ps, List<IndexedRecord> inputs, OutputAction action) throws SQLException{
    	
    	for (IndexedRecord record: inputs) {
    		// Identify the columns and set the data into the preparedstatement
    		
        	Schema metaData = record.getSchema();
        	
        	List<Field> columns = metaData.getFields();
        	int colSize = columns.size();
        	Schema columnMetaData;
		    
    		Schema.Type fieldType;
    		
    		String formatDateTime = null;
    		
    		int sqlFPos = 1;

    		for (Field f : columns) {
    			
    			  switch (action) {
    		        case INSERT:
    		        	sqlFPos = f.pos();
    		        	break;
    		        case UPDATE:
    		        	sqlFPos = schemaToSQLPositionMap.get(f.pos());
    		        	break;
    		        case DELETE:
    		      		columnMetaData = f.schema();
    		    		Object isKeyProp = columnMetaData.getObjectProp(SchemaConstants.TALEND_COLUMN_IS_KEY);
    		    		
    		    		if (null != isKeyProp && ((Boolean)isKeyProp).booleanValue() == true) {
    		    			sqlFPos = schemaToSQLPositionMap.get(f.pos());
    		    		} else {
    		    			continue; //check the next field
    		    		}
    		        	break;
    		        
    		        default:
    		        	continue; //Ideally, should not reach here
    		        	
    		      }
    			
    			fieldType = f.schema().getType();
    			
    			//boolean fieldNullable = f.schema(). TODO
    			
    			Object prec = f.schema().getObjectProp(SchemaConstants.TALEND_COLUMN_PRECISION);
    			formatDateTime = f.schema().getProp(SchemaConstants.TALEND_COLUMN_PATTERN);
    			
    			Object value = record.get(f.pos()); //data for the field
    			
    			switch(fieldType) {
    			
    			case STRING:
    				if (null != prec) {
    					//NUMBER
    					if (null != value) {
    						ps.setInt(sqlFPos, (Integer)value);
    					} else {
    						ps.setNull(sqlFPos, Types.INTEGER);
    					}
    				} else if (false) {
    					//TODO: handle OBJECT, ARRAY & VARIANT
    				} 
    				else {
    					if (null != value) {
    						ps.setString(sqlFPos, (String)value);
    					} else {
    						ps.setString(sqlFPos, null);
    					}
    				}
    				break;
    				
    			case INT:
    				if (null != prec) {
    					if (null != value) {
    						ps.setInt(sqlFPos, (Integer)value);
    					} else {
    						ps.setNull(sqlFPos, Types.INTEGER);
    					}
    					
    				} else {
    					//TODO: check other types
    				}
    				break;
    				
    			case DOUBLE:
					if (null != value) {
						ps.setDouble(sqlFPos, (Double)value);
					} else {
						ps.setNull(sqlFPos, Types.DOUBLE);
					}
    				break;
    				
    			case FLOAT:
					if (null != value) {
						ps.setFloat(sqlFPos, (Float)value);
					} else {
						ps.setNull(sqlFPos, Types.FLOAT);
					}
    				break;
    				
    			case LONG:
					if (null != formatDateTime) {
						switch(formatDateTime) {
						case formatDate:
							if (null != value) {
								ps.setDate(sqlFPos, new java.sql.Date((Long)value));
							} else {
								ps.setNull(sqlFPos, Types.DATE);
							}
							break;
							
						case formatTime:
							if (null != value) {
								ps.setTime(sqlFPos, new java.sql.Time((Long)value));
							} else {
								ps.setNull(sqlFPos, Types.TIME);
							}
							break;
							
						case formatTimestamp:
							if (null != value) {
								ps.setTimestamp(sqlFPos, new java.sql.Timestamp((Long)value));
							} else {
								ps.setNull(sqlFPos, Types.TIMESTAMP);
							}
							break;
						}
						
					} else if (null != value) {
						ps.setLong(sqlFPos, (Long)value);
					} else {
						ps.setNull(sqlFPos, Types.BIGINT);
					}
    				break;

    			case BOOLEAN:
					if (null != value) {
						ps.setBoolean(sqlFPos, (Boolean)value);
					} else {
						ps.setNull(sqlFPos, Types.BOOLEAN);
					}
    				break;
    			}
    		}//end fields
    	}//end records
    	
    	ps.addBatch();
    }

    private int[] doInsert() throws IOException {
        if (insertItems.size() > 0) {
            // Clean the feedback records at each batch write.
            cleanFeedbackRecords();
            
            int[] saveResults = {};
            try {
            	setDataToNativeSink(insertPS, insertItems, OutputAction.INSERT);
            	//insertPS.addBatch();
            	saveResults = insertPS.executeBatch();
            	insertPS.clearBatch();
            	
            	//TODO: code: check result, handle success and failure accordingly
            	insertItems.clear();
                return saveResults;
            } catch (Exception e) { //TODO: catch the correct exception
                throw new IOException(e);
            }
        }
        return null;
    }

    private int[] doInsertForUpsert() throws IOException {
        if (insertAfterUpdateItems.size() > 0) {
            // Clean the feedback records at each batch write.
            cleanFeedbackRecords();
            
            int[] saveResults = {};
            try {
            	setDataToNativeSink(insertPS, insertAfterUpdateItems, OutputAction.INSERT);
            	//insertPS.addBatch();
            	saveResults = insertPS.executeBatch();
            	insertPS.clearBatch();
            	
            	//TODO: code: check result, handle success and failure accordingly
            	//insertAfterUpdateItems.clear();
                return saveResults;
            } catch (Exception e) { //TODO: catch the correct exception
                throw new IOException(e);
            }
        }
        return null;
    }

    private int[] update(IndexedRecord input) throws IOException {
        updateItems.add(input);

    	if (null == updateSQL || updateSQL.equalsIgnoreCase("")) {
    		updateSQL = buildUpdateSQL(input);
    	}
    	if (null == updatePS) {
            try {
            	Connection conn = connection.getConnection();
            	updatePS = conn.prepareStatement(updateSQL);
            } catch (Exception e) { //TODO: catch the correct exception
                throw new IOException(e);
            }    		
    	} 	
        
        if (updateItems.size() >= commitLevel) {
        	return doUpdate();
        }
        return null;
    }
    
    private String buildUpdateSQL(IndexedRecord input) throws IOException{
    	Schema metaData = input.getSchema(); //Assumption: this will not violate the target schema
    	
    	String tableName = metaData.getName();
    	List<Field> columns = metaData.getFields();
    	int colSize = columns.size();
    	Schema columnMetaData;
    	List<Field> pkFields = new ArrayList<>();
    	schemaToSQLPositionMap = new HashMap<>();
    	int sqlPos = 0;
    	
    	updateSQL = "UPDATE TABLE " + tableName + " SET ";
    	
    	//List the column names
    	for (Field f : columns) {
    		columnMetaData = f.schema();
    		Object isKeyProp = columnMetaData.getObjectProp(SchemaConstants.TALEND_COLUMN_IS_KEY);
    		
    		if (null != isKeyProp && ((Boolean)isKeyProp).booleanValue() == true) {
    			pkFields.add(f);
    		} else {
    			sqlPos++;
    			schemaToSQLPositionMap.put(f.pos(), sqlPos);
    			updateSQL += columnMetaData.getName() + "=?, ";
    		}
    		
    	}
    	updateSQL = updateSQL.substring(0, updateSQL.lastIndexOf(", ")) + " WHERE ";
    	
    	if (pkFields.isEmpty()) {
    		throw new IOException("TABLE "+ tableName + " CANNOT BE UPDATED WITHOUT PRIMARY KEY(S)");
    	}
    	for(Field f: pkFields) {
    		columnMetaData = f.schema();
    		
			sqlPos++; //running position
			schemaToSQLPositionMap.put(f.pos(), sqlPos);
    		
    		updateSQL += columnMetaData.getName() + "=? AND ";
    	}
    	updateSQL = updateSQL.substring(0, updateSQL.lastIndexOf("AND ")) + ")";
    	
    	return updateSQL;
    }
    
    private int[] doUpdate() throws IOException {
        if (updateItems.size() > 0) {
            // Clean the feedback records at each batch write.
            cleanFeedbackRecords();
            
            int[] saveResults = {};
            try {
            	setDataToNativeSink(updatePS, updateItems, OutputAction.UPDATE);
            	//updatePS.addBatch();
            	saveResults = updatePS.executeBatch();
            	updatePS.clearBatch();
            	
            	//TODO: code: check result, handle success and failure accordingly
            	updateItems.clear();
                return saveResults;
            } catch (Exception e) { //TODO: catch the correct exception
                throw new IOException(e);
            }
        }
        return null;
    }
    
    private int[] doUpdateForUpsert() throws IOException {
        if (upsertItems.size() > 0) {
            // Clean the feedback records at each batch write.
            cleanFeedbackRecords();
            
            int[] saveResults = {};
            try {
            	setDataToNativeSink(updatePS, upsertItems, OutputAction.UPDATE);
            	//updatePS.addBatch();
            	saveResults = updatePS.executeBatch();
            	updatePS.clearBatch();
            	
            	//TODO: code: check result, handle success and failure accordingly
            	//upsertItems.clear(); //Do not clear now, need to check the update result for attempting insert
                return saveResults;
            } catch (Exception e) { //TODO: catch the correct exception
                throw new IOException(e);
            }
        }
        return null;
    }

    
    private int[] upsert(IndexedRecord input) throws IOException {
        
    	upsertItems.add(input);

        
        // Update 
    	if (null == updateSQL || updateSQL.equalsIgnoreCase("")) {
    		updateSQL = buildUpdateSQL(input);
    	}
    	if (null == updatePS) {
            try {
            	Connection conn = connection.getConnection();
            	updatePS = conn.prepareStatement(updateSQL);
            } catch (Exception e) { //TODO: catch the correct exception
                throw new IOException(e);
            }    		
    	} 	
        // Insert
    	if (null == insertSQL || insertSQL.equalsIgnoreCase("")) {
    		insertSQL = buildInsertSQL(input);
    	}
    	if (null == insertPS) {
            try {
            	Connection conn = connection.getConnection();
            	insertPS = conn.prepareStatement(insertSQL);
            } catch (Exception e) { //TODO: catch the correct exception
                throw new IOException(e);
            }    		
    	} 	
        
        if (upsertItems.size() >= commitLevel) {
        	return doUpsert();
        }  
        
        return null;
    }

    private int[] doUpsert() throws IOException {

    	int[] upd = {};
        int[] upsertResult = {};
    	
    	if (upsertItems.size() > 0) {
            // Clean the feedback records at each batch write.
            cleanFeedbackRecords();

    		//1. Try updating
    		upd = doUpdateForUpsert();
        
    		for (int i = 0; i< upd.length; i++) {
    			if (upd[i] == 0) {
    				insertAfterUpdateItems.add(upsertItems.get(i));
    			}
    		}
    		
    		//2. Insert the failed records
    		if (insertAfterUpdateItems.size() > 0) {
    			upsertResult = doInsertForUpsert();
    		}
    		
    		upsertItems.clear();
    		insertAfterUpdateItems.clear();
    		
        }
        
    	return upsertResult;

    }

    private void handleSuccess(IndexedRecord input, String id) {
        successCount++;
        Schema outSchema = sprops.schemaFlow.schema.getValue();
        if (outSchema == null || outSchema.getFields().size() == 0)
            return;
        if (input.getSchema().equals(outSchema)) {
            successfulWrites.add(input);
        } else {
            IndexedRecord successful = new GenericData.Record(outSchema);
            for (Schema.Field outField : successful.getSchema().getFields()) {
                Object outValue = null;
                Schema.Field inField = input.getSchema().getField(outField.name());
                if (inField != null) {
                    outValue = input.get(inField.pos());
                } else if (TSnowflakeOutputProperties.FIELD_SNOWFLAKE_ID.equals(outField.name())) {
                    outValue = id;
                }
                successful.put(outField.pos(), outValue);
            }
            successfulWrites.add(successful);
        }
    }

    private void handleReject(IndexedRecord input, Error[] resultErrors, String[] changedItemKeys, int batchIdx)
            throws IOException {
    	//TODO: implement
    }

    private int[] delete(IndexedRecord input) throws IOException {
        deleteItems.add(input);

    	if (null == deleteSQL || deleteSQL.equalsIgnoreCase("")) {
    		deleteSQL = buildDeleteSQL(input);
    	}
    	if (null == deletePS) {
            try {
            	Connection conn = connection.getConnection();
            	deletePS = conn.prepareStatement(deleteSQL);
            } catch (Exception e) { //TODO: catch the correct exception
                throw new IOException(e);
            }    		
    	} 	
        
        if (deleteItems.size() >= commitLevel) {
        	return doDelete();
        }
        return null;
    }
    
    private String buildDeleteSQL(IndexedRecord input) throws IOException{
    	Schema metaData = input.getSchema(); //Assumption: this will not violate the target schema
    	
    	String tableName = metaData.getName();
    	List<Field> columns = metaData.getFields();
    	Schema columnMetaData;
    	List<Field> pkFields = new ArrayList<>();
    	schemaToSQLPositionMap = new HashMap<>();
    	int sqlPos = 0;
    	
    	deleteSQL = "DELETE FROM " + tableName + " WHERE ";
    	
    	//List the column names
    	for (Field f : columns) {
    		columnMetaData = f.schema();
    		Object isKeyProp = columnMetaData.getObjectProp(SchemaConstants.TALEND_COLUMN_IS_KEY);
    		
    		if (null != isKeyProp && ((Boolean)isKeyProp).booleanValue() == true) {
    			pkFields.add(f);
    		}
    	}
    	
    	if (pkFields.isEmpty()) {
    		throw new IOException("DELETE CANNOT BE DONE ON TABLE "+ tableName + " WITHOUT PRIMARY KEY(S)");
    	}
    	for(Field f: pkFields) {
    		columnMetaData = f.schema();
    		
			sqlPos++; //running position
			schemaToSQLPositionMap.put(f.pos(), sqlPos);
    		
			deleteSQL += columnMetaData.getName() + "=? AND ";
    	}
    	deleteSQL = deleteSQL.substring(0, deleteSQL.lastIndexOf("AND ")) + ")";
    	
    	return deleteSQL;
    }
    

    private int[] doDelete() throws IOException {
        if (deleteItems.size() > 0) {
            // Clean the feedback records at each batch write.
            cleanFeedbackRecords();
            
            int[] saveResults = {};
            try {
            	setDataToNativeSink(deletePS, deleteItems, OutputAction.DELETE);
            	//deletePS.addBatch();
            	saveResults = deletePS.executeBatch();
            	deletePS.clearBatch();
            	
            	//TODO: code: check result, handle success and failure accordingly
            	deleteItems.clear();
                return saveResults;
            } catch (Exception e) { //TODO: catch the correct exception
                throw new IOException(e);
            }
        }
        return null;
    }

    @Override
    public Result close() throws IOException {
        logout();
        return new Result(uId, dataCount, successCount, rejectCount);
    }

    private void logout() throws IOException {
        // Finish anything uncommitted
        
        switch (sprops.outputAction.getValue()) {
        case INSERT:
        	doInsert();
            if (null != insertPS) {
            	try {
    				insertPS.close();
    			} catch (SQLException e) {
    				e.printStackTrace();
    			}
            }
            insertSQL = null;
            break;
            
        case UPDATE:
            doUpdate();
            if (null != updatePS) {
            	try {
    				updatePS.close();
    			} catch (SQLException e) {
    				e.printStackTrace();
    			}
            }
            updateSQL = null;
            break;

        case UPSERT:
            doUpsert();
            if (null != updatePS) {
            	try {
    				updatePS.close();
    			} catch (SQLException e) {
    				e.printStackTrace();
    			}
            }
            if (null != insertPS) {
            	try {
    				insertPS.close();
    			} catch (SQLException e) {
    				e.printStackTrace();
    			}
            }
            updateSQL = null;
            insertSQL = null;
            break;

        case DELETE:
            doDelete();
            if (null != deletePS) {
            	try {
    				deletePS.close();
    			} catch (SQLException e) {
    				e.printStackTrace();
    			}
            }
            deleteSQL = null;
            break;
        }

        Connection conn = connection.getConnection();
        try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
        
    }

    @Override
    public WriteOperation<Result> getWriteOperation() {
        return snowflakeWriteOperation;
    }

    @Override
    public List<IndexedRecord> getSuccessfulWrites() {
        return Collections.unmodifiableList(successfulWrites);
    }

    @Override
    public List<IndexedRecord> getRejectedWrites() {
        return Collections.unmodifiableList(rejectedWrites);
    }

    private void cleanFeedbackRecords(){
        successfulWrites.clear();
        rejectedWrites.clear();
    }
}
