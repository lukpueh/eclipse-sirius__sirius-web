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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.sirius.components.collaborative.diagrams.DiagramContext;
import org.eclipse.sirius.components.collaborative.diagrams.api.IDiagramDescriptionService;
import org.eclipse.sirius.components.collaborative.diagrams.api.IDiagramElementConnectorToolsProvider;
import org.eclipse.sirius.components.collaborative.diagrams.dto.SingleClickOnTwoDiagramElementsTool;
import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.diagrams.Edge;
import org.eclipse.sirius.components.diagrams.Node;
import org.eclipse.sirius.components.diagrams.description.DiagramDescription;
import org.eclipse.sirius.components.diagrams.description.IDiagramElementDescription;
import org.eclipse.sirius.components.interpreter.AQLInterpreter;
import org.eclipse.sirius.components.interpreter.Result;
import org.eclipse.sirius.components.interpreter.Status;
import org.eclipse.sirius.components.representations.VariableManager;
import org.eclipse.sirius.components.view.View;
import org.eclipse.sirius.components.view.diagram.DiagramElementDescription;
import org.eclipse.sirius.components.view.diagram.Tool;
import org.eclipse.sirius.components.view.emf.IViewRepresentationDescriptionPredicate;
import org.eclipse.sirius.components.view.emf.api.IViewAQLInterpreterFactory;
import org.eclipse.sirius.components.view.emf.diagram.ToolFinder;
import org.eclipse.sirius.components.view.emf.diagram.api.IViewDiagramDescriptionSearchService;
import org.eclipse.sirius.components.view.emf.diagram.tools.api.IConnectorPaletteVariableManagerProvider;
import org.eclipse.sirius.components.view.emf.diagram.tools.api.IEdgeToolConverter;
import org.springframework.stereotype.Service;

/**
 * Provides the connector tools available from a diagram element described by a View model.
 *
 * @author mcharfadi
 */
@Service
public class DiagramElementConnectorToolsProvider implements IDiagramElementConnectorToolsProvider {

    private final IViewRepresentationDescriptionPredicate viewRepresentationDescriptionPredicate;

    private final IViewDiagramDescriptionSearchService viewDiagramDescriptionSearchService;

    private final IDiagramDescriptionService diagramDescriptionService;

    private final IViewAQLInterpreterFactory aqlInterpreterFactory;

    private final IConnectorPaletteVariableManagerProvider variableManagerProvider;

    private final IEdgeToolConverter edgeToolConverter;

    public DiagramElementConnectorToolsProvider(IViewRepresentationDescriptionPredicate viewRepresentationDescriptionPredicate, IViewDiagramDescriptionSearchService viewDiagramDescriptionSearchService, IDiagramDescriptionService diagramDescriptionService, IViewAQLInterpreterFactory aqlInterpreterFactory, IConnectorPaletteVariableManagerProvider variableManagerProvider, IEdgeToolConverter edgeToolConverter) {
        this.viewRepresentationDescriptionPredicate = Objects.requireNonNull(viewRepresentationDescriptionPredicate);
        this.viewDiagramDescriptionSearchService = Objects.requireNonNull(viewDiagramDescriptionSearchService);
        this.diagramDescriptionService = Objects.requireNonNull(diagramDescriptionService);
        this.aqlInterpreterFactory = Objects.requireNonNull(aqlInterpreterFactory);
        this.variableManagerProvider = Objects.requireNonNull(variableManagerProvider);
        this.edgeToolConverter = Objects.requireNonNull(edgeToolConverter);
    }

    @Override
    public boolean canHandle(IEditingContext editingContext, DiagramContext diagramContext, DiagramDescription diagramDescription, String diagramElementId) {
        return this.viewRepresentationDescriptionPredicate.test(diagramDescription);
    }

    @Override
    public List<SingleClickOnTwoDiagramElementsTool> getConnectorTools(IEditingContext editingContext, DiagramContext diagramContext, DiagramDescription diagramDescription, Object diagramElement) {
        return this.computeConnectorTools(editingContext, diagramContext, diagramDescription, diagramElement);
    }

    private List<SingleClickOnTwoDiagramElementsTool> computeConnectorTools(IEditingContext editingContext, DiagramContext diagramContext, DiagramDescription diagramDescription, Object diagramElement) {
        var optionalDiagramElementDescription = this.findDiagramElementDescription(diagramDescription, diagramElement);
        var optionalViewDiagramElementDescription = this.findViewDiagramElementDescription(editingContext, diagramElement);
        var optionalViewDiagramDescription = this.viewDiagramDescriptionSearchService.findById(editingContext, diagramDescription.getId());
        var optionalVariableManager = this.variableManagerProvider.getVariableManager(editingContext, diagramContext, diagramElement, null);

        if (optionalDiagramElementDescription.isPresent() && optionalViewDiagramElementDescription.isPresent() && optionalViewDiagramDescription.isPresent() && optionalVariableManager.isPresent()) {
            var viewDiagramDescription = optionalViewDiagramDescription.get();
            var interpreter = this.aqlInterpreterFactory.createInterpreter(editingContext, (View) viewDiagramDescription.eContainer());
            var toolFinder = new ToolFinder();
            var diagramElementDescription = optionalDiagramElementDescription.get();
            var variableManager = optionalVariableManager.get();
            return toolFinder.findEdgeTools(optionalViewDiagramElementDescription.get()).stream()
                    .filter(tool -> this.checkPrecondition(tool, variableManager, interpreter))
                    .map(tool -> this.edgeToolConverter.createEdgeTool(interpreter, tool, diagramDescription, diagramElementDescription, variableManager))
                    .filter(SingleClickOnTwoDiagramElementsTool.class::isInstance)
                    .map(SingleClickOnTwoDiagramElementsTool.class::cast)
                    .toList();
        }
        return List.of();
    }

    private Optional<IDiagramElementDescription> findDiagramElementDescription(DiagramDescription diagramDescription, Object diagramElement) {
        Optional<IDiagramElementDescription> optionalDiagramElementDescription = Optional.empty();
        if (diagramElement instanceof Node node) {
            optionalDiagramElementDescription = this.diagramDescriptionService.findNodeDescriptionById(diagramDescription, node.getDescriptionId()).map(IDiagramElementDescription.class::cast);
        } else if (diagramElement instanceof Edge edge) {
            optionalDiagramElementDescription = this.diagramDescriptionService.findEdgeDescriptionById(diagramDescription, edge.getDescriptionId()).map(IDiagramElementDescription.class::cast);
        }
        return optionalDiagramElementDescription;
    }

    private Optional<DiagramElementDescription> findViewDiagramElementDescription(IEditingContext editingContext, Object diagramElement) {
        Optional<DiagramElementDescription> optionalDiagramElementDescription = Optional.empty();
        if (diagramElement instanceof Node node) {
            optionalDiagramElementDescription = this.viewDiagramDescriptionSearchService.findViewNodeDescriptionById(editingContext, node.getDescriptionId()).map(DiagramElementDescription.class::cast);
        } else if (diagramElement instanceof Edge edge) {
            optionalDiagramElementDescription = this.viewDiagramDescriptionSearchService.findViewEdgeDescriptionById(editingContext, edge.getDescriptionId()).map(DiagramElementDescription.class::cast);
        }
        return optionalDiagramElementDescription;
    }

    private boolean checkPrecondition(Tool tool, VariableManager variableManager, AQLInterpreter interpreter) {
        String precondition = tool.getPreconditionExpression();
        if (precondition != null && !precondition.isBlank()) {
            Result result = interpreter.evaluateExpression(variableManager.getVariables(), precondition);
            return result.getStatus().compareTo(Status.WARNING) <= 0 && result.asBoolean().orElse(Boolean.FALSE);
        }
        return true;
    }
}
