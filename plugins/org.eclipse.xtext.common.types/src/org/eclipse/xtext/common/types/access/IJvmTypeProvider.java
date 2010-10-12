/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.common.types.access;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.common.types.JvmType;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public interface IJvmTypeProvider {

	/**
	 * Find a type by its fully qualified name, e.g. java.util.Map$Entry, int[], void or 
	 * java.lang.String[][][].
	 */
	JvmType findTypeByName(String name) throws TypeNotFoundException;
	
	/**
	 * Returns the resource set that will contain dynamically created resources.
	 * @return the resource set.
	 */
	ResourceSet getResourceSet();
	
	interface Factory {
		
		IJvmTypeProvider createTypeProvider(ResourceSet resourceSet);
		
		IJvmTypeProvider findTypeProvider(ResourceSet resourceSet);
		
		IJvmTypeProvider createTypeProvider() throws UnsupportedOperationException;
		
	}
	
}