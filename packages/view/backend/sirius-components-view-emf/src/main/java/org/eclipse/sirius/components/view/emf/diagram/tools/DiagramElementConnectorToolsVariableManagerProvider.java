/*******************************************************************************
 * Copyright (c) 2026 Obeo.
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
package org.eclipse.sirius.components.view.emf.diagram.tools;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.sirius.components.collaborative.diagrams.DiagramContext;
import org.eclipse.sirius.components.collaborative.diagrams.DiagramService;
import org.eclipse.sirius.components.collaborative.diagrams.api.DiagramInteractionOperations;
import org.eclipse.sirius.components.collaborative.diagrams.api.IDiagramService;
import org.eclipse.sirius.components.collaborative.diagrams.variables.DiagramVariables;
import org.eclipse.sirius.components.core.api.Environment;
import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.core.api.IObjectSearchService;
import org.eclipse.sirius.components.core.api.variables.CoreVariables;
import org.eclipse.sirius.components.diagrams.Edge;
import org.eclipse.sirius.components.diagrams.Node;
import org.eclipse.sirius.components.representations.IOperationValidator;
import org.eclipse.sirius.components.representations.RepresentationVariables;
import org.eclipse.sirius.components.representations.VariableManager;
import org.eclipse.sirius.components.view.emf.diagram.ViewDiagramConversionData;
import org.eclipse.sirius.components.view.emf.diagram.ViewDiagramDescriptionConverter;
import org.eclipse.sirius.components.view.emf.diagram.tools.api.IDiagramElementConnectorToolsVariableManagerProvider;
import org.eclipse.sirius.components.view.emf.editingcontext.api.IViewEditingContext;
import org.springframework.stereotype.Service;

/**
 * Provides the variable manager used to evaluate connector tool preconditions for a diagram element.
 *
 * @author mcharfadi
 */
@Service
public class DiagramElementConnectorToolsVariableManagerProvider implements IDiagramElementConnectorToolsVariableManagerProvider {

    private final IObjectSearchService objectSearchService;

    private final IOperationValidator operationValidator;

    public DiagramElementConnectorToolsVariableManagerProvider(IObjectSearchService objectSearchService, IOperationValidator operationValidator) {
        this.objectSearchService = Objects.requireNonNull(objectSearchService);
        this.operationValidator = Objects.requireNonNull(operationValidator);
    }

    @Override
    public Optional<VariableManager> getVariableManager(IEditingContext editingContext, DiagramContext diagramContext, Object diagramElement) {
        String targetObjectId = null;
        if (diagramElement instanceof Node node) {
            targetObjectId = node.getTargetObjectId();
        } else if (diagramElement instanceof Edge edge) {
            targetObjectId = edge.getTargetObjectId();
        }

        return Optional.ofNullable(targetObjectId)
                .flatMap(id -> this.objectSearchService.getObject(editingContext, id))
                .map(semanticElement -> this.createVariableManager(editingContext, diagramContext, diagramElement, semanticElement));
    }

    private VariableManager createVariableManager(IEditingContext editingContext, DiagramContext diagramContext, Object diagramElement, Object semanticElement) {
        VariableManager variableManager = new VariableManager();
        variableManager.put(RepresentationVariables.SELF.name(), semanticElement);
        variableManager.put(CoreVariables.EDITING_CONTEXT.name(), editingContext);
        variableManager.put(CoreVariables.ENVIRONMENT.name(), new Environment(Environment.SIRIUS_COMPONENTS));
        variableManager.put(DiagramVariables.DIAGRAM_CONTEXT.name(), diagramContext);
        variableManager.put(IDiagramService.DIAGRAM_SERVICES, new DiagramService(diagramContext));
        variableManager.put(DiagramVariables.EDGE_SOURCE.name(), diagramElement);
        variableManager.put(DiagramVariables.SEMANTIC_EDGE_SOURCE.name(), semanticElement);

        variableManager.put(DiagramVariables.SELECTED_NODE.name(), Optional.ofNullable(diagramElement)
                .filter(Node.class::isInstance)
                .map(Node.class::cast)
                .orElse(null));
        variableManager.put(DiagramVariables.SELECTED_EDGE.name(), Optional.ofNullable(diagramElement)
                .filter(Edge.class::isInstance)
                .map(Edge.class::cast)
                .orElse(null));

        this.getViewDiagramConversionData(editingContext, diagramContext.diagram().getDescriptionId())
                .ifPresent(viewDiagramConversionData -> variableManager.put(ViewDiagramDescriptionConverter.CONVERTED_NODES_VARIABLE, viewDiagramConversionData.convertedNodes()));

        this.operationValidator.validate(DiagramInteractionOperations.EDGE_TOOL, variableManager.getVariables());
        return variableManager;
    }

    private Optional<ViewDiagramConversionData> getViewDiagramConversionData(IEditingContext editingContext, String diagramDescriptionId) {
        return Optional.of(editingContext)
                .filter(IViewEditingContext.class::isInstance)
                .map(IViewEditingContext.class::cast)
                .map(IViewEditingContext::getViewConversionData)
                .map(viewConversionData -> viewConversionData.get(diagramDescriptionId))
                .filter(Objects::nonNull)
                .filter(ViewDiagramConversionData.class::isInstance)
                .map(ViewDiagramConversionData.class::cast);
    }
}
