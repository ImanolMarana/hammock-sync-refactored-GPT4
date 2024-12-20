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

import org.hammock.sync.documentstore.Database;
import org.hammock.sync.internal.documentstore.DatabaseImpl;
import org.hammock.sync.documentstore.DocumentStoreException;
import org.hammock.sync.internal.sqlite.Cursor;
import org.hammock.sync.internal.sqlite.SQLCallable;
import org.hammock.sync.internal.sqlite.SQLDatabase;
import org.hammock.sync.internal.util.DatabaseUtils;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Get the most recent (highest) sequence number for the database
 */
public class GetLastSequenceCallable implements SQLCallable<Long> {

    private static final Logger logger = Logger.getLogger(DatabaseImpl.class.getCanonicalName());

    @Override
    public Long call(SQLDatabase db) throws Exception {
        String sql = "SELECT MAX(sequence) FROM revs";
        Cursor cursor = null;
        long result = 0;
        try {
            cursor = db.rawQuery(sql, null);
            if (cursor.moveToFirst()) {
                // TODO this will always be an integer or null and we can't be expected to handle other cases
                if (cursor.columnType(0) == Cursor.FIELD_TYPE_INTEGER) {
                    result = cursor.getLong(0);
                } else if (cursor.columnType(0) == Cursor.FIELD_TYPE_NULL) {
                    result = Database.SEQUENCE_NUMBER_START;
                } else {
                    throw new IllegalStateException("SQLite return an unexpected value.");
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error getting last sequence", e);
            throw new DocumentStoreException(e);
        } finally {
            DatabaseUtils.closeCursorQuietly(cursor);
        }
        return result;
    }

}
