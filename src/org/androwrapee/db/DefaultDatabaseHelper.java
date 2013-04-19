/*
 * Andro Wrapee
 *
 * Copyright 2012 - 2013 cosminstefanxp@gmail.com, rafaela.voiculescu@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.androwrapee.db;

import java.lang.reflect.Field;
import java.util.concurrent.locks.ReentrantLock;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * The Class DefaultDatabaseHelper that acts like a SQLiteOpenHelper, creating the database when
 * needed. Please check the documentation of {@link DefaultDAO} for full specifications and
 * requirements.
 * 
 */
public class DefaultDatabaseHelper extends SQLiteOpenHelper {

	/** The name of the id field in the object. */
	protected static final String ID_FIELD = "id";

	/** The table names used for this object. */
	protected final String[] mTableNames;

	/** The class. */
	@SuppressWarnings("rawtypes")
	protected final Class[] mClasses;

	/** The threads inside count. */
	private int threadsInsideCount = 0;

	/** The lock. */
	private ReentrantLock lock;

	/**
	 * Instantiates a new default database helper.
	 * 
	 * @param context the Android context
	 * @param databaseName the database name
	 * @param databaseVersion the database version
	 * @param classes the classes that this database should store
	 * @param tableNames the table names, in correspondence with the {@literal classes} parameter.
	 */
	@SuppressWarnings("rawtypes")
	public DefaultDatabaseHelper(Context context, String databaseName, int databaseVersion, Class classes[],
			String tableNames[]) {
		super(context, databaseName, null, databaseVersion);
		this.mTableNames = tableNames;
		this.mClasses = classes;
		this.lock = new ReentrantLock();
	}

	/**
	 * Multi thread open.
	 * 
	 * @return the sQ lite database
	 */
	public SQLiteDatabase multiThreadOpen() {
		lock.lock();
		SQLiteDatabase mDb = this.getWritableDatabase();
		threadsInsideCount++;
		lock.unlock();
		return mDb;
	}

	/**
	 * Multi thread close.
	 * 
	 * @return the sQ lite database
	 */
	public void multiThreadClose() {
		lock.lock();
		threadsInsideCount--;
		if (threadsInsideCount == 0) {
			this.close();
		}
		lock.unlock();
	}

	/* (non-Javadoc)
	 * 
	 * @see
	 * android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase) */
	@SuppressWarnings("rawtypes")
	@Override
	public void onCreate(SQLiteDatabase database) {

		// For every class type, create the corresponding table
		for (int i = 0; i < mClasses.length; i++) {
			Class cls = mClasses[i];
			String tableName = mTableNames[i];

			// Build the reflection manager for the current class
			ReflectionManager rm;
			try {
				rm = new ReflectionManager(cls);
			} catch (IllegalClassStructureException e) {
				e.printStackTrace();
				Log.e("aw.db", "IllegalClassStructure Exception for class " + cls + ": " + e.getMessage());
				continue;
			}

			// Build the Create Table Statement
			String createQuery = "CREATE TABLE " + tableName + "  ( ";

			// Build the query for the id field
			createQuery += DefaultDAO.ID_PREPENDER + rm.getIdField().getName() + " integer primary key autoincrement, ";

			// Build the query for database fields
			for (Field field : rm.getDatabaseFields()) {
				{
					// Special type checks
					if (field.getType().equals(Double.class) || field.getType().equals(double.class)
							|| field.getType().equals(Float.class) || field.getType().equals(float.class))
						createQuery += field.getName() + " real, ";
					else if (field.getType().equals(Integer.class) || field.getType().equals(int.class)
							|| field.getType().equals(Short.class) || field.getType().equals(short.class)
							|| field.getType().equals(Byte.class) || field.getType().equals(byte.class))
						createQuery += field.getName() + " integer, ";
					else
						createQuery += field.getName() + " text not null, ";
				}
			}

			// Build the query for parent reference fields
			for (Field field : rm.getReferenceFields()) {
				createQuery += DefaultDAO.REFERENCE_PREPENDER + field.getName() + " integer, ";
			}

			createQuery = createQuery.substring(0, createQuery.length() - 2);
			createQuery += ");";

			// Create the table
			Log.w("aw.db", "Creating database with query: " + createQuery);
			database.execSQL(createQuery);
		}
	}

	/* (non-Javadoc)
	 * 
	 * @see
	 * android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase,
	 * int, int) */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Delete all existing data and re-create the table
		Log.w("aw.db", "Upgrading database from version " + oldVersion + " to " + newVersion
				+ ", which will destroy all existing data.");
		for (int i = 0; i < mTableNames.length; i++)
			db.execSQL("DROP TABLE IF EXISTS " + mTableNames[i]);
		onCreate(db);
	}

}
