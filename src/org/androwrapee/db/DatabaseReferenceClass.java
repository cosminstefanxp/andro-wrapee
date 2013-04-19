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
 * The Interface DatabaseReference must be inherited by any class that aims to be used as a
 * reference field for a entry to be stored in the database.
 */
public interface DatabaseReferenceClass {

	/**
	 * Gets the id.
	 * 
	 * @return the id
	 */
	public long getId();

}
