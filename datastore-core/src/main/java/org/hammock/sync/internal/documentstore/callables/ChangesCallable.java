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

package org.hammock.sync.internal.documentstore.callables;

import org.hammock.sync.documentstore.Changes;
import org.hammock.sync.documentstore.DocumentRevision;
import org.hammock.sync.internal.documentstore.AttachmentStreamFactory;
import org.hammock.sync.internal.documentstore.ChangesImpl;
import org.hammock.sync.internal.documentstore.InternalDocumentRevision;
import org.hammock.sync.internal.sqlite.Cursor;
import org.hammock.sync.internal.sqlite.SQLCallable;
import org.hammock.sync.internal.sqlite.SQLDatabase;
import org.hammock.sync.internal.util.DatabaseUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Return the list of changes to the DocumentStore, starting at a given `since` sequence value, limited
 * to a maximum number of `limit` changes
 */
public class ChangesCallable implements SQLCallable<Changes> {

    private long since;
    private int limit;

    private String attachmentsDir;
    private AttachmentStreamFactory attachmentStreamFactory;

    /**
     * @param since Starting sequence number to retrieve changes from
     * @param limit Maximum number of changes to retrieve
     * @param attachmentsDir          Location of attachments
     * @param attachmentStreamFactory Factory to manage access to attachment streams
     */
    public ChangesCallable(long since, int limit, String attachmentsDir, AttachmentStreamFactory
            attachmentStreamFactory) {
        this.since = since;
        this.limit = limit;
        this.attachmentsDir = attachmentsDir;
        this.attachmentStreamFactory = attachmentStreamFactory;
    }

    @Override
    public Changes call(SQLDatabase db) throws Exception {

        String[] args = {Long.toString(since), Long.toString(since +
                limit)};
        Cursor cursor = null;
        try {
            Long lastSequence = since;
            List<Long> ids = new ArrayList<Long>();
            cursor = db.rawQuery(CallableSQLConstants.SQL_CHANGE_IDS_SINCE_LIMIT, args);
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
                lastSequence = Math.max(lastSequence, cursor.getLong(1));
            }

            if (ids.isEmpty()){
                return new ChangesImpl(lastSequence, Collections.<DocumentRevision>emptyList());
            }

            List<InternalDocumentRevision> results = new GetDocumentsWithInternalIdsCallable(ids, attachmentsDir, attachmentStreamFactory).call(db);
            if (results.size() != ids.size()) {
                throw new IllegalStateException(String.format(Locale.ENGLISH,
                        "The number of documents does not match number of ids, " +
                                "something must be wrong here. Number of IDs: %s, " +
                                "number of documents: %s",
                        ids.size(),
                        results.size()
                ));
            }

            return new ChangesImpl(lastSequence, new ArrayList<DocumentRevision>(results));
        } catch (SQLException e) {
            throw new IllegalStateException("Error querying all changes since: " +
                    since + ", limit: " + limit, e);
        } finally {
            DatabaseUtils.closeCursorQuietly(cursor);
        }
    }

}
