/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.tinkergraph.process.traversal.dsl.graph;

import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.process.traversal.Step;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalEngine;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategies;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategy;
import org.apache.tinkerpop.gremlin.process.traversal.engine.ComputerTraversalEngine;
import org.apache.tinkerpop.gremlin.process.traversal.engine.StandardTraversalEngine;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.AddVertexStartStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.GraphStep;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class VariableGraphTraversalSource  implements TraversalSource {

    public static Builder standard() {
        return VariableGraphTraversalSource.build().engine(StandardTraversalEngine.build());
    }

    public static Builder computer() {
        return VariableGraphTraversalSource.build().engine(ComputerTraversalEngine.build());
    }

    public static Builder computer(final Class<? extends GraphComputer> graphComputerClass) {
        return VariableGraphTraversalSource.build().engine(ComputerTraversalEngine.build().computer(graphComputerClass));
    }

    ////

    private final transient Graph graph;
    private final TraversalEngine.Builder engine;
    private final TraversalStrategies strategies;
    private final List<TraversalStrategy> withStrategies;
    private final List<Class<? extends TraversalStrategy>> withoutStrategies;

    private VariableGraphTraversalSource(final Graph graph, final TraversalEngine.Builder engine, final List<TraversalStrategy> withStrategies, final List<Class<? extends TraversalStrategy>> withoutStrategies) {
        this.graph = graph;
        this.engine = engine;
        this.withStrategies = withStrategies;
        this.withoutStrategies = withoutStrategies;
        final TraversalStrategies tempStrategies = TraversalStrategies.GlobalCache.getStrategies(this.graph.getClass());
        this.strategies = withStrategies.isEmpty() && withoutStrategies.isEmpty() ?
                tempStrategies :
                tempStrategies.clone()
                        .addStrategies(withStrategies.toArray(new TraversalStrategy[withStrategies.size()]))
                        .removeStrategies(withoutStrategies.toArray(new Class[withoutStrategies.size()]));
    }

    public VariableGraphTraversal<Vertex, Vertex> addV(final Object... keyValues) {
        final VariableGraphTraversal.Admin<Vertex, Vertex> traversal = new DefaultVariableGraphTraversal<>(this.graph);
        traversal.setEngine(this.engine.create(this.graph));
        traversal.setStrategies(this.strategies);
        return traversal.addStep(new AddVertexStartStep(traversal, keyValues));
    }

    public VariableGraphTraversal<Vertex, Vertex> V(final Object... vertexIds) {
        final VariableGraphTraversal.Admin<Vertex, Vertex> traversal = new DefaultVariableGraphTraversal<>(this.graph);
        traversal.setEngine(this.engine.create(this.graph));
        traversal.setStrategies(this.strategies);

        // track the variables - in the future if there are multiple vars then interpret them as such
        final Step s = new GraphStep<>(traversal, Vertex.class, vertexIds);
        final List<TraversalVariablePosition> variablePositions = new ArrayList<>();
        variablePositions.add(new TraversalVariablePosition((TraversalVariable) vertexIds[0], 0));
        ((DefaultVariableGraphTraversal) traversal).getStepVariables().put(s, variablePositions);

        return traversal.addStep(s);
    }

    public VariableGraphTraversal<Edge, Edge> E(final Object... edgesIds) {
        final VariableGraphTraversal.Admin<Edge, Edge> traversal = new DefaultVariableGraphTraversal<>(this.graph);
        traversal.setEngine(this.engine.create(this.graph));
        traversal.setStrategies(this.strategies);
        return traversal.addStep(new GraphStep<>(traversal, Edge.class, edgesIds));
    }

    public Transaction tx() {
        return this.graph.tx();
    }

    public static Builder build() {
        return new Builder();
    }


    @Override
    public Optional<GraphComputer> getGraphComputer() {
        return this.engine.create(this.graph).getGraphComputer();
    }

    @Override
    public Optional<Graph> getGraph() {
        return Optional.ofNullable(this.graph);
    }

    @Override
    public VariableGraphTraversalSource.Builder asBuilder() {
        final VariableGraphTraversalSource.Builder builder = VariableGraphTraversalSource.build().engine(this.engine);
        this.withStrategies.forEach(builder::with);
        this.withoutStrategies.forEach(builder::without);
        return builder;
    }

    @Override
    public String toString() {
        return StringFactory.traversalSourceString(this);
    }

    //////

    public static class Builder implements TraversalSource.Builder<VariableGraphTraversalSource> {

        private TraversalEngine.Builder engineBuilder = StandardTraversalEngine.build();
        private List<TraversalStrategy> withStrategies = null;
        private List<Class<? extends TraversalStrategy>> withoutStrategies = null;

        private Builder() {}

        @Override
        public Builder engine(final TraversalEngine.Builder engineBuilder) {
            this.engineBuilder = engineBuilder;
            return this;
        }

        @Override
        public Builder with(final TraversalStrategy strategy) {
            if (null == this.withStrategies) this.withStrategies = new ArrayList<>();
            this.withStrategies.add(strategy);
            return this;
        }

        @Override
        public TraversalSource.Builder without(Class<? extends TraversalStrategy> strategyClass) {
            if (null == this.withoutStrategies) this.withoutStrategies = new ArrayList<>();
            this.withoutStrategies.add(strategyClass);
            return this;
        }

        @Override
        public VariableGraphTraversalSource create(final Graph graph) {
            return new VariableGraphTraversalSource(graph, this.engineBuilder,
                    null == this.withStrategies ? Collections.emptyList() : this.withStrategies,
                    null == this.withoutStrategies ? Collections.emptyList() : this.withoutStrategies);
        }
    }
}
