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

package org.hammock.sync.internal.sqlite.android;

/**
 * Similar to AndroidSQLite class.  Replaces Android SQLite implementation with
 * SQLCipher-based SQLite.
 * SQLCipher adds:
 * - TODO A user-based password (provided with the KeyProvider object) for encryption
 * - TODO Method to change password (will be handled through key management feature)
 *
 * Created by estebanmlaver.
 */

import android.database.SQLException;

import org.hammock.sync.internal.android.ContentValues;
import org.hammock.sync.documentstore.encryption.KeyProvider;
import org.hammock.sync.internal.documentstore.encryption.KeyUtils;
import org.hammock.sync.internal.sqlite.Cursor;
import org.hammock.sync.internal.sqlite.SQLDatabase;
import org.hammock.sync.internal.util.Misc;

import net.zetetic.database.sqlcipher.SQLiteDatabase;


import java.io.File;

public class AndroidSQLCipherSQLite extends SQLDatabase {

    SQLiteDatabase database = null;

    /**
     * Constructor for creating SQLCipher-based SQLite database.
     * @param path full file path of the db file
     * @param provider Provider object that contains the key to encrypt the SQLCipher database
     * @return
     */
    public static AndroidSQLCipherSQLite open(File path, KeyProvider provider) {

        //Call SQLCipher-based method for opening database, or creating if database not found
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(path.toString(),KeyUtils.sqlCipherKeyForKeyProvider(provider),null,null);
        return new AndroidSQLCipherSQLite(db);
    }

    public AndroidSQLCipherSQLite(final SQLiteDatabase database) {
        this.database = database;
    }

    @Override
    public void compactDatabase() {
        try {
            database.execSQL("VACUUM");
        } catch (SQLException e) {
            String error = "Fatal error running 'VACUUM', the database is probably malfunctioning.";
            throw new IllegalStateException(error);
        }
    }

    @Override
    public void open() {
        // database should be already opened
    }

    @Override
    public void close() {
        this.database.close();
    }

    // This implementation of isOpen will only return true if the database is open AND the current
    // thread has not called the close() method on this object previously, this makes it compatible
    // with the JavaSE SQLiteWrapper, where each thread closes its own connection.
    @Override
    public boolean isOpen() {
        return this.database.isOpen();
    }

    @Override
    public void beginTransaction() {
        this.database.beginTransaction();
    }

    @Override
    public void endTransaction() {
        this.database.endTransaction();
    }

    @Override
    public void setTransactionSuccessful() {
        this.database.setTransactionSuccessful();
    }

    @Override
    public void execSQL(String sql) throws java.sql.SQLException {
        Misc.checkNotNullOrEmpty(sql.trim(), "Input SQL");
        try {
            this.database.execSQL(sql);
        } catch (SQLException e) {
            throw new java.sql.SQLException(e);
        }
    }

    public int status(int operation, boolean reset) {
        return 0;
    }

    @Override
    public void execSQL(String sql, Object[] bindArgs) throws java.sql.SQLException {
        Misc.checkNotNullOrEmpty(sql.trim(), "Input SQL");
        try {
            this.database.execSQL(sql, bindArgs);
        } catch (SQLException e) {
            throw new java.sql.SQLException(e);
        }
    }

    @Override
    public int getVersion() {
        return this.database.getVersion();
    }

    @Override
    public int update(String table, ContentValues args, String whereClause, String[] whereArgs) {
        return this.database.update("\""+table+"\"", this.createAndroidContentValues(args), whereClause, whereArgs);
    }

    @Override
    public Cursor rawQuery(String sql, String[] values) throws java.sql.SQLException {
        try {
            return new AndroidSQLiteCursor(this.database.rawQuery(sql, values));
        } catch (SQLException e) {
            throw new java.sql.SQLException(e);
        }
    }

    @Override
    public int delete(String table, String whereClause, String[] whereArgs) {
        return this.database.delete("\""+table+"\"", whereClause, whereArgs);
    }

    @Override
    public long insert(String table, ContentValues args) {
        return this.insertWithOnConflict(table, args, SQLiteDatabase.CONFLICT_NONE);
    }

    @Override
    public long insertWithOnConflict(String table, ContentValues initialValues, int conflictAlgorithm) {
        //android DB will thrown an exception rather than return a -1 row ID if there is a failure
        // so we catch constraintException and return -1
        try {
            return this.database.insertWithOnConflict("\""+table+"\"", null,
                    createAndroidContentValues(initialValues), conflictAlgorithm);
        } catch (Exception sqlce){
            return -1;
        }
    }

    private android.content.ContentValues createAndroidContentValues(ContentValues values) {
        android.content.ContentValues newValues = new android.content.ContentValues(values.size());
        for(String key : values.keySet()) {
            Object value = values.get(key);
            if(value instanceof Boolean) {
                newValues.put(key, (Boolean)value);
            } else if(value instanceof Byte) {
                newValues.put(key, (Byte)value);
            } else if(value instanceof byte[]) {
                newValues.put(key, (byte[])value);
            } else if(value instanceof Double) {
                newValues.put(key, (Double)value);
            } else if(value instanceof Float) {
                newValues.put(key, (Float)value);
            } else if(value instanceof Integer) {
                newValues.put(key, (Integer)value);
            } else if(value instanceof Long) {
                newValues.put(key, (Long)value);
            } else if(value instanceof Short) {
                newValues.put(key, (Short)value);
            } else if(value instanceof String) {
                newValues.put(key, (String)value);
            } else if( value == null) {
                newValues.putNull(key);
            } else {
                throw new IllegalArgumentException("Unsupported data type: " + value.getClass());
            }
        }
        return newValues;
    }

    @Override
    protected void finalize() throws Throwable{
        super.finalize();
        if(this.database.isOpen())
            this.database.close();
    }
}
