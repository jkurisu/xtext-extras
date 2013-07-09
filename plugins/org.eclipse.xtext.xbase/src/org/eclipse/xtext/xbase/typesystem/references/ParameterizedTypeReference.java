/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xbase.typesystem.references;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.common.types.JvmArrayType;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmGenericType;
import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmPrimitiveType;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeConstraint;
import org.eclipse.xtext.common.types.JvmTypeParameter;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmUpperBound;
import org.eclipse.xtext.common.types.util.Primitives;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference.IdentifierFunction;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference.JavaIdentifierFunction;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference.SimpleNameFunction;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference.UniqueIdentifierFunction;
import org.eclipse.xtext.xbase.typesystem.util.IVisibilityHelper;
import org.eclipse.xtext.xbase.typesystem.util.TypeParameterSubstitutor;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
@NonNullByDefault
public class ParameterizedTypeReference extends LightweightTypeReference {

	private List<LightweightTypeReference> typeArguments;
	private JvmType type;
	protected boolean resolved;
	
	public ParameterizedTypeReference(ITypeReferenceOwner owner, JvmType type) {
		super(owner);
		if (type == null) {
			throw new NullPointerException("type may not be null");
		}
		if (type instanceof JvmArrayType) {
			throw new IllegalArgumentException("type may not be an array type");
		}
		this.type = type;
		// TODO check against owner or specialized representation of the owner
		this.resolved = !(type instanceof JvmTypeParameter);
	}
	
	@Override
	public JvmTypeReference toTypeReference() {
		JvmParameterizedTypeReference result = getTypesFactory().createJvmParameterizedTypeReference();
		result.setType(type);
		if (typeArguments != null) {
			for(LightweightTypeReference typeArgument: typeArguments) {
				result.getArguments().add(typeArgument.toTypeReference());
			}
		}
		return result;
	}
	
	protected boolean isTypeVisible(IVisibilityHelper visibilityHelper) {
		return !(type instanceof JvmDeclaredType) || visibilityHelper.isVisible((JvmDeclaredType)type);
	}
	
	@Override
	public boolean isVisible(IVisibilityHelper visibilityHelper) {
		if (isTypeVisible(visibilityHelper)) {
			if (typeArguments != null) {
				for(LightweightTypeReference typeArgument: typeArguments) {
					if (!typeArgument.isVisible(visibilityHelper)) {
						return false;
					}
				}
			}
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public JvmTypeReference toJavaCompliantTypeReference(IVisibilityHelper visibilityHelper) {
		if (isTypeVisible(visibilityHelper)) {
			JvmParameterizedTypeReference result = getTypesFactory().createJvmParameterizedTypeReference();
			result.setType(type);
			if (typeArguments != null) {
				for(LightweightTypeReference typeArgument: typeArguments) {
					result.getArguments().add(typeArgument.toJavaCompliantTypeReference());
				}
			}
			return result;
		} else {
			return toJavaCompliantTypeReference(getSuperTypes(), visibilityHelper);
		}
	}
	
	@Override
	public JvmType getType() {
		return type;
	}
	
	@Override
	protected boolean isRawType(Set<JvmType> seenTypes) {
		if (type instanceof JvmGenericType) {
			if (!((JvmGenericType) type).getTypeParameters().isEmpty() && expose(typeArguments).isEmpty())
				return true;
		} 
		else if (type instanceof JvmTypeParameter && seenTypes.add(type)) {
			JvmTypeParameter typeParameter = (JvmTypeParameter) type;
			List<JvmTypeConstraint> constraints = typeParameter.getConstraints();
			for(JvmTypeConstraint constraint: constraints) {
				JvmTypeReference typeReference = constraint.getTypeReference();
				LightweightTypeReference lightweightConstraint = new OwnedConverter(getOwner()).toLightweightReference(typeReference);
				if (lightweightConstraint.isRawType(seenTypes)) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean isOwnedBy(ITypeReferenceOwner owner) {
		if (super.isOwnedBy(owner)) {
			if (typeArguments != null) {
				for(LightweightTypeReference typeArgument: typeArguments) {
					if (!typeArgument.isOwnedBy(owner))
						return false;
				}
			}
			return true;
		}
		return false;
	}
	
	@Override
	public LightweightTypeReference getWrapperTypeIfPrimitive() {
		if (type instanceof JvmPrimitiveType) {
			Primitives primitives = getOwner().getServices().getPrimitives();
			JvmType wrapperType = primitives.getWrapperType((JvmPrimitiveType) type);
			return new ParameterizedTypeReference(getOwner(), wrapperType);
		}
		return this;
	}
	
	@Override
	public LightweightTypeReference getPrimitiveIfWrapperType() {
		if (type instanceof JvmDeclaredType) {
			Primitives primitives = getOwner().getServices().getPrimitives();
			JvmType primitiveType = primitives.getPrimitiveTypeIfWrapper((JvmDeclaredType) type);
			if (primitiveType != null) {
				return new ParameterizedTypeReference(getOwner(), primitiveType);
			}
		}
		return this;
	}
	
	@Override
	public boolean isPrimitive() {
		return type instanceof JvmPrimitiveType;
	}
	
	@Override
	protected boolean isInterfaceType() {
		if (type instanceof JvmGenericType) {
			return ((JvmGenericType) type).isInterface();
		}
		return false;
	}
	
	@Override
	public boolean isWrapper() {
		if (type instanceof JvmDeclaredType || type instanceof JvmTypeParameter) {
			Primitives primitives = getOwner().getServices().getPrimitives();
			boolean result = primitives.isWrapperType(type);
			return result;
		}
		return false;
	}
	
	@Override
	protected List<LightweightTypeReference> getSuperTypes(TypeParameterSubstitutor<?> substitutor) {
		// TODO should this be a service?
		if (type instanceof JvmDeclaredType) {
			List<JvmTypeReference> superTypes = ((JvmDeclaredType) type).getSuperTypes();
			if (!superTypes.isEmpty()) {
				if (!isRawType()) {
					OwnedConverter converter = new OwnedConverter(getOwner());
					List<LightweightTypeReference> result = Lists.newArrayListWithCapacity(superTypes.size());
					for(JvmTypeReference superType: superTypes) {
						LightweightTypeReference lightweightSuperType = converter.toLightweightReference(superType);
						if (!lightweightSuperType.isType(Object.class) || superTypes.size() == 1)
							result.add(substitutor.substitute(lightweightSuperType));
					}
					return result;
				} else {
					OwnedConverter converter = new OwnedConverter(getOwner());
					List<LightweightTypeReference> result = Lists.newArrayListWithCapacity(superTypes.size());
					for(JvmTypeReference superType: superTypes) {
						LightweightTypeReference lightweightSuperType = converter.toLightweightReference(superType);
						if (!lightweightSuperType.isType(Object.class) || superTypes.size() == 1)
							result.add(substitutor.substitute(lightweightSuperType).getRawTypeReference());
					}
					return result;
				}
			}
		} else if (type instanceof JvmTypeParameter) {
			List<JvmTypeConstraint> constraints = ((JvmTypeParameter) type).getConstraints();
			if (!constraints.isEmpty()) {
				List<LightweightTypeReference> result = Lists.newArrayListWithCapacity(constraints.size());
				OwnedConverter converter = new OwnedConverter(getOwner());
				for(JvmTypeConstraint constraint: constraints) {
					if (constraint instanceof JvmUpperBound && constraint.getTypeReference() != null) {
						LightweightTypeReference upperBound = converter.toLightweightReference(constraint.getTypeReference());
						result.add(substitutor.substitute(upperBound));
					}
				}
				return result;
			}
		}
		return Collections.emptyList();
	}
	
	@Override
	@Nullable
	public LightweightTypeReference getSuperType(JvmType rawType) {
		if (rawType == type)
			return this;
		JvmTypeReference superType = getSuperType(rawType, type, Sets.<JvmType>newHashSetWithExpectedSize(3));
		if (superType != null) {
			if (superType instanceof JvmParameterizedTypeReference) {
				if (((JvmParameterizedTypeReference) superType).getArguments().isEmpty()) {
					return new ParameterizedTypeReference(getOwner(), rawType);
				}
			}
			JvmParameterizedTypeReference plainSuperType = getServices().getTypeReferences().createTypeRef(rawType);
			OwnedConverter converter = new OwnedConverter(getOwner());
			LightweightTypeReference unresolved = converter.toLightweightReference(plainSuperType);
			TypeParameterSubstitutor<?> substitutor = createSubstitutor();
			LightweightTypeReference result = substitutor.substitute(unresolved);
			if (isRawType()) {
				result = result.getRawTypeReference();
			}
			return result;
		}
		return null;
	}

	@Nullable
	protected JvmTypeReference getSuperType(JvmType rawType, JvmType thisType, Set<JvmType> seenTypes) {
		if (thisType == rawType || !seenTypes.add(thisType)) {
			return null;
		}
		if (thisType instanceof JvmGenericType && rawType instanceof JvmGenericType) {
			if (!"java.lang.Object".equals(rawType.getIdentifier())) {
				if (((JvmGenericType) thisType).isInterface() && !((JvmGenericType) rawType).isInterface()) {
					return null;
				}
			} else {
				return getServices().getTypeReferences().createTypeRef(rawType);
			}
		}
		if (thisType instanceof JvmDeclaredType) {
			List<JvmTypeReference> superTypes = ((JvmDeclaredType) thisType).getSuperTypes();
			for(JvmTypeReference superType: superTypes) {
				if (superType.getType() == rawType)
					return superType;
				JvmTypeReference result = getSuperType(rawType, superType.getType(), seenTypes);
				if (result != null)
					return result;
			}
		} else if (thisType instanceof JvmTypeParameter) {
			List<JvmTypeConstraint> constraints = ((JvmTypeParameter) thisType).getConstraints();
			for(JvmTypeConstraint constraint: constraints) {
				if (constraint instanceof JvmUpperBound && constraint.getTypeReference() != null) {
					JvmTypeReference superType = constraint.getTypeReference();
					if (superType.getType() == rawType) {
						return superType;
					}
					JvmTypeReference result = getSuperType(rawType, superType.getType(), seenTypes);
					if (result != null)
						return result;
				}
			}
		} else if (thisType instanceof JvmArrayType) {
			String identifier = rawType.getIdentifier();
			if (Cloneable.class.getCanonicalName().equals(identifier)
					|| Serializable.class.getCanonicalName().equals(identifier)
					|| Object.class.getCanonicalName().equals(identifier)) {
				return getServices().getTypeReferences().createTypeRef(rawType);
			}
		}
		return null;
	}
	
	@Override
	public List<LightweightTypeReference> getTypeArguments() {
		return expose(typeArguments);
	}

	@Override
	protected ParameterizedTypeReference doCopyInto(ITypeReferenceOwner owner) {
		ParameterizedTypeReference result = new ParameterizedTypeReference(owner, type);
		copyTypeArguments(result, owner);
		return result;
	}

	protected void copyTypeArguments(ParameterizedTypeReference result, ITypeReferenceOwner owner) {
		if (typeArguments != null && !typeArguments.isEmpty()) {
			for(LightweightTypeReference typeArgument: typeArguments) {
				result.addTypeArgument(typeArgument.copyInto(owner));
			}
		}
	}
	
	@Override
	public boolean isResolved() {
		return resolved;
	}

	public void addTypeArgument(LightweightTypeReference argument) {
		if (argument == null) {
			throw new NullPointerException("argument may not be null");
		}
		if (!argument.isOwnedBy(getOwner())) {
			throw new IllegalArgumentException("argument is not valid in current context");
		}
		if (typeArguments == null)
			typeArguments = Lists.newArrayListWithCapacity(2);
		typeArguments.add(argument);
		resolved = resolved && argument.isResolved();
	}
	
	@Override
	public String getSimpleName() {
		return getAsString(type.getSimpleName(), SimpleNameFunction.INSTANCE);
	}
	
	@Override
	public String getIdentifier() {
		return getAsString(type.getIdentifier(), IdentifierFunction.INSTANCE);
	}
	
	@Override
	public String getUniqueIdentifier() {
		return getAsString(getUniqueIdentifier(type), UniqueIdentifierFunction.INSTANCE);
	}

	@Override
	public String getJavaIdentifier() {
		return getAsString(type.getIdentifier(), JavaIdentifierFunction.INSTANCE);
	}
	
	protected String getAsString(String type, Function<LightweightTypeReference, String> format) {
		if (typeArguments != null)
			return type + "<" + Joiner.on(", ").join(Iterables.transform(typeArguments, format)) + ">";
		return type;
	}

	@Override
	public boolean isType(Class<?> clazz) {
		if (type != null) {
			return clazz.getCanonicalName().equals(type.getIdentifier());
		}
		return false;
	}
	
	@Override
	public void accept(TypeReferenceVisitor visitor) {
		visitor.doVisitParameterizedTypeReference(this);
	}
	
	@Override
	public <Param> void accept(TypeReferenceVisitorWithParameter<Param> visitor, Param param) {
		visitor.doVisitParameterizedTypeReference(this, param);
	}
	
	@Override
	@Nullable
	public <Result> Result accept(TypeReferenceVisitorWithResult<Result> visitor) {
		return visitor.doVisitParameterizedTypeReference(this);
	}
	
	@Override
	@Nullable
	public <Param, Result> Result accept(TypeReferenceVisitorWithParameterAndResult<Param, Result> visitor, Param param) {
		return visitor.doVisitParameterizedTypeReference(this, param);
	}

	@Override
	public FunctionTypeKind getFunctionTypeKind() {
		FunctionTypes functionTypes = getServices().getFunctionTypes();
		return functionTypes.getFunctionTypeKind(this);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see FunctionTypes#getAsFunctionTypeReference(ParameterizedTypeReference)
	 */
	@Override
	@Nullable
	public FunctionTypeReference getAsFunctionTypeReference() {
		FunctionTypes functionTypes = getServices().getFunctionTypes();
		return functionTypes.getAsFunctionTypeReference(this);
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * @see FunctionTypes#tryConvertToFunctionTypeReference(ParameterizedTypeReference, boolean)
	 */
	@Override
	@Nullable
	public FunctionTypeReference tryConvertToFunctionTypeReference(boolean rawType) {
		FunctionTypes functionTypes = getServices().getFunctionTypes();
		return functionTypes.tryConvertToFunctionTypeReference(this, rawType);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ArrayTypes#tryConvertToArray(ParameterizedTypeReference)
	 */
	@Override
	@Nullable
	public ArrayTypeReference tryConvertToArray() {
		ArrayTypes arrayTypes = getServices().getArrayTypes();
		return arrayTypes.tryConvertToArray(this);
	}
	
	@Override
	@Nullable
	public LightweightTypeReference tryConvertToListType() {
		if (isAssignableFrom(List.class))
			return this;
		return super.tryConvertToListType();
	}
	
	/**
	 * Returns a projection of this type to the instance level. That is, type arguments will 
	 * be replaced by their invariant bounds.
	 * 
	 * The instance projection of <code>ArrayList&lt;? extends Iterable&lt;? extends String&gt;&gt;</code>
	 * is <code>ArrayList&lt;Iterable&lt;? extends String&gt;&gt;</code> since it is possible to create instances
	 * of <code>ArrayList&lt;Iterable&lt;? extends String&gt;&gt;</code>.
	 */
	public ParameterizedTypeReference toInstanceTypeReference() {
		ParameterizedTypeReference result = new ParameterizedTypeReference(getOwner(), getType());
		for(LightweightTypeReference typeArgument: getTypeArguments()) {
			result.addTypeArgument(typeArgument.getInvariantBoundSubstitute());
		}
		return result;
	}
}