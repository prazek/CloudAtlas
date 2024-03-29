/**
 * Copyright (c) 2014, University of Warsaw
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package model;

import core.Model;

import java.io.Serializable;

/**
 * A type of a value that may be stored as an attribute.
 */
public abstract class Type implements Serializable {
	/**
	 * A primary type. This is a characteristic that every type has. It can be extended: for instance a collection may
	 * be parameterized with a type of stored values.
	 */
	public static enum PrimaryType {
		BOOLEAN, CONTACT, DOUBLE, DURATION, INT, LIST, NULL, SET, STRING, TIME,
	}
	
	private final PrimaryType primaryType;
	
	/**
	 * Creates a <code>Type</code> object with a given primary type.
	 * 
	 * @param primaryType a primary type for this type
	 */
	public Type(PrimaryType primaryType) {
		this.primaryType = primaryType;
	}
	
	/**
	 * Returns a primary type of this type.
	 * 
	 * @return a primary type
	 */
	public PrimaryType getPrimaryType() {
		return primaryType;
	}
	
	/**
	 * Indicates whether this type can be implicitly "cast" to given one and vice verse. This is introduced to deal with
	 * null values. In practice, two types are compatible either if they are the same or if one them is a special
	 * "null type".
	 * 
	 * @param type a type to check
	 * @return whether two types are compatible with each other
	 * @see TypePrimitive#NULL
	 * @see ValueNull
	 */
	public boolean isCompatible(Type type) {
		return getPrimaryType() == PrimaryType.NULL || type.getPrimaryType() == PrimaryType.NULL;
	}
	
	/**
	 * Indicates whether this type represents a collection.
	 * 
	 * @return true for collections, false otherwise
	 */
	public boolean isCollection() {
		return false;
	}

	public Model.Type serializeType() {
		if (this instanceof TypeCollection)
			return Model.Type.newBuilder().setCollection(((TypeCollection)this).serialize()).build();
		if (this instanceof TypePrimitive)
			return Model.Type.newBuilder().setPrimitive(((TypePrimitive)this).serialize()).build();
		assert false;
		return null;
	}

	public Model.TypePrimary serializePrimary() {
		switch (getPrimaryType()) {
			case BOOLEAN:
				return Model.TypePrimary.BOOLEAN;
			case CONTACT:
				return Model.TypePrimary.CONTACT;
			case DOUBLE:
				return Model.TypePrimary.DOUBLE;
			case DURATION:
				return Model.TypePrimary.DURATION;
			case INT:
				return Model.TypePrimary.INT;
			case LIST:
				return Model.TypePrimary.LIST;
			case NULL:
				return Model.TypePrimary.NULL;
			case SET:
				return Model.TypePrimary.SET;
			case STRING:
				return Model.TypePrimary.STRING;
			case TIME:
				return Model.TypePrimary.TIME;
		}
		assert false;
		return null;
	}

	public static Type fromProtobuf(Model.Type type) {
		if (type.hasCollection())
			return TypeCollection.fromProtobuf(type.getCollection());
		else
			return TypePrimitive.fromProtobuf(type.getPrimitive());
	}

	public static PrimaryType fromProtobuf(Model.TypePrimary type) {
		switch (type.getNumber()) {
			case Model.TypePrimary.BOOLEAN_VALUE:
				return PrimaryType.BOOLEAN;
			case Model.TypePrimary.CONTACT_VALUE:
				return PrimaryType.CONTACT;
			case Model.TypePrimary.DOUBLE_VALUE:
				return PrimaryType.DOUBLE;
			case Model.TypePrimary.DURATION_VALUE:
				return PrimaryType.DURATION;
			case Model.TypePrimary.INT_VALUE:
				return PrimaryType.INT;
			case Model.TypePrimary.LIST_VALUE:
				return PrimaryType.LIST;
			case Model.TypePrimary.NULL_VALUE:
				return PrimaryType.NULL;
			case Model.TypePrimary.SET_VALUE:
				return PrimaryType.SET;
			case Model.TypePrimary.STRING_VALUE:
				return PrimaryType.STRING;
			case Model.TypePrimary.TIME_VALUE:
				return PrimaryType.TIME;
		}
		assert false;
		return null;
	}
}
