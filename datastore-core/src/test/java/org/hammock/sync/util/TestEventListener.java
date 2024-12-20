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

package org.hammock.sync.util;

import org.junit.Assert;

public class TestEventListener {

    public Throwable errorInfo = null;
    public boolean errorCalled = false;
    public boolean finishCalled = false;
    public int documentsReplicated = 0;
    public int batchesReplicated = 0;

    public void assertReplicationCompletedOrThrow() throws Exception {
        if (errorCalled) {
            throw new Exception("Replication errored", errorInfo);
        } else {
            Assert.assertTrue("The replication should finish.", finishCalled);
        }
    }
}
