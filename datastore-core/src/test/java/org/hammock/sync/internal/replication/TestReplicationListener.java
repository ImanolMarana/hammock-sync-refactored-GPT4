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

import org.hammock.sync.event.Subscribe;
import org.hammock.sync.event.notifications.ReplicationCompleted;
import org.hammock.sync.event.notifications.ReplicationErrored;
import org.hammock.sync.util.TestEventListener;

public class TestReplicationListener extends TestEventListener {

    @Subscribe
    public void complete(ReplicationCompleted rc) {
        finishCalled = true;
        batchesReplicated = rc.batchesReplicated;
        documentsReplicated = rc.documentsReplicated;
    }

    @Subscribe
    public void error(ReplicationErrored re) {
        errorCalled = true;
        errorInfo = re.errorInfo;
    }
}
