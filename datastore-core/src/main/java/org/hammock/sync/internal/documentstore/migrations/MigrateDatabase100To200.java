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

package org.hammock.sync.internal.documentstore.migrations;

import org.hammock.sync.internal.android.ContentValues;
import org.hammock.sync.internal.documentstore.callables.PickWinningRevisionCallable;
import org.hammock.sync.internal.sqlite.Cursor;
import org.hammock.sync.internal.sqlite.SQLDatabase;
import org.hammock.sync.internal.util.DatabaseUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * <p>
 * Migration to repair datastores impacted by issues 326 and 329.
 * </p>
 * <p>
 * Added migration on `Datastore` opening to repair datastores that have documents with
 * multiple identical revision IDs (as caused by issue #329). The migration will delete duplicate
 * revisions and correct the document tree. It will also re-evaluate the winning revision resolving
 * issues caused by #326.
 * </p>
 * <p>
 * It also deletes duplicate entries with the same sequence and attachment filename, which could
 * have been force inserted into the attachments table during a multipart attachment pull during
 * identical replications running in parallel.
 * </p>
 */
public class MigrateDatabase100To200 implements Migration {

    private static final Logger LOGGER = Logger.getLogger(MigrateDatabase100To200.class.getName());
    private static final String ALL_BUT_LOWEST_WITH_ID_REV = "(SELECT sequence FROM revs WHERE " +
            "doc_id = ? AND revid = ? AND sequence != ?)";
    private String[] schemaUpdates;

    public MigrateDatabase100To200(String[] schemaUpdates) {
        this.schemaUpdates = Arrays.copyOf(schemaUpdates, schemaUpdates.length);
    }

    @Override
    public void runMigration(SQLDatabase db) throws Exception {
        Cursor c = null;
        try {
            List<DocRevSequence> lowestDuplicateRevs = findLowestDuplicateRevs(db);
            performCleanupForDuplicates(db, lowestDuplicateRevs);
            new SchemaOnlyMigration(schemaUpdates).runMigration(db);
        } finally {
            DatabaseUtils.closeCursorQuietly(c);
        }
    }

    private List<DocRevSequence> findLowestDuplicateRevs(SQLDatabase db) throws Exception {
        List<DocRevSequence> lowestDuplicateRevs = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT doc_id, revid, min(sequence) FROM revs GROUP BY doc_id, revid HAVING COUNT(*) > 1;", null)) {
            while (c.moveToNext()) {
                lowestDuplicateRevs.add(new DocRevSequence(c.getLong(0), c.getString(1), c.getLong(2)));
            }
        }
        LOGGER.info(String.format("Found %d duplicated revisions.", lowestDuplicateRevs.size()));
        return lowestDuplicateRevs;
    }
    
    private void performCleanupForDuplicates(SQLDatabase db, List<DocRevSequence> lowestDuplicateRevs) throws Exception {
        for (DocRevSequence lowest : lowestDuplicateRevs) {
            updateChildrenAndAttachments(db, lowest);
            deleteDuplicateRevs(db, lowest);
            cleanupDuplicateAttachments(db, lowest);
            new PickWinningRevisionCallable(lowest.doc_id).call(db);
        }
    }

    private void updateChildrenAndAttachments(SQLDatabase db, DocRevSequence lowest) throws Exception {
        ContentValues lowestSequenceAsParent = new ContentValues(1);
        lowestSequenceAsParent.put("parent", lowest.sequence);
        ContentValues lowestSequenceAsSequence = new ContentValues(1);
        lowestSequenceAsSequence.put("sequence", lowest.sequence);
        String[] allButLowestArgs = new String[]{lowest.doc_id.toString(), lowest.revid, lowest.sequence.toString()};
        int childrenUpdated = db.update("revs", lowestSequenceAsParent, "parent IN " + ALL_BUT_LOWEST_WITH_ID_REV, allButLowestArgs);
        int attachmentsMigrated = db.update("attachments", lowestSequenceAsSequence, "sequence IN " + ALL_BUT_LOWEST_WITH_ID_REV, allButLowestArgs);
        logUpdates(childrenUpdated, attachmentsMigrated, lowest);
    }

    private void deleteDuplicateRevs(SQLDatabase db, DocRevSequence lowest) throws Exception {
        String[] allButLowestArgs = new String[]{lowest.doc_id.toString(), lowest.revid, lowest.sequence.toString()};
        int deleted = db.delete("revs", "sequence IN " + ALL_BUT_LOWEST_WITH_ID_REV, allButLowestArgs);
        if (deleted > 0) {
            LOGGER.info(String.format("Deleted %d duplicate revisions of %d:%d", deleted, lowest.doc_id, lowest.sequence));
        }
    }

    private void logUpdates(int childrenUpdated, int attachmentsMigrated, DocRevSequence lowest){
        if (childrenUpdated > 0) {
            LOGGER.info(String.format("Updated %d children to have parent %d:%d", childrenUpdated, lowest.doc_id, lowest.sequence));
        }
        if (attachmentsMigrated > 0) {
            LOGGER.info(String.format("Migrated %d attachments to %d:%d", attachmentsMigrated, lowest.doc_id, lowest.sequence));
        }
    }

    private void cleanupDuplicateAttachments(SQLDatabase db, DocRevSequence lowest) throws Exception {
        Map<String, Integer> duplicateAttachmentFilenames = new HashMap<>();
        try (Cursor c = db.rawQuery("SELECT filename, COUNT(*) FROM attachments WHERE sequence=? GROUP BY filename HAVING COUNT(*) > 1", new String[]{lowest.sequence.toString()})) {
            while (c.moveToNext()) {
                duplicateAttachmentFilenames.put(c.getString(0), c.getInt(1));
            }
        }
        for (Map.Entry<String, Integer> entry : duplicateAttachmentFilenames.entrySet()) {
            deleteExcessAttachments(db, lowest, entry.getKey(), entry.getValue());
        }
    }

    private void deleteExcessAttachments(SQLDatabase db, DocRevSequence lowest, String filename, int count) throws Exception {
        int toDelete = count - 1;
        int deleted = db.delete("attachments", "rowid IN (SELECT rowid FROM attachments WHERE sequence=? AND filename=? ORDER BY rowid DESC limit ?)", new String[]{lowest.sequence.toString(), filename, String.valueOf(toDelete)});
        LOGGER.info(String.format("Deleted %d copies of attachment '%s' on %d:%d", deleted, filename, lowest.doc_id, lowest.sequence));
    }

    private static final class DocRevSequence {
        private final Long doc_id;
        private final Long sequence;
        private final String revid;

        private DocRevSequence(Long id, String rev, Long seq) {
            this.doc_id = id;
            this.revid = rev;
            this.sequence = seq;
        }
    }
}
        } finally {
            DatabaseUtils.closeCursorQuietly(c);
        }
    }

    private static final class DocRevSequence {
        private final Long doc_id;
        private final Long sequence;
        private final String revid;

        private DocRevSequence(Long id, String rev, Long seq) {
            this.doc_id = id;
            this.revid = rev;
            this.sequence = seq;
        }
    }
}
