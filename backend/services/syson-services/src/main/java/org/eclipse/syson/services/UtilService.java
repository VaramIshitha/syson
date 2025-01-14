/*******************************************************************************
 * Copyright (c) 2023, 2024 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.syson.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.antlr.v4.runtime.atn.Transition;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.syson.sysml.AcceptActionUsage;
import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.Definition;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.Membership;
import org.eclipse.syson.sysml.Namespace;
import org.eclipse.syson.sysml.Package;
import org.eclipse.syson.sysml.PartUsage;
import org.eclipse.syson.sysml.PortUsage;
import org.eclipse.syson.sysml.Relationship;
import org.eclipse.syson.sysml.StateDefinition;
import org.eclipse.syson.sysml.StateUsage;
import org.eclipse.syson.sysml.TransitionFeatureKind;
import org.eclipse.syson.sysml.TransitionFeatureMembership;
import org.eclipse.syson.sysml.TransitionUsage;
import org.eclipse.syson.sysml.Type;
import org.eclipse.syson.sysml.Usage;
import org.eclipse.syson.sysml.helper.NameHelper;
import org.eclipse.syson.util.SysMLMetamodelHelper;
import org.eclipse.syson.util.SysONEContentAdapter;

/**
 * Miscellaneous Java services used by the SysON views.
 *
 * @author arichard
 */
public class UtilService {

    /**
     * Return the {@link Package} containing the given {@link EObject}.
     *
     * @param element
     *            the {@link EObject} element.
     * @return the {@link Package} containing the given {@link EObject}.
     */
    public org.eclipse.syson.sysml.Package getContainerPackage(EObject element) {
        org.eclipse.syson.sysml.Package pack = null;
        if (element instanceof org.eclipse.syson.sysml.Package pkg) {
            pack = pkg;
        } else if (element != null) {
            return this.getContainerPackage(element.eContainer());
        }
        return pack;
    }

    /**
     * Return the {@link PartUsage} containing the given {@link PortUsage}.
     *
     * @param element
     *            the portUsage {@link PortUsage}.
     * @return the {@link PartUsage} containing the given {@link PortUsage} if found, <code>null</code> otherwise.
     */
    public PartUsage getContainerPart(PortUsage portUsage) {
        PartUsage containerPart = null;
        Usage owningUsage = portUsage.getOwningUsage();
        if (owningUsage instanceof PartUsage partUsage) {
            containerPart = partUsage;
        }
        return containerPart;
    }

    /**
     * When an accept action usage is directly or indirectly a composite feature of a part definition or usage, then the
     * default for the receiver (via) of the accept action usage is the containing part, not the accept action itself.
     * This is known as the default accepting context.
     *
     * @param aau
     *            the acceptActionUsage {@link AcceptActionUsage}.
     * @return the first container that is a {@link Definition} or a {@link Usage} for the given
     *         {@link AcceptActionUsage} if found, <code>null</code> otherwise.
     */
    public Type getReceiverContainerDefinitionOrUsage(AcceptActionUsage aau) {
        EObject container = aau.eContainer();
        while (container != null) {
            if (container instanceof Definition || container instanceof Usage) {
                break;
            }
            container = container.eContainer();
        }
        if (container instanceof Definition || container instanceof Usage) {
            return (Type) container;
        }
        return null;
    }

    /**
     * Get all reachable elements of a type in the {@link ResourceSet} of given {@link EObject}.
     *
     * @param eObject
     *            the {@link EObject} stored in a {@link ResourceSet}
     * @param type
     *            the search typed (either simple or qualified named of the EClass ("Package" vs "sysml::Package")
     * @return a list of reachable object
     */
    public List<EObject> getAllReachable(EObject eObject, String type) {
        EClass eClass = SysMLMetamodelHelper.toEClass(type);
        return this.getAllReachable(eObject, eClass);
    }

    /**
     * Get all reachable elements of the type given by the {@link EClass} in the {@link ResourceSet} of the given
     * {@link EObject}.
     *
     * @param eObject
     *            the {@link EObject} stored in a {@link ResourceSet}
     * @param eClass
     *            the searched {@link EClass}
     * @return a list of reachable object
     */
    public List<EObject> getAllReachable(EObject eObject, EClass eClass) {
        List<EObject> allReachable = null;
        Adapter adapter = EcoreUtil.getAdapter(eObject.eAdapters(), SysONEContentAdapter.class);
        if (adapter instanceof SysONEContentAdapter cacheAdapter) {
            allReachable = cacheAdapter.getCache().get(eClass);
        }
        if (allReachable == null) {
            allReachable = List.of();
        }
        return allReachable;
    }

    /**
     * Find an {@link Element} that match the given name in the ResourceSet of the given element.
     *
     * @param object
     *            the object for which to find a corresponding type.
     * @param elementName
     *            the element name to match.
     * @return the found element or <code>null</code>.
     */
    public <T extends Element> T findByName(EObject object, String elementName) {
        T result = null;
        Namespace namespace = null;
        if (object instanceof Element element) {
            namespace = element.getOwningNamespace();
        } else if (object instanceof Relationship relationship && relationship.getOwner() != null) {
            namespace = relationship.getOwner().getOwningNamespace();
        }
        if (namespace != null) {
            var membership = namespace.resolve(elementName);
            if (membership != null) {
                result = (T) membership.getMemberElement();
            }
        }
        return result;
    }

    /**
     * Find an {@link Element} that match the given name and type in the ResourceSet of the given element.
     *
     * @param object
     *            the object for which to find a corresponding type.
     * @param elementName
     *            the element name to match.
     * @param elementType
     *            the type to match.
     * @return the found element or <code>null</code>.
     */
    public <T extends Element> T findByNameAndType(EObject object, String elementName, Class<T> elementType) {
        final T result = this.findByNameAndType(this.getAllRootsInResourceSet(object), elementName, elementType);
        return result;
    }

    /**
     * Iterate over the given {@link Collection} of root elements to find a element with the given name and type.
     *
     * @param roots
     *            the elements to inspect.
     * @param elementName
     *            the name to match.
     * @param elementType
     *            the type to match.
     * @return the found element or <code>null</code>.
     */
    public <T extends Element> T findByNameAndType(Collection<EObject> roots, String elementName, Class<T> elementType) {
        String[] splitElementName = elementName.split("::");
        List<String> qualifiedName = Arrays.asList(splitElementName);
        for (final EObject root : roots) {
            final T result;
            if (qualifiedName.size() > 1) {
                result = this.findInRootByQualifiedNameAndTypeFrom(root, qualifiedName, elementType);
            } else {
                result = this.findByNameAndTypeFrom(root, elementName, elementType);
            }
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Iterate over the children of the given root {@link EObject} to find an {@link Element} with the given qualified
     * name and type.
     *
     * @param root
     *            the root object to iterate.
     * @param qualifiedName
     *            the qualified name to match.
     * @param elementType
     *            the type to match.
     * @return the found element or <code>null</code>.
     */
    private <T extends Element> T findInRootByQualifiedNameAndTypeFrom(EObject root, List<String> qualifiedName, Class<T> elementType) {
        T element = null;
        if (root instanceof Namespace namespace && namespace.eContainer() == null && namespace.getName() == null) {
            // Ignore top-level namespaces with no name, they aren't part of the qualified name
            element = this.findByQualifiedNameAndTypeFrom(namespace, qualifiedName, elementType);
        } else if (root instanceof Element rootElt && this.nameMatches(rootElt, qualifiedName.get(0))) {
            element = this.findByQualifiedNameAndTypeFrom(rootElt, qualifiedName.subList(1, qualifiedName.size()), elementType);
        }
        return element;
    }

    /**
     * Iterate over the children of the given parent {@link EObject} to find an {@link Element} with the given qualified
     * name and type.
     *
     * @param parent
     *            the parent object to iterate.
     * @param qualifiedName
     *            the qualified name to match.
     * @param elementType
     *            the type to match.
     * @return the found element or <code>null</code>.
     */
    private <T extends Element> T findByQualifiedNameAndTypeFrom(Element parent, List<String> qualifiedName, Class<T> elementType) {
        T element = null;

        Optional<Element> child = parent.getOwnedElement().stream()
                .filter(Element.class::isInstance)
                .map(Element.class::cast)
                .filter(elt -> this.nameMatches(elt, qualifiedName.get(0)))
                .findFirst();
        if (child.isPresent() && qualifiedName.size() > 1) {
            element = this.findByQualifiedNameAndTypeFrom(child.get(), qualifiedName.subList(1, qualifiedName.size()), elementType);
        } else if (child.isPresent() && elementType.isInstance(child.get())) {
            element = elementType.cast(child.get());
        }
        return element;
    }

    /**
     * Iterate over the children of the given root {@link EObject} to find an {@link Element} with the given name and
     * type.
     *
     * @param root
     *            the root object to iterate.
     * @param elementName
     *            the name to match.
     * @param elementType
     *            the type to match.
     * @return the found element or <code>null</code>.
     */
    private <T extends Element> T findByNameAndTypeFrom(EObject root, String elementName, Class<T> elementType) {
        T element = null;

        if (elementType.isInstance(root) && this.nameMatches(elementType.cast(root), elementName)) {
            return elementType.cast(root);
        }

        TreeIterator<EObject> eAllContents = root.eAllContents();
        while (eAllContents.hasNext()) {
            EObject obj = eAllContents.next();
            if (elementType.isInstance(obj) && this.nameMatches(elementType.cast(obj), elementName)) {
                element = elementType.cast(obj);
                break;
            }
        }

        return element;
    }

    /**
     * Retrieves all the root elements of the resource in the resource set of the given context object.
     *
     * @param context
     *            the context object on which to execute this service.
     * @return a {@link Collection} of all the root element of the current resource set.
     */
    private Collection<EObject> getAllRootsInResourceSet(EObject context) {
        final Resource res = context.eResource();
        if (res != null && res.getResourceSet() != null) {
            final Collection<EObject> roots = new ArrayList<>();
            for (final Resource childRes : res.getResourceSet().getResources()) {
                roots.addAll(childRes.getContents());
            }
            return roots;
        }
        return Collections.emptyList();
    }

    /**
     * Check if the given element's name match the given String.
     *
     * @param element
     *            the {@link Element} to check.
     * @param name
     *            the name to match.
     * @return <code>true</code> if the name match, <code>false</code> otherwise.
     */
    private boolean nameMatches(Element element, String name) {
        boolean matches = false;
        if (element != null && name != null) {
            String elementName = element.getName();
            if (elementName != null) {
                matches = elementName.strip().equals(name.strip());
                if (!matches && name.startsWith("'") && name.endsWith("'")) {
                    // We give the option to quote names, but the quotes aren't part of the model.
                    elementName = "'" + elementName + "'";
                    matches = elementName.strip().equals(name.strip());
                }
            }
        }
        return matches;
    }

    /**
     * Checks whether {@code element} is a Parallel state. This method allows an {@link Element} as a parameter but is
     * intended to be called either with a {@link StateUsage} or a {@link StateDefinition}. Will return false in the
     * other cases.
     *
     * @param element
     *            The element to check
     */
    public boolean isParallelState(Element element) {
        return (element instanceof StateUsage su && su.isIsParallel()) || (element instanceof StateDefinition sd && sd.isIsParallel());
    }

    /**
     * Check if the given element name is a qualified name.
     *
     * @param elementName
     *            the given element name.
     * @return <code>true</code> if the given element name is a qualified name, <code>false</code> otherwise.
     */
    public boolean isQualifiedName(String elementName) {
        List<String> segments = NameHelper.parseQualifiedName(elementName);
        return segments.size() > 1;
    }

    /**
     * Remove {@link TransitionFeatureMembership} elements of specific kind from the provided {@link Transition}.
     *
     * @param transition
     *            The transition to modify
     * @param kind
     *            The {@link TransitionFeatureKind} used to identify {@link TransitionFeatureMembership} to remove
     */
    public void removeTransitionFeaturesOfSpecificKind(TransitionUsage transition, TransitionFeatureKind kind) {
        List<TransitionFeatureMembership> elementsToDelete = transition.getOwnedFeatureMembership().stream()
                .filter(TransitionFeatureMembership.class::isInstance)
                .map(TransitionFeatureMembership.class::cast)
                .filter(tfm -> tfm.getKind().equals(kind))
                .toList();
        EcoreUtil.removeAll(elementsToDelete);
    }

    /**
     * Retrieve the start action defined inside the standard library <code>Actions</code>.
     *
     * @param eObject
     *            an object to access to the library resources.
     *
     * @return the standard start ActionUsage defined in the <code>Actions</code> library.
     */
    public ActionUsage retrieveStandardStartAction(Element eObject) {
        return this.findByName(eObject, "Actions::Action::start");
    }

    /**
     * Retrieve the done action defined inside the standard library <code>Actions</code>.
     *
     * @param eObject
     *            an object to access to the library resources.
     *
     * @return the standard done ActionUsage defined in the <code>Actions</code> library.
     */
    public ActionUsage retrieveStandardDoneAction(Element eObject) {
        return this.findByName(eObject, "Actions::Action::done");
    }

    /**
     * Check if the given element is actually the standard start action defined by <code>Actions::Action::start</code>.
     *
     * @param element
     *            an element that could be the standard start action.
     * @return <code>true</code> if the given element is the standard start action and <code>false</code> otherwise.
     */
    public boolean isStandardStartAction(Element element) {
        var elt = element;
        if (element instanceof Membership membership) {
            elt = membership.getMemberElement();
        }
        if (elt instanceof ActionUsage au) {
            return "9a0d2905-0f9c-5bb4-af74-9780d6db1817".equals(au.getElementId());
        }
        return false;
    }

    /**
     * Check if the given element is actually the standard done action defined by <code>Actions::Action::done</code>.
     *
     * @param element
     *            an element that could be the standard done action.
     * @return <code>true</code> if the given element is the standard done action and <code>false</code> otherwise.
     */
    public boolean isStandardDoneAction(Element element) {
        var elt = element;
        if (element instanceof Membership membership) {
            elt = membership.getMemberElement();
        }
        if (elt instanceof ActionUsage au) {
            return "0cdc3cd3-b06c-5c32-beda-0cf4ba164a64".equals(au.getElementId());
        }
        return false;
    }
}
