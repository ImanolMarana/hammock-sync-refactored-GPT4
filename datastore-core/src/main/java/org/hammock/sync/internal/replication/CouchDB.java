/*
 * Copyright © 2017 IBM Corp. All rights reserved.
 *
 * Copyright © 2013 Cloudant, Inc. All rights reserved.
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

package org.hammock.sync.internal.replication;

import org.hammock.sync.internal.documentstore.InternalDocumentRevision;
import org.hammock.sync.internal.documentstore.DocumentRevsList;
import org.hammock.sync.internal.documentstore.MultipartAttachmentWriter;
import org.hammock.sync.internal.mazha.ChangesResult;
import org.hammock.sync.internal.mazha.CouchClient;
import org.hammock.sync.internal.mazha.DocumentRevs;
import org.hammock.sync.internal.mazha.Response;
import org.hammock.sync.replication.PullFilter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

interface CouchDB {

    String getIdentifier();

    /**
     * @return true if database exists, otherwise false
     */
    boolean exists();

    Response create(Object object);
    Response update(String id, Object object);
    <T> T get(Class<T> classType, String id);
    Response delete(String id, String rev);

    String getCheckpoint(String checkpointId);
    void putCheckpoint(String checkpointId, String sequence);

    ChangesResult changes(Object lastSequence, int limit);
    ChangesResult changes(PullFilter filter, Object lastSequence, int limit);
    ChangesResult changes(String selector, Object lastSequence, int limit);
    ChangesResult changes(List<String> docIds, Object lastSequence, int limit);
    List<DocumentRevs> getRevisions(String documentId,
                                           Collection<String> revisionIds,
                                           Collection<String> attsSince,
                                           boolean pullAttachmentsInline);
    void bulkCreateDocs(List<InternalDocumentRevision> revisions);
    void bulkCreateSerializedDocs(List<String> serializedDocs);
    List<Response> putMultiparts(List<MultipartAttachmentWriter> multiparts);
    Map<String, CouchClient.MissingRevisions> revsDiff(Map<String, Set<String>> revisions);

    Iterable<DocumentRevsList> bulkGetRevisions(List<BulkGetRequest> requests,
                                                boolean pullAttachmentsInline);
    boolean isBulkSupported();

    <T> T pullAttachmentWithRetry(String id, String rev, String name, CouchClient.InputStreamProcessor<T> streamProcessor);

}
