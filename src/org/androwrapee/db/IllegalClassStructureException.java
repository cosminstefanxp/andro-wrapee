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

/**
 * The DatabaseClassStructureException is thrown when the structure of a class that has to be stored
 * in the database is not according to the specifications. Please check {@link ReflectionManager} and
 * {@link DefaultDAO} for specifications.
 */
public class IllegalClassStructureException extends Exception {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 7208320571758498752L;

	public IllegalClassStructureException() {
		super();
	}

	public IllegalClassStructureException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public IllegalClassStructureException(String detailMessage) {
		super(detailMessage);
	}

	public IllegalClassStructureException(Throwable throwable) {
		super(throwable);
	}

}
