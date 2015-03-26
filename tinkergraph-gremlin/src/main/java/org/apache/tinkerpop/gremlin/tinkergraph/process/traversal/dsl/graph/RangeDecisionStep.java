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

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.step.Ranging;
import org.apache.tinkerpop.gremlin.process.traversal.step.filter.FilterStep;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class RangeDecisionStep<S> extends FilterStep<S> implements Ranging {
    private final Number low;
    private final Number high;
    private final TraversalVariable scopeVariable;

    public RangeDecisionStep(final Traversal.Admin traversal, final Number low, final Number high, final TraversalVariable scopeVariable) {
        super(traversal);
        this.low = low;
        this.high = high;
        this.scopeVariable = scopeVariable;
    }

    public TraversalVariable getScopeVariable() {
        return scopeVariable;
    }

    public Number getLow() {
        return low;
    }

    public Number getHigh() {
        return high;
    }

    @Override
    protected boolean filter(Traverser.Admin<S> traverser) {
        return false;
    }

    @Override
    public long getLowRange() {
        return low.longValue();
    }

    @Override
    public long getHighRange() {
        return high.longValue();
    }
}
