/*
 * Copyright © 2017 IBM Corp. All rights reserved.
 *
 * Copyright © 2014 Cloudant, Inc. All rights reserved.
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

package org.hammock.sync.query;

import org.hammock.sync.documentstore.Database;
import org.hammock.sync.documentstore.DocumentBodyFactory;
import org.hammock.sync.documentstore.DocumentRevision;
import org.hammock.sync.documentstore.DocumentStoreException;
import org.hammock.sync.internal.documentstore.DocumentRevisionBuilder;
import org.hammock.sync.internal.query.UnindexedMatcher;
import org.hammock.sync.internal.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 *  Iterable result of a query executed with {@link Query}.
 *
 *  @see Query
 *
 */
public class QueryResult implements Iterable<DocumentRevision> {

    private final static int DEFAULT_BATCH_SIZE = 50;

    private final List<String> originalDocIds;
    private final Database database;
    private final List<String> fields;
    private final long skip;
    private final long limit;
    private final UnindexedMatcher matcher;

    public QueryResult(List<String> originalDocIds,
                       Database database,
                       List<String> fields,
                       long skip,
                       long limit,
                       UnindexedMatcher matcher) {
        this.originalDocIds = originalDocIds;
        this.database = database;
        this.fields = fields;
        this.skip = skip;
        this.limit = limit;
        this.matcher = matcher;
    }

    /**
     *  Returns the number of documents in this query result.
     *
     *  @return the number of documents {@code DocumentRevision} in this query result.
     */
    public int size() {
        return documentIds().size();
    }

    /**
     *  Returns a list of the document IDs in this query result.
     *
     *  This method is implemented this way to ensure that the list of document IDs is
     *  consistent with the iterator results.
     *
     *  @return list of the document IDs
     */
    public List<String> documentIds() {
        List<String> documentIds = new ArrayList<String>();
        List<DocumentRevision> docs = CollectionUtils.newArrayList(iterator());
        for (DocumentRevision doc : docs) {
            documentIds.add(doc.getId());
        }
        return documentIds;
    }

    /**
     * @return a newly created Iterator over the query results
     */
    @Override
    public Iterator<DocumentRevision> iterator() {
        return new QueryResultIterator();
    }

    private class QueryResultIterator implements Iterator<DocumentRevision> {

        private Range range;
        private int nSkipped;
        private int nReturned;
        private boolean limitReached;
        private Iterator<DocumentRevision> documentBlock;

        private QueryResultIterator() {
            range = new Range(0, DEFAULT_BATCH_SIZE);
            nSkipped = 0;
            nReturned = 0;
            limitReached = false;
            documentBlock = populateDocumentBlock();
        }

        @Override
        public boolean hasNext() {
            return documentBlock.hasNext() ||
                   (!limitReached && range.location < originalDocIds.size());
        }

        @Override
        public DocumentRevision next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }

            // it is always safe to call documentBlock.next() without calling hasNext because
            // the this.hasNext() will return false if there are no more documents to read this
            // is because we load the next block of documents (if there are any), before
            // returning the last document from the last batch, ensuring that documentBlock.next()
            // will always have a document to return.
            DocumentRevision doc =  documentBlock.next();

            // Only load the next batch if the limit hasn't been reached, if it has
            // it will cause an off by one error, eg instead of 60, you'd get 61 results.
            if (!documentBlock.hasNext() && !limitReached) {
                documentBlock = populateDocumentBlock();
            }

            return doc;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private Iterator<DocumentRevision> populateDocumentBlock() {
    try {
        List<DocumentRevision> docList = new ArrayList<>();
        while (shouldContinue()) {
            processBatchOfDocuments(docList);
            if (limitReached || !docList.isEmpty()) break;
        }
        return docList.iterator();
    } catch (DocumentStoreException dse) {
        throw new NoSuchElementException(dse.toString());
    }
}

private boolean shouldContinue() {
    return range.location < originalDocIds.size() && !limitReached;
}

private void processBatchOfDocuments(List<DocumentRevision> docList) {
    prepareRangeForBatch();
    List<String> batch = originalDocIds.subList(range.location, range.location + range.length);
    List<? extends DocumentRevision> docs = database.read(batch);
    appendMatchingDocuments(docs, docList);
    range.location += range.length;
}

private void prepareRangeForBatch() {
    range.length = Math.min(DEFAULT_BATCH_SIZE, originalDocIds.size() - range.location);
}

private void appendMatchingDocuments(List<? extends DocumentRevision> docs, List<DocumentRevision> docList) {
    for (DocumentRevision rev : docs) {
        if (skipRemaining()) continue;
        if (!matches(rev)) continue;
        DocumentRevision projectedRev = projectIfNeeded(rev);
        docList.add(projectedRev);
        if (incrementAndCheckLimit()) break;
    }
}

private boolean skipRemaining() {
    if (skip > 0 && nSkipped < skip) {
        nSkipped++;
        return true;
    }
    return false;
}

private boolean matches(DocumentRevision rev) {
    return matcher == null || matcher.matches(rev);
}

private DocumentRevision projectIfNeeded(DocumentRevision rev) {
    return fields != null && !fields.isEmpty() ? projectFields(fields, rev, database) : rev;
}

private boolean incrementAndCheckLimit() {
    nReturned++;
    if (limit > 0 && nReturned >= limit) {
        limitReached = true;
        return true;
    }
    return false;
}
        }
    }

    private DocumentRevision projectFields(List<String> fields,
                                           DocumentRevision rev,
                                                   Database database) {
        // grab the map filter fields and rebuild object
        Map<String, Object> originalBody = rev.getBody().asMap();
        Map<String, Object> body = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : originalBody.entrySet()) {
            if (fields.contains(entry.getKey())) {
                body.put(entry.getKey(), entry.getValue());
            }
        }

        DocumentRevisionBuilder revBuilder = new DocumentRevisionBuilder();
        revBuilder.setDocId(rev.getId());
        revBuilder.setRevId(rev.getRevision());
        revBuilder.setBody(DocumentBodyFactory.create(body));
        revBuilder.setDeleted(rev.isDeleted());
        revBuilder.setAttachments(rev.getAttachments());
        revBuilder.setDatabase(database);

        return revBuilder.buildProjected();
    }

    private static class Range {
        public int location;
        public int length;

        private Range(int location, int length) {
            this.location = location;
            this.length = length;
        }
    }

}
