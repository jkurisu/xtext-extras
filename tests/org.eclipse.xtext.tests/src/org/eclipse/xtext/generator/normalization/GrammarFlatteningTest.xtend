/** 
 * Copyright (c) 2015 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.xtext.generator.normalization

import org.eclipse.emf.common.util.URI
import org.eclipse.xtext.Grammar
import org.eclipse.xtext.RuleNames
import org.eclipse.xtext.XtextStandaloneSetup
import org.eclipse.xtext.junit4.AbstractXtextTests
import org.eclipse.xtext.resource.XtextResource
import org.junit.Test

/** 
 * @author Sebastian Zarnekow - Initial contribution and API
 */
class GrammarFlatteningTest extends AbstractXtextTests {
	override void setUp() throws Exception {
		super.setUp()
		with(XtextStandaloneSetup)
	}

	override Grammar getModel(String model) throws Exception {
		var Grammar grammar = super.getModel(model) as Grammar
		var RuleNames ruleNames = RuleNames.getRuleNames(grammar, false)
		var RuleFilter filter = new RuleFilter()
		var Grammar result = FlattenedGrammarProvider.flatten(grammar, ruleNames, filter)
		var XtextResource resource = get(XtextResource)
		resource.getContents().add(result)
		resource.setURI(URI.createURI("synthetic://flattened.xtext"))
		return result
	}

	@Test def void test_01() throws Exception {
		var Grammar flattened = getModel(
			'''
				grammar com.foo.bar with org.eclipse.xtext.common.Terminals
				generate myPack 'http://myURI'
				Rule: name=ID;
			''')
		var String serialized = getSerializer().serialize(flattened)
		assertEquals('''
			grammar com.foo.bar hidden(RULE_WS, RULE_ML_COMMENT, RULE_SL_COMMENT)
			
			ruleRule:
				name=RULE_ID;
			
			terminal RULE_ID:
				"^"? ("a".."z" | "A".."Z" | "_") ("a".."z" | "A".."Z" | "_" | "0".."9")*;
			
			terminal RULE_INT:
				"0".."9"+;
			
			terminal RULE_STRING:
				"\"" ("\\" . | !("\\" | "\""))* "\"" | "\'" ("\\" . | !("\\" | "\'"))* "\'";
			
			terminal RULE_ML_COMMENT:
				"/*"->"*/";
			
			terminal RULE_SL_COMMENT:
				"//" !("\n" | "\r")* ("\r"? "\n")?;
			
			terminal RULE_WS:
				" " | "\t" | "\r" | "\n"+;
			
			terminal RULE_ANY_OTHER:
				.;'''.toString, serialized)
	}
	
	@Test def void test_02() throws Exception {
		var Grammar flattened = getModel(
			'''
				grammar com.foo.bar with org.eclipse.xtext.common.Terminals
				generate myPack 'http://myURI'
				Rule: name=ID;
				terminal ID: super;
			''')
		var String serialized = getSerializer().serialize(flattened)
		assertEquals('''
			grammar com.foo.bar hidden(RULE_WS, RULE_ML_COMMENT, RULE_SL_COMMENT)
			
			ruleRule:
				name=RULE_ID;
			
			terminal RULE_ID:
				SUPER_ID;
			
			terminal fragment SUPER_ID:
				"^"? ("a".."z" | "A".."Z" | "_") ("a".."z" | "A".."Z" | "_" | "0".."9")*;
			
			terminal RULE_INT:
				"0".."9"+;
			
			terminal RULE_STRING:
				"\"" ("\\" . | !("\\" | "\""))* "\"" | "\'" ("\\" . | !("\\" | "\'"))* "\'";
			
			terminal RULE_ML_COMMENT:
				"/*"->"*/";
			
			terminal RULE_SL_COMMENT:
				"//" !("\n" | "\r")* ("\r"? "\n")?;
			
			terminal RULE_WS:
				" " | "\t" | "\r" | "\n"+;
			
			terminal RULE_ANY_OTHER:
				.;'''.toString, serialized)
	}

}