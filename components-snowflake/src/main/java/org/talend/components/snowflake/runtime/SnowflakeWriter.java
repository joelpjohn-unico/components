package org.talend.components.snowflake.runtime;

import static org.talend.components.snowflake.SnowflakeOutputProperties.OutputAction.UPDATE;
import static org.talend.components.snowflake.SnowflakeOutputProperties.OutputAction.UPSERT;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.IndexedRecord;
import org.talend.components.api.component.runtime.Result;
import org.talend.components.api.component.runtime.WriteOperation;
import org.talend.components.api.component.runtime.WriterWithFeedback;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.api.exception.ComponentException;
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
    private String deleteSQL = "";

    protected final List<IndexedRecord> insertItems;
    private String insertSQL = "";

    protected final List<IndexedRecord> upsertItems;

    protected final List<IndexedRecord> updateItems;
    private String updateSQL = "";

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
        
        if (insertItems.size() >= commitLevel) {
        	if (null == insertSQL || insertSQL.equalsIgnoreCase("")) {
        		insertSQL = buildInsertSQL(input);
        	}
        	return doInsert();
        }
        return null;
    }
    
    private String buildInsertSQL(IndexedRecord input) {
    	return "";
    	//TODO: parse the input data and construct SQL Query
    }
    
    private void setInsertData(PreparedStatement ps, List<IndexedRecord> inputs) throws SQLException{
    	for (IndexedRecord record: inputs) {
    		//TODO: Identify the columns and set the data into the preparedstatement
    		ps.addBatch();
    	}
    }

    private int[] doInsert() throws IOException {
        if (insertItems.size() > 0) {
            // Clean the feedback records at each batch write.
            cleanFeedbackRecords();
            
            int[] saveResults = {};
            try {
            	Connection conn = connection.getConnection();
            	PreparedStatement ps = conn.prepareStatement(insertSQL);
            	setInsertData(ps, insertItems);
            	saveResults = ps.executeBatch();
            	
            	//TODO: code: check result, handle success and failure accordingly
            	insertItems.clear();
                return saveResults;
            } catch (Exception e) { //TODO: catch the correct exception
                throw new IOException(e);
            }
        }
        return null;
    }

    private int[] update(IndexedRecord input) throws IOException {
        updateItems.add(input);
        if (updateItems.size() >= commitLevel) {
        	if (null == insertSQL || insertSQL.equalsIgnoreCase("")) {
        		updateSQL = buildUpdateSQL(input);
        	}
        	
            return doUpdate();
        }
        return null;
    }
    
    private String buildUpdateSQL(IndexedRecord input) {
    	return "";
    	//TODO: parse the input data and construct SQL Query
    }
    
    private void setUpdateData(PreparedStatement ps, List<IndexedRecord> inputs) throws SQLException{
    	for (IndexedRecord record: inputs) {
    		//TODO: Identify the columns and set the data into the preparedstatement
    		ps.addBatch();
    	}
    }

    private int[] doUpdate() throws IOException {
        if (updateItems.size() > 0) {
            // Clean the feedback records at each batch write.
            cleanFeedbackRecords();
            
            int[] saveResults = {};
            try {
            	Connection conn = connection.getConnection();
            	PreparedStatement ps = conn.prepareStatement(updateSQL);
            	setUpdateData(ps, insertItems);
            	saveResults = ps.executeBatch();

            	//TODO:check result and handle success and failure accordingly 
            	
            	updateItems.clear();
                return saveResults;
            } catch (Exception e) { //TODO: catch the proper exception
                throw new IOException(e);
            }
        }
        return null;
    }

    private int[] upsert(IndexedRecord input) throws IOException {
        upsertItems.add(input);
        if (upsertItems.size() >= commitLevel) {
            return doUpsert();
        }
        return null;
    }

    private int[] doUpsert() throws IOException {
        if (upsertItems.size() > 0) {
            // Clean the feedback records at each batch write.
            cleanFeedbackRecords();
            //TODO: code: prepare for upsert; check values exist for key columns etc
            int[] upsertResults = {};
            try {
            	//TODO: code: execute update/insert logic
            	//handle success and failure accordingly
            	upsertItems.clear();
                return upsertResults;
            } catch (Exception e) { //TODO: catch the correct exception
                throw new IOException(e);
            }
        }
        return null;

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
        // Calculate the field position of the Id the first time that it is used. The Id field must be present in the
        // schema to delete rows.
        if (deleteFieldId == -1) {
            String ID = "Id";
            Schema.Field idField = input.getSchema().getField(ID);
            if (null == idField) {
                //TODO: throw the appropriate exception
            	/*throw new ComponentException(new DefaultErrorCode(HttpServletResponse.SC_BAD_REQUEST, "message"),
                        ExceptionContext.build().put("message", ID + " not found"));*/
            }
            deleteFieldId = idField.pos();
        }
        String id = (String) input.get(deleteFieldId);
        if (id != null) {
            deleteItems.add(input);
            if (deleteItems.size() >= commitLevel) {
                return doDelete();
            }
        }
        return null;
    }

    private int[] doDelete() throws IOException {
        if (deleteItems.size() > 0) {
            // Clean the feedback records at each batch write.
            cleanFeedbackRecords();
            String[] delIDs = new String[deleteItems.size()];
            String[] changedItemKeys = new String[delIDs.length];
            for (int ix = 0; ix < delIDs.length; ++ix) {
                delIDs[ix] = (String) deleteItems.get(ix).get(deleteFieldId);
                changedItemKeys[ix] = delIDs[ix];
            }
            int[] dr = {};
            try {
            	//TODO: code: execute delete query; check the result and handle success andf failure accordingly
            	deleteItems.clear();
                return dr;
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
        doInsert();
        doDelete();
        doUpdate();
        doUpsert();
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
