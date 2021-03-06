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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Simple database access helper class, for SQLite for Android. Defines the basic CRUD operations
 * for the and gives the ability to insert, get, update or delete objects from the database.<br/>
 * <br/>
 * 
 * Uses Java Reflection for creating the database entries. This has some implications and
 * restrictions:
 * <ul>
 * <li>The Classes that should be saved in the database have to have the {@link DatabaseClass}
 * annotation.</li>
 * <li>The Classes can have super classes which will be saved recursively in the database, as long
 * as they are marked with the {@link DatabaseClass} annotation.</li>
 * <li>All the fields that need to be stored in the database must be marked with the
 * {@link DatabaseField} or {@link IdField} annotations.</li>
 * <li>Every object MUST have an id field annotated with {@link IdField} of recommended type long
 * (but must be numerical), which will be saved as {@literal _id_}+field name in the database.</li>
 * <li>All other fields will be saved in the database with the same name as the object field.</li>
 * <li>The class requires a constructor with no parameters, otherwise it will throw an exception.</li>
 * <li>The class or any of the super classes can have a field marked with {@link ReferenceField}
 * which will not be stored in the database as it is, but it MUST implement the
 * {@link DatabaseReferenceClass} class, thus providing an 'id' which will be stored in the database
 * as {@code _rid_}+field name. This field can be used for queries and for making one-to-many
 * relationships.</li>
 * </ul>
 * 
 * @param <T> the type of the classes manipulated by the DefaultDAO.
 */
public class DefaultDAO<T> {

	protected String mTableName;

	public static final String ID_PREPENDER = "_id_";
	public static final String REFERENCE_PREPENDER = "_rid_";

	public static final String BOOLEAN_TRUE_VALUE = "#t";
	public static final String BOOLEAN_FALSE_VALUE = "#f";

	/** The column names. */
	protected String mColumnNames[];

	protected ReflectionManager mReflectionManager;

	/** The database. */
	private SQLiteDatabase mDb;

	/** The database helper. */
	private DefaultDatabaseHelper mDbHelper;

	/** The class. */
	private final Class<T> mClass;

	/**
	 * Constructor - takes the context to allow the database to be opened/created.
	 * 
	 * @param c the class
	 * @param dbHelper the db helper
	 * @param rm the rm
	 * @param tableName the table name
	 */
	public DefaultDAO(Class<T> c, DefaultDatabaseHelper dbHelper, ReflectionManager rm, String tableName) {
		super();
		this.mClass = c;
		this.mDbHelper = dbHelper;
		this.mTableName = tableName;
		this.mReflectionManager = rm;

		// Create the column names
		ArrayList<String> columnNames = new ArrayList<String>();
		columnNames.add(ID_PREPENDER + mReflectionManager.getIdField().getName());
		for (Field f : mReflectionManager.getDatabaseFields())
			columnNames.add(f.getName());
		for (Field f : mReflectionManager.getReferenceFields())
			columnNames.add(REFERENCE_PREPENDER + f.getName());

		mColumnNames = (String[]) columnNames.toArray(new String[columnNames.size()]);

	}

	/**
	 * Open the database. If it cannot be opened, try to create a new instance of the database. If
	 * it cannot be created, throw an exception to signal the failure
	 * 
	 * @return this (self reference, allowing this to be chained in an initialization call)
	 * @throws SQLException if the database could be neither opened or created
	 */
	public DefaultDAO<T> open() throws SQLException {
		mDb = mDbHelper.multiThreadOpen();
		return this;
	}

	/**
	 * Close the database.
	 */
	public void close() {
		mDbHelper.multiThreadClose();
	}

	/**
	 * Inserts a new entry in the database for the object provided. If the entry is successfully
	 * created return the new rowId for that entry, otherwise return a -1 to indicate failure.
	 * <p>
	 * Not that, for efficiency reasons, the id field in the object is not set by this method, but
	 * it's returned, so it can be set manually as needed.
	 * </p>
	 * <p>
	 * The {@code generateID} parameter sets the way the database id field is filled. If the value
	 * is {@literal true}, the id is generated by the database, otherwise, the value of the id field
	 * from the object is used. In the latter case, it is the responsibility of the user to guaranty
	 * the uniqueness of the id value.
	 * </p>
	 * 
	 * @param newObject the new object
	 * @param generateID whether the database id is generated by the database or the field in the
	 *            object is used as an id.
	 * 
	 * @return rowId or -1 if failed
	 */
	public long insert(T newObject, boolean generateID) {

		ContentValues initialValues;
		try {
			initialValues = buildContentValues(newObject, !generateID);
		} catch (Exception e) {
			Log.e("aw.db",
					"Error occured while parsing object for insertion: " + newObject + ". Error message: "
							+ e.getMessage());
			e.printStackTrace();
			return -1;
		}

		// Use the ContentValues to insert the entry in the database and return the row id.
		Log.d("aw.db", "Inserting: " + initialValues);
		long id = mDb.insert(mTableName, null, initialValues);
		return id;
	}

	/**
	 * Update the entry in the database corresponding to the provided object. The row id is given
	 * separately.
	 * 
	 * @param object the object
	 * @param rowId the row id
	 * @return true if the object was successfully updated, false otherwise
	 */
	public boolean update(T object, long rowId) {
		ContentValues args;
		try {
			args = buildContentValues(object, true);
		} catch (Exception e) {
			Log.e("aw.db",
					"Error occured while parsing object for update: " + object + ". Error message: " + e.getMessage());
			e.printStackTrace();
			return false;
		}

		return mDb.update(mTableName, args, ID_PREPENDER + mReflectionManager.getIdField().getName() + "=" + rowId,
				null) > 0;
	}

	/**
	 * Builds the content values required to update or create a new entry in the database.
	 * 
	 * @param object the object
	 * @param setID whether to set the ID_FIELD to the value in the object, or leave it without any
	 *            value, so it will be generated by itself, if the database column is set as
	 * @return the content values
	 * @throws IllegalArgumentException the illegal argument exception
	 * @throws IllegalAccessException the illegal access exception
	 * @throws SecurityException the security exception
	 * @throws NoSuchFieldException the no such field exception
	 * @throws NoSuchMethodException the no such method exception {@literal autoincrement}
	 * @throws InvocationTargetException the invocation target exception
	 */
	protected ContentValues buildContentValues(T object, boolean setID) throws IllegalArgumentException,
			IllegalAccessException, SecurityException, NoSuchFieldException, NoSuchMethodException,
			InvocationTargetException {
		ContentValues contentValues = new ContentValues();

		// Take the id field
		if (setID) {
			Field id = mReflectionManager.getIdField();
			contentValues.put(ID_PREPENDER + id.getName(), id.get(object).toString());
		}

		// Take every field in the object and insert it in the ContentValues entry
		for (Field field : mReflectionManager.getDatabaseFields()) {

			Object value = field.get(object);
			String fieldName = field.getName();

			// Special type checks
			if (field.getType().equals(Date.class))
				value = ((Date) value).getTime();
			if (field.getType().equals(boolean.class) || field.getType().equals(Boolean.class)) {
				if ((Boolean) value)
					value = BOOLEAN_TRUE_VALUE;
				else
					value = BOOLEAN_FALSE_VALUE;
			}
			contentValues.put(fieldName, value.toString());
		}

		// if it's a reference field, get the id and put it in the database
		for (Field field : mReflectionManager.getReferenceFields()) {
			Object reference = field.get(object);
			if (reference == null)
				continue; // don't add
			Method referenceGetMethod = reference.getClass().getMethod("getId", (Class[]) null);
			referenceGetMethod.setAccessible(true);
			Long referenceId = (Long) referenceGetMethod.invoke(reference, (Object[]) null);
			contentValues.put(REFERENCE_PREPENDER + field.getName(), referenceId.toString());
		}

		return contentValues;
	}

	/**
	 * Delete the entry with the given rowId.
	 * 
	 * @param rowId id of the object to delete
	 * @return true if anything was deleted, false otherwise
	 */
	public boolean delete(long rowId) {
		return mDb.delete(mTableName, ID_PREPENDER + mReflectionManager.getIdField().getName() + "=" + rowId, null) > 0;
	}

	/**
	 * Delete all the entries that match a given whereClause.
	 * 
	 * @param whereClause the where clause
	 * @return the count of rows affected, or 0 otherwise.
	 */
	public int delete(String whereClause) {
		return mDb.delete(mTableName, whereClause, null);
	}

	/**
	 * Return the object positioned at the entry that matches the given id.
	 * 
	 * @param rowId id of object to retrieve
	 * @return the object fetched from the database
	 */
	public T fetch(long rowId) {

		// Get the cursor for the database entry
		Cursor cursor = mDb.query(true, mTableName, mColumnNames, ID_PREPENDER
				+ mReflectionManager.getIdField().getName() + "=" + rowId, null, null, null, null, null);

		if (cursor == null || cursor.getCount() == 0) {
			cursor.close();
			return null;
		}

		// Build the object from the cursor
		cursor.moveToFirst();
		try {
			T object = buildObject(cursor);
			cursor.close();
			return object;
		} catch (Exception e) {
			Log.e("aw.db", "Error occured while building object of type " + mClass + " from cursor: " + cursor + ".");
			Log.e("aw.db", "Error message: " + e.getMessage());
			e.printStackTrace();
			cursor.close();
			return null;
		}
	}

	/**
	 * Return the cursor positioned at the entry that matches the given id.
	 * 
	 * @param rowId the row id
	 * @return the cursor
	 */
	public Cursor fetchCursor(long rowId) {
		// Get the cursor for the database entry
		Cursor cursor = mDb.query(true, mTableName, mColumnNames, ID_PREPENDER
				+ mReflectionManager.getIdField().getName() + "=" + rowId, null, null, null, null, null);

		if (cursor == null || cursor.getCount() == 0)
			return null;
		cursor.moveToFirst();
		return cursor;
	}

	/**
	 * Fetches all the object in the database that match a given where clause.
	 * 
	 * @param where the where clause; null means it will return all rows
	 * @return the array list
	 */
	public ArrayList<T> fetchAll(String where) {
		ArrayList<T> objects = new ArrayList<T>();

		// Get the cursor for the database entry
		Cursor cursor = mDb.query(true, mTableName, mColumnNames, where, null, null, null, null, null);
		if (cursor == null || cursor.getCount() == 0) {
			cursor.close();
			return objects;
		}

		// Build the objects from the cursor
		cursor.moveToFirst();
		try {

			// Build all objects from the cursor
			while (!cursor.isAfterLast()) {
				T object = buildObject(cursor);
				objects.add(object);
				cursor.moveToNext();
			}

			cursor.close();
			return objects;
		} catch (Exception e) {
			Log.e("aw.db", "Error occured while building objects of type " + mClass + " from cursor: " + cursor + ".");
			Log.e("aw.db", "Error message: " + e.getMessage());
			e.printStackTrace();
			cursor.close();
			return null;
		}
	}

	/**
	 * Return the cursor positioned at the first entry of the results of the query with the provided
	 * whereClause. The cursor can be used to cycle through all the query results.
	 * 
	 * @param whereClause the where clause
	 * @return the cursor
	 */
	public Cursor fetchCursor(String whereClause) {
		// Get the cursor for the database entry
		Cursor cursor = mDb.query(true, mTableName, mColumnNames, whereClause, null, null, null, null, null);
		cursor.moveToFirst();
		return cursor;
	}

	/**
	 * Count the entries in the database that match a given whereClause.
	 * 
	 * @param whereClause the where clause
	 * @return the count
	 */
	public int countEntries(String whereClause) {
		final String sqlQuery = "SELECT COUNT(*) FROM " + mTableName + " WHERE " + whereClause;
		Cursor mCount = mDb.rawQuery(sqlQuery, null);
		mCount.moveToFirst();
		int count = mCount.getInt(0);
		mCount.close();
		return count;
	}

	/**
	 * Gets the id for a referenced entry. The fieldName must be given like the object's field name
	 * (without the prepender).
	 * 
	 * @param cursor the cursor
	 * @param fieldName the field name, without prepender
	 * @return the reference id
	 */
	public long getReferenceId(Cursor cursor, String fieldName) {
		int columnIndex = cursor.getColumnIndex(REFERENCE_PREPENDER + fieldName);
		return cursor.getLong(columnIndex);
	}

	/**
	 * Gets the database reference that can be used to manually do queries.
	 * 
	 * @return the database reference
	 */
	public SQLiteDatabase getDatabaseReference() {
		return mDb;
	}

	/**
	 * Builds the object based on a given cursor. Does not fill the reference fields, which remain
	 * null. Please use {@code getReferenceId} to get the ids for the referenced entries.
	 * 
	 * @param cursor the cursor
	 * @return the object
	 * @throws IllegalAccessException the illegal access exception
	 * @throws InstantiationException the instantiation exception
	 * @throws IllegalArgumentException the illegal argument exception
	 * @throws ParseException the parse exception
	 * @throws SecurityException the security exception
	 * @throws NoSuchMethodException the no such method exception
	 * @throws InvocationTargetException the invocation target exception
	 * @throws IllegalClassStructureException the illegal class structure exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public T buildObject(Cursor cursor) throws IllegalAccessException, InstantiationException,
			IllegalArgumentException, ParseException, SecurityException, NoSuchMethodException,
			InvocationTargetException, IllegalClassStructureException {
		// Create a new instance of the class, that will be populated with information
		// Hack for private/inner classes
		Constructor<T> c = mClass.getDeclaredConstructor();
		c.setAccessible(true);
		T object = c.newInstance();

		// Fill in the id
		Field idField = mReflectionManager.getIdField();
		int columnIndex = cursor.getColumnIndex(ID_PREPENDER + idField.getName());
		idField.set(object, cursor.getLong(columnIndex));

		// For every database field in the class, fill it with data from the cursor
		for (Field field : mReflectionManager.getDatabaseFields()) {

			columnIndex = cursor.getColumnIndex(field.getName());

			// Special type checks
			if (field.getType() == int.class || field.getType() == Integer.class)
				field.set(object, cursor.getInt(columnIndex));
			else if (field.getType() == short.class || field.getType() == Short.class)
				field.set(object, cursor.getShort(columnIndex));
			else if (field.getType() == long.class || field.getType() == Long.class)
				field.set(object, cursor.getLong(columnIndex));
			else if (field.getType() == double.class || field.getType() == Double.class)
				field.set(object, cursor.getDouble(columnIndex));
			else if (field.getType() == float.class || field.getType() == Float.class)
				field.set(object, cursor.getFloat(columnIndex));
			else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
				if (cursor.getString(columnIndex).equalsIgnoreCase(BOOLEAN_TRUE_VALUE))
					field.set(object, true);
				else
					field.set(object, false);
			} else if (field.getType() == Date.class)
				field.set(object, new Date(cursor.getLong(columnIndex)));
			else if (field.getType() == String.class)
				field.set(object, cursor.getString(columnIndex));
			else if (field.getType().isEnum()) {
				String enumValue = (String) cursor.getString(columnIndex);
				if (enumValue == null)
					continue;
				field.set(object, Enum.valueOf((Class<Enum>) field.getType(), enumValue));
			} else
				throw new IllegalClassStructureException("Unknown field type in Database Class.");
		}

		return object;
	}

}
