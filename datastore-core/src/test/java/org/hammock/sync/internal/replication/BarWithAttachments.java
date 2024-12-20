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


import org.hammock.sync.internal.mazha.Document;

import java.util.HashMap;

public class BarWithAttachments extends Document {

    private String name;
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }

    private int age = 0;
    public Integer getAge() {
        return this.age;
    }
    public void setAge(int age) {
        this.age = age;
    }

    public HashMap<String, Object> _attachments;

    @Override
    public String toString() {
        return "Bar: {name:" + name + ", age:" + age + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Bar)) {
            return false;
        }

        Bar that = (Bar)obj;
        return this.getId().equals(that.getId()) &&
                this.getRevision().equals(that.getRevision()) &&
                this.getName().equals(that.getName()) &&
                this.getAge().equals(that.getAge());
    }
}
