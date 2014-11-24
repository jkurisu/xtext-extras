/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.generator.formatting2

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap
import com.google.inject.Inject
import com.google.inject.name.Named
import java.util.Collection
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EReference
import org.eclipse.xtend.lib.annotations.Accessors
import org.eclipse.xtend.lib.annotations.FinalFieldsConstructor
import org.eclipse.xtext.Grammar
import org.eclipse.xtext.formatting2.AbstractFormatter2
import org.eclipse.xtext.formatting2.IFormattableDocument
import org.eclipse.xtext.generator.Naming
import org.eclipse.xtext.generator.grammarAccess.GrammarAccessUtil
import org.eclipse.xtext.generator.serializer.JavaEMFFile

import static extension org.eclipse.xtext.GrammarUtil.*
import static extension org.eclipse.xtext.generator.IInheriting.Util.*

/**
 * @author Moritz Eysholdt - Initial contribution and API
 */
@FinalFieldsConstructor class FormatterStubGenerator {
	@Accessors(PUBLIC_GETTER) static class Service {
		@Inject Naming naming
		@Inject @Named("fileHeader") String fileHeader

		def FormatterStubGenerator createGenerator(Grammar grammar) {
			new FormatterStubGenerator(this, grammar)
		}
	}

	val FormatterStubGenerator.Service service
	val Grammar grammar

	def String getStubSimpleName() {
		'''�service.naming.toSimpleName(grammar.name)�Formatter'''
	}

	def String getStubPackageName() {
		'''�service.naming.toPackageName(grammar.name)�.formatting2'''
	}

	def String getStubQualifiedName() {
		'''�stubPackageName�.�stubSimpleName�'''
	}

	def String getStubFileName() {
		'''�service.naming.asPath(getStubQualifiedName)�.xtend'''
	}

	def String getStubSuperClassName() {
		val superGrammar = grammar.nonTerminalsSuperGrammar
		if (superGrammar != null)
			return service.createGenerator(superGrammar).stubQualifiedName
		else
			return AbstractFormatter2.name
	}

	def Multimap<EClass, EReference> getLocalyAssignedContainmentReferences() {
		val result = LinkedHashMultimap.<EClass, EReference>create
		for (assignment : grammar.containedAssignments) {
			val type = assignment.findCurrentType
			if (type instanceof EClass) {
				val feature = type.getEStructuralFeature(assignment.feature)
				if (feature instanceof EReference && (feature as EReference).isContainment) {
					result.put(type, feature as EReference)
				}
			}
		}
		for (action : grammar.containedActions) {
			val featureName = action.feature
			if (featureName != null) {
				val type = action.type.classifier
				if (type instanceof EClass) {
					val feature = type.getEStructuralFeature(featureName)
					if (feature instanceof EReference && (feature as EReference).isContainment) {
						result.put(type, feature as EReference)
					}
				}
			}
		}
		return result
	}

	def String generateStubFileContents() {
		val extension file = new JavaEMFFile(grammar.eResource.resourceSet, stubPackageName);
		file.imported(IFormattableDocument)

		val type2ref = getLocalyAssignedContainmentReferences
		file.body = '''
			class �stubSimpleName� extends �stubSuperClassName.imported� {
				
				@�Inject.imported� extension �GrammarAccessUtil.getGrammarAccessFQName(grammar, service.naming).imported�
				�FOR type : type2ref.keySet�

					�type.generateFormatMethod(file, type2ref.get(type))�
				�ENDFOR�	
			}
		'''
		return file.toString
	}
	
	def String toName(EClass clazz) {
		clazz.name.toLowerCase
	}
		
	def generateFormatMethod(EClass clazz, extension JavaEMFFile file, Collection<EReference> containmentRefs) '''
		def dispatch void format(�clazz.importedGenTypeName� �clazz.toName�, extension IFormattableDocument document) {
			// TODO: format HiddenRegions around keywords, attribtues, cross references, etc. 
			�FOR ref:containmentRefs�
				�IF ref.isMany�
					for (�ref.EReferenceType.importedGenTypeName� �ref.name� : �clazz.toName�.�ref.getAccessor�()) {
						format(�ref.name�, document);
					}
				�ELSE�
					format(�clazz.toName�.�ref.getAccessor�(), document);
				�ENDIF�
			�ENDFOR�
		}
	'''
}
