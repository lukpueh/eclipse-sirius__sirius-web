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
package org.eclipse.sirius.components.diagrams.tests.graphql;

import java.util.Map;
import java.util.Objects;

import org.eclipse.sirius.components.graphql.tests.api.GraphQLResult;
import org.eclipse.sirius.components.graphql.tests.api.IGraphQLRequestor;
import org.eclipse.sirius.components.graphql.tests.api.IQueryRunner;
import org.springframework.stereotype.Service;

/**
 * Used to retrieve the connector tools of diagram elements.
 *
 * @author mcharfadi
 */
@Service
public class ConnectorToolsQueryRunner implements IQueryRunner {

    private static final String CONNECTOR_TOOLS_QUERY = """
            query getToolSections($editingContextId: ID!, $diagramId: ID!, $diagramElementId: ID!) {
              viewer {
                editingContext(editingContextId: $editingContextId) {
                  representation(representationId: $diagramId) {
                    description {
                      ... on DiagramDescription {
                        connectorTools(diagramElementId: $diagramElementId) {
                          candidates {
                            sources {
                              id
                            }
                            targets {
                              id
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

    private final IGraphQLRequestor graphQLRequestor;

    public ConnectorToolsQueryRunner(IGraphQLRequestor graphQLRequestor) {
        this.graphQLRequestor = Objects.requireNonNull(graphQLRequestor);
    }

    @Override
    public GraphQLResult run(Map<String, Object> variables) {
        return this.graphQLRequestor.execute(CONNECTOR_TOOLS_QUERY, variables);
    }
}
