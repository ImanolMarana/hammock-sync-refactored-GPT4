/*
 * Copyright © 2016 IBM Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package org.hammock.sync.internal.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import org.hammock.sync.documentstore.encryption.NullKeyProvider;
import org.hammock.sync.query.FieldSort;
import org.hammock.sync.query.QueryException;
import org.hammock.sync.query.QueryResult;
import org.hammock.sync.internal.sqlite.SQLDatabase;
import org.hammock.sync.internal.sqlite.SQLDatabaseQueue;
import org.hammock.sync.internal.sqlite.SQLCallable;
import org.hammock.sync.util.SQLDatabaseTestUtils;
import org.hammock.sync.util.TestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

public class QueryResultTest extends AbstractQueryTestBase {

    SQLDatabaseQueue queue;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        indexManagerDatabaseQueue = TestUtils.getDBQueue(im);
        assertThat(im, is(notNullValue()));
        assertThat(indexManagerDatabaseQueue, is(notNullValue()));
        String[] metadataTableList = new String[]{QueryConstants.INDEX_METADATA_TABLE_NAME};
        SQLDatabaseTestUtils.assertTablesExist(indexManagerDatabaseQueue, metadataTableList);

        queue = new SQLDatabaseQueue(new File(factoryPath, "/db.sync"), new NullKeyProvider());

        setUpBasicQueryData();
    }

    @After
    public void shutdownQueue() throws Exception {
        queue.shutdown();
    }


    /*
     * Perform a simple query then drop the revs table from the database before attempting
     * to get the document IDs from the QueryResult.
     */
    @Test(expected = NoSuchElementException.class)
    public void testQueryGetDocumentsWithIdsFails() throws InterruptedException,
        ExecutionException, QueryException {
        List<FieldSort> fields = Collections.<FieldSort>singletonList(new FieldSort("pet"));
        assertThat(im.createTextIndex(fields, "basic_text", null).indexName, is("basic_text"));

        // query - { "$text" : { "$search" : "cat" } }
        Map<String, Object> search = new HashMap<String, Object>();
        search.put("$search", "cat");
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("$text", search);
        QueryResult queryResult = im.find(query);

        queue.submit(new SQLCallable<Void>() {
            @Override
            public Void call(SQLDatabase db) throws Exception {
                db.execSQL("DROP TABLE IF EXISTS revs");
                return null;
            }
        }).get();

        // Attempt to retrieve the document IDs. This should fail with an
        // NoSuchElementException because the revs table has been dropped.
        queryResult.documentIds();
    }
}
