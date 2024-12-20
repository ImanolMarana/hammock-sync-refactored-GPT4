/*
 * Copyright © 2015 IBM Corp. All rights reserved.
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

import org.hammock.sync.internal.sqlite.SQLDatabase;

import java.util.Arrays;

/**
 * Runs a database migration which consists only of SQL statements.
 */
public class SchemaOnlyMigration implements Migration {

    private String[] statements;

    public SchemaOnlyMigration(String[] statements) {
        this.statements = Arrays.copyOf(statements, statements.length);
    }

    public void runMigration(SQLDatabase db) throws Exception {
        for (String statement : statements) {
            db.execSQL(statement);
        }
    }
}
