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

package org.hammock.sync.documentstore;

/**
 * Created by tomblench on 28/04/2014.
 */

import java.io.IOException;
import java.io.InputStream;

/**
 * An Attachment which is read from a stream before saving to the database
 */
public class UnsavedStreamAttachment extends Attachment {

    public UnsavedStreamAttachment(InputStream stream, String type) {
        super(type, Encoding.Plain, -1);
        this.stream = stream;
    }

    public UnsavedStreamAttachment(InputStream stream, String type, Encoding encoding) {
        super(type, encoding, -1);
        this.stream = stream;
    }

    public InputStream getInputStream() throws IOException {
        return stream;
    }

    private InputStream stream;

}
