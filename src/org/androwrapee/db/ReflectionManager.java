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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

/**
 * The ReflectionManager manages and gets the required field of a given class, to be used in the
 * rest of the Database Provider Classes. Please check the documentation for {@link DefaultDAO} for
 * full specifications and requirements. <br/>
 * <br/>
 * Each field should be annotated with one Annotation maximum and each class (including
 * superclasses) that needs to be stored to the database has to have the {@link DatabaseClass}
 * annotation.
 * 
 */
public class ReflectionManager {

	/** The c. */
	@SuppressWarnings("rawtypes")
	private Class c;

	/** The reference fields. */
	private List<Field> referenceFields;

	/** The database fields. */
	private List<Field> databaseFields;

	/** The id field. */
	private Field idField;

	/**
	 * Instantiates a new reflection manager.
	 * 
	 * @param c the c
	 * @throws IllegalClassStructureException the database class structure exception
	 */
	@SuppressWarnings("rawtypes")
	public ReflectionManager(Class c) throws IllegalClassStructureException {
		super();
		this.c = c;
		referenceFields = new LinkedList<Field>();
		databaseFields = new LinkedList<Field>();

		prepareFields();
	}

	/**
	 * Checks if is database field.
	 * 
	 * @param f the field
	 * @return true, if is database field
	 */
	private boolean isDatabaseField(Field f) {
		DatabaseField annotation = f.getAnnotation(DatabaseField.class);
		if (annotation == null)
			return false;
		return true;
	}

	/**
	 * Checks if is the field references another object.
	 * 
	 * @param f the field
	 * @return true, if is reference field
	 */
	private boolean isReferenceField(Field f) {
		ReferenceField annotation = f.getAnnotation(ReferenceField.class);
		if (annotation == null)
			return false;
		return true;
	}

	/**
	 * Checks if is id field.
	 * 
	 * @param f the field
	 * @return true, if is id field
	 */
	private boolean isIdField(Field f) {
		IdField annotation = f.getAnnotation(IdField.class);
		if (annotation == null)
			return false;
		return true;
	}

	/**
	 * Process the fields in a given class. Should be called on a class annotated with
	 * 
	 * @param cls the class {@link DatabaseClass}
	 */
	@SuppressWarnings("rawtypes")
	private void processFields(Class cls) {
		Field[] fields = cls.getDeclaredFields();

		// Process each field, make it accessible and classify it according to the annotation
		for (Field field : fields) {
			field.setAccessible(true);

			if (isIdField(field)) {
				this.idField = field;
				continue;
			}
			if (isDatabaseField(field)) {
				this.databaseFields.add(field);
				continue;
			}
			if (isReferenceField(field))
				this.referenceFields.add(field);
		}
	}

	/**
	 * Prepare the fields.
	 * 
	 * @throws IllegalClassStructureException the database class structure exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void prepareFields() throws IllegalClassStructureException {

		Annotation annotation;

		// Process the main class
		annotation = c.getAnnotation(DatabaseClass.class);
		if (annotation == null)
			throw new IllegalClassStructureException("Class " + c.getName()
					+ " is not a DatabaseClass. Check for the required annotation: "
					+ DatabaseClass.class.getSimpleName());
		processFields(c);

		// Process any base class that is a DatabaseClass
		Class cls = c.getSuperclass();
		while (cls != null) {
			annotation = cls.getAnnotation(DatabaseClass.class);
			if (annotation == null)
				break;
			processFields(cls);
			cls = cls.getSuperclass();
		}

		// Check if id was found
		if (idField == null)
			throw new IllegalClassStructureException("Class " + c.getName()
					+ " does not have an id field. Check for the required annotation: " + IdField.class.getSimpleName());

		// Check if the reference fields implements the required interface (only for classes, not interfaces)
		// NOT Checked
		// for (Field field : referenceFields) {
		// if (!field.getType().isInterface() &&
		// !field.getType().isAssignableFrom(DatabaseReferenceClass.class))
		// throw new
		// IllegalClassStructureException("The field marked as Reference Field has a type ("
		// + field.getType() + ") that does not implement the required interface "
		//		+ DatabaseReferenceClass.class.getName());
		//		}

	}

	/**
	 * Gets the reference fields.
	 * 
	 * @return the Reference Fields
	 */
	final List<Field> getReferenceFields() {
		return referenceFields;
	}

	/**
	 * Gets the database fields.
	 * 
	 * @return the databaseFields
	 */
	final List<Field> getDatabaseFields() {
		return databaseFields;
	}

	/**
	 * Gets the id field.
	 * 
	 * @return the idField
	 */
	final Field getIdField() {
		return idField;
	}

}
