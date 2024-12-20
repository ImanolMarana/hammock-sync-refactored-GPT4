/*
 * Copyright © 2017 IBM Corp. All rights reserved.
 *
 * Copyright © 2015 Cloudant, Inc. All rights reserved.
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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.hammock.sync.documentstore.ConflictException;
import org.hammock.sync.documentstore.DocumentBodyFactory;
import org.hammock.sync.documentstore.DocumentRevision;
import org.hammock.sync.internal.documentstore.ProjectedDocumentRevision;
import org.hammock.sync.query.QueryException;
import org.hammock.sync.query.QueryResult;
import org.hammock.sync.util.SQLDatabaseTestUtils;
import org.hammock.sync.util.TestUtils;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class QueryFilterFieldsTest extends AbstractQueryTestBase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        indexManagerDatabaseQueue = TestUtils.getDBQueue(im);
        assertThat(im, is(notNullValue()));
        assertThat(indexManagerDatabaseQueue, is(notNullValue()));
        String[] metadataTableList = new String[] { QueryConstants.INDEX_METADATA_TABLE_NAME };
        SQLDatabaseTestUtils.assertTablesExist(indexManagerDatabaseQueue, metadataTableList);

        setUpBasicQueryData();
    }

    // When filtering fields on find

    @Test
    public void returnsFieldSpecifiedOnly() throws QueryException {
        // query - { "name" : "mike" }
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("name", "mike");
        QueryResult queryResult = im.find(query, 0, Long.MAX_VALUE, Arrays.asList("name"), null);
        for (DocumentRevision rev : queryResult) {
            Map<String, Object> revBody = rev.getBody().asMap();
            assertThat(revBody.keySet(), contains("name"));
        }
    }

    @Test
    public void returnsAllFieldsWhenFieldsArrayEmpty() throws QueryException {
        // query - { "name" : "mike" }
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("name", "mike");
        QueryResult queryResult = im.find(query, 0, Long.MAX_VALUE, new ArrayList<String>(), null);
        for (DocumentRevision rev : queryResult) {
            Map<String, Object> revBody = rev.getBody().asMap();
            assertThat(revBody.keySet(), containsInAnyOrder("name", "pet", "age"));
        }
    }

    @Test
    public void returnsAllFieldsWhenFieldsArrayNull() throws QueryException {
        // query - { "name" : "mike" }
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("name", "mike");
        QueryResult queryResult = im.find(query, 0, Long.MAX_VALUE, null, null);
        for (DocumentRevision rev : queryResult) {
            Map<String, Object> revBody = rev.getBody().asMap();
            assertThat(revBody.keySet(), containsInAnyOrder("name", "pet", "age"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void returnsNullWhenUsingDottedNotation() throws QueryException {
        // query - { "name" : "mike" }
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("name", "mike");
        im.find(query,
                                          0,
                                          Long.MAX_VALUE,
                                          Arrays.asList("name.blah"),
                                          null);
    }

    @Test
    public void returnsOnlyFieldsSpecified() throws QueryException {
        // query - { "name" : "mike" }
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("name", "mike");
        QueryResult queryResult = im.find(query,
                                          0,
                                          Long.MAX_VALUE,
                                          Arrays.asList("name", "pet"),
                                          null);
        for (DocumentRevision rev : queryResult) {
            Map<String, Object> revBody = rev.getBody().asMap();
            assertThat(revBody.keySet(), containsInAnyOrder("name", "pet"));
        }
    }

    @Test
    public void returnsFullMutableCopyOfProjectedDoc() throws Exception {
        // query - { "name" : "mike", "age" : 12 }
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("name", "mike");
        query.put("age", 12);
        QueryResult queryResult = im.find(query, 0, Long.MAX_VALUE, Arrays.asList("name"), null);
        assertThat(queryResult.size(), is(1));
        for (DocumentRevision rev : queryResult) {
            Map<String, Object> revBody = rev.getBody().asMap();
            assertThat(revBody.keySet(), contains("name"));
            assertThat((String) revBody.get("name"), is("mike"));

            assertThat(rev, instanceOf(ProjectedDocumentRevision.class));
            DocumentRevision copy = rev.toFullRevision();
            Map<String, Object> bodyCopy = copy.getBody().asMap();
            assertThat(bodyCopy.keySet(), containsInAnyOrder("name", "age", "pet"));
            assertThat((String) bodyCopy.get("name"), is("mike"));
            assertThat((Integer) bodyCopy.get("age"), is(12));
            assertThat((String) bodyCopy.get("pet"), is("cat"));
        }
    }

    @Test
    public void returnsNullMutableCopyWhenDocUpdated() throws Exception {
        // query - { "name" : "mike", "age" : 12 }
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("name", "mike");
        query.put("age", 12);
        QueryResult queryResult = im.find(query, 0, Long.MAX_VALUE, Arrays.asList("name"), null);
        assertThat(queryResult.size(), is(1));
        for (DocumentRevision rev : queryResult) {
            assertThat(rev, is(instanceOf(ProjectedDocumentRevision.class)));
            Map<String, Object> revBody = rev.getBody().asMap();
            assertThat(revBody.keySet(), contains("name"));
            assertThat((String) revBody.get("name"), is("mike"));

            DocumentRevision original = ds.read(rev.getId());
            DocumentRevision update = original;
            Map<String, Object> updateBody = original.getBody().asMap();
            updateBody.put("name", "charles");
            update.setBody(DocumentBodyFactory.create(updateBody));
            assertThat(ds.update(update), is(notNullValue()));
            assertThat(rev.isFullRevision(),is(false));
            DocumentRevision fullRevision = rev.toFullRevision();
            assertThat(fullRevision.isFullRevision(),is(true));
            assertThat(fullRevision.getRevision(),is(equalTo(rev.getRevision())));
            assertThat(fullRevision.getId(),is(equalTo(rev.getId())));
        }
    }

    @Test
    public void returnsNullMutableCopyWhenDocDeleted() throws Exception {
        // query - { "name" : "mike", "age" : 12 }
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("name", "mike");
        query.put("age", 12);
        QueryResult queryResult = im.find(query, 0, Long.MAX_VALUE, Arrays.asList("name"), null);
        assertThat(queryResult.size(), is(1));
        for (DocumentRevision rev : queryResult) {
            assertThat(rev, is(instanceOf(ProjectedDocumentRevision.class)));
            Map<String, Object> revBody = rev.getBody().asMap();
            assertThat(revBody.keySet(), contains("name"));
            assertThat((String) revBody.get("name"), is("mike"));

            try {
                DocumentRevision deleted;
                deleted = ds.delete((ProjectedDocumentRevision) rev);
                assertThat(deleted, is(notNullValue()));
            } catch (ConflictException e) {
                Assert.fail("Failed to delete document revision");
                e.printStackTrace();
            }

            DocumentRevision fullRevision = rev.toFullRevision();
            assertThat(fullRevision.isFullRevision(),is(true));
            assertThat(fullRevision.getRevision(),is(equalTo(rev.getRevision())));
            assertThat(fullRevision.getId(), is(equalTo(rev.getId())));
        }
    }

    @Test
    public void projectedDocumentProhibitedFromSaving() throws Exception {
        // query - { "name" : "mike", "age" : 12 }
        Map<String, Object> query = new HashMap<String, Object>();
        query.put("name", "mike");
        query.put("age", 12);
        QueryResult queryResult = im.find(query, 0, Long.MAX_VALUE, Arrays.asList("name"), null);
        assertThat(queryResult.size(), is(1));
        for (DocumentRevision rev : queryResult) {
            assertThat(rev, is(instanceOf(ProjectedDocumentRevision.class)));
            try {
                ds.update(rev);
                Assert.fail("IllegalArgumentException not thrown");
            } catch(IllegalArgumentException iae) {
                ; // exception thrown - can't update from a projected revision
            }
        }
    }

}
