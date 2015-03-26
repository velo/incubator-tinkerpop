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

import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.Scopeable;
import org.apache.tinkerpop.gremlin.process.traversal.Step;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.step.filter.HasStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.filter.IsStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.filter.RangeGlobalStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.RangeLocalStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.HasContainer;
import org.apache.tinkerpop.gremlin.process.traversal.util.DefaultTraversal;
import org.apache.tinkerpop.gremlin.structure.Graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class DefaultVariableGraphTraversal<S, E> extends DefaultTraversal<S, E> implements VariableGraphTraversal.Admin<S, E> {

    private final Map<Step, List<TraversalVariablePosition>> stepVariables = new HashMap<>();

    public DefaultVariableGraphTraversal(final Graph graph) {
        super(graph);
    }

    @Override
    public VariableGraphTraversal.Admin<S, E> asAdmin() {
        return this;
    }

    @Override
    public VariableGraphTraversal<S, E> iterate() {
        return VariableGraphTraversal.Admin.super.iterate();
    }

    @Override
    public DefaultVariableGraphTraversal<S, E> clone() {
        return (DefaultVariableGraphTraversal<S, E>) super.clone();
    }

    @Override
    public Map<Step, List<TraversalVariablePosition>> getStepVariables() {
        return stepVariables;
    }

    @Override
    public VariableGraphTraversal<S, E> has(final CharSequence key, final BiPredicate predicate, final Object value) {
        final String k;
        final List<TraversalVariablePosition> variablePositions = new ArrayList<>();
        if ((key instanceof TraversalVariable)) {
            final TraversalVariable var = (TraversalVariable) key;
            k = var.getDefaultString();

            final TraversalVariablePosition variablePosition = new TraversalVariablePosition(var, 0);
            variablePositions.add(variablePosition);
        } else {
            k = key != null ? key.toString() : null;
        }

        if ((predicate instanceof TraversalVariable))
            variablePositions.add(new TraversalVariablePosition((TraversalVariable) predicate, 1));
        if ((value instanceof TraversalVariable))
            variablePositions.add(new TraversalVariablePosition((TraversalVariable) value, 2));

        final Step s = new HasStep(this.asAdmin(), new HasContainer(k, predicate, value));
        stepVariables.put(s, variablePositions);
        return this.asAdmin().addStep(s);
    }

    @Override
    public VariableGraphTraversal<S, E> is(final BiPredicate predicate, final Object value) {
        final List<TraversalVariablePosition> variablePositions = new ArrayList<>();
        if ((predicate instanceof TraversalVariable))
            variablePositions.add(new TraversalVariablePosition((TraversalVariable) predicate, 0));

        if ((value instanceof TraversalVariable))
            variablePositions.add(new TraversalVariablePosition((TraversalVariable) value, 1));

        final Step s = new IsStep(this.asAdmin(), predicate, value);
        stepVariables.put(s, variablePositions);
        return this.asAdmin().addStep(s);
    }

    @Override
    public VariableGraphTraversal<S, E> range(final Scopeable scope, final Number low, final Number high) {
        // this one is tricky because Scope is an enum - enums would have to implement an interface that
        // TraversalVariable could implement.  maybe we just add a Scopeable interface as a marker to
        // arrange for that. that seems pretty non-intrusive.
        final List<TraversalVariablePosition> variablePositions = new ArrayList<>();

        final int l;
        if ((low instanceof TraversalVariable)) {
            final TraversalVariable var = (TraversalVariable) low;
            l = var.intValue();

            final TraversalVariablePosition variablePosition = new TraversalVariablePosition(var, 0);
            variablePositions.add(variablePosition);
        } else {
            l = low != null ? low.intValue() : 0;
        }

        final int h;
        if ((high instanceof TraversalVariable)) {
            final TraversalVariable var = (TraversalVariable) high;
            h = var.intValue();

            final TraversalVariablePosition variablePosition = new TraversalVariablePosition(var, 1);
            variablePositions.add(variablePosition);
        } else {
            h = high != null ? high.intValue() : 0;
        }

        // scope is a tricky one.  scope determines the step that is added.  if it is parameterized then there is
        // no way to know which step will be used.  added a "decision" step to mark that position and be a holder
        // for the variables.
        final Step s;
        if ((scope instanceof TraversalVariable)) {
            final TraversalVariable var = (TraversalVariable) scope;
            s = new RangeDecisionStep(this.asAdmin(), l, h, var);
            final TraversalVariablePosition variablePosition = new TraversalVariablePosition(var, -1);
            variablePositions.add(variablePosition);
        } else {
            s = scope.equals(Scope.global)
                    ? new RangeGlobalStep<>(this.asAdmin(), l, h)
                    : new RangeLocalStep<>(this.asAdmin(), l, h);
        }

        stepVariables.put(s, variablePositions);
        return this.asAdmin().addStep(s);
    }
}