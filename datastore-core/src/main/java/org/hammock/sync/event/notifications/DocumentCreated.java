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

package org.hammock.sync.event.notifications;

import org.hammock.sync.documentstore.Database;
import org.hammock.sync.documentstore.DocumentRevision;

/**
 * <p>
 * Event for document create
 * </p>
 *
 * <p>This event is posted by
 * {@link Database#create(DocumentRevision)}
 * </p>
 */
public class DocumentCreated extends DocumentModified {

    /**
     * Event for document create
     *
     * @param newDocument
     *            New document revision
     */
    public DocumentCreated(DocumentRevision newDocument) {
        super(null, newDocument);
    }

}
