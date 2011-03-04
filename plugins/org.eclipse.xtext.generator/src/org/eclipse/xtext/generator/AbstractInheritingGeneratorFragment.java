/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.generator;

import org.eclipse.xtext.Grammar;

/**
 * A generator fragment that generates code which inherits form the generated code
 * of the super language.
 * 
 * @author Jan Koehnlein - Initial contribution and API
 */
public class AbstractInheritingGeneratorFragment extends AbstractGeneratorFragment {

	private boolean isInheritImplementation = true;

	public boolean isInheritImplementation() {
		return isInheritImplementation;
	}

	public void setInheritImplementation(boolean isInheritImplementation) {
		this.isInheritImplementation = isInheritImplementation;
	}

	public String getSuperClassName(String superClassName, String defaultName) {
		if (isInheritImplementation && isClassExists(superClassName))
			return superClassName;
		else
			return defaultName;
	}

	protected boolean isClassExists(String className) {
		try {
			Class.forName(className);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	protected Grammar getSuperGrammar(Grammar grammar) {
		return grammar.getUsedGrammars().isEmpty() ? null : grammar.getUsedGrammars().get(0);
	}
}
