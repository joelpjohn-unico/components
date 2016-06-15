// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.components.jira.avro;

import static org.junit.Assert.assertEquals;

import org.apache.avro.Schema;
import org.junit.BeforeClass;
import org.junit.Test;
import org.talend.components.jira.testutils.Utils;

/**
 * Unit-tests for {@link IssueIndexedRecord} class
 */
public class IssueIndexedRecordTest {

    /**
     * {@link Schema} used as test argument
     */
    private static Schema testSchema;

    /**
     * Used as test argument
     */
    private static String testJson;

    /**
     * Initializes test arguments before tests
     */
    @BeforeClass
    public static void setUp() {
        String schemaJson = Utils.readFile("src/test/resources/org/talend/components/jira/tjirainput/schema.json");
        testSchema = new Schema.Parser().parse(schemaJson);
        testJson = Utils.readFile("src/test/resources/org/talend/components/jira/datum/noIssues.json");
    }

    /**
     * Checks {@link IssueIndexedRecord#getSchema()} returns schema without changes
     */
    @Test
    public void testGetSchema() {
        IssueIndexedRecord indexedRecord = new IssueIndexedRecord(testJson, testSchema);
        assertEquals(testSchema, indexedRecord.getSchema());
    }

    /**
     * Checks {@link IssueIndexedRecord#get()} returns json field value, when 0 is passed as index argument
     */
    @Test
    public void testGet() {
        IssueIndexedRecord indexedRecord = new IssueIndexedRecord(testJson, testSchema);
        assertEquals(testJson, indexedRecord.get(0));
    }

    /**
     * Checks {@link IssueIndexedRecord#get()} throws {@link IndexOutOfBoundsException}, when any other than 0 is passed
     * as index argument
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetIndexOutOfBoundException() {
        IssueIndexedRecord indexedRecord = new IssueIndexedRecord(testJson, testSchema);
        indexedRecord.get(3);
    }

    /**
     * Checks {@link IssueIndexedRecord#put()} put field value correctly when 0 is passed as index
     */
    @Test
    public void testPut() {
        IssueIndexedRecord indexedRecord = new IssueIndexedRecord(testJson, null);
        indexedRecord.put(0, testJson);
        assertEquals(testJson, indexedRecord.get(0));
    }

    /**
     * Checks {@link IssueIndexedRecord#put()} throws {@link IndexOutOfBoundsException}, when any other than 0 is passed
     * as index argument
     */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testPutIndexOutOfBoundException() {
        IssueIndexedRecord indexedRecord = new IssueIndexedRecord(testJson, null);
        indexedRecord.put(3, testJson);
    }
}
