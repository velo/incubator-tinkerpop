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
package org.apache.tinkerpop.gremlin.process.traversal.step.map;

import org.apache.tinkerpop.gremlin.process.computer.KeyValue;
import org.apache.tinkerpop.gremlin.process.computer.MapReduce;
import org.apache.tinkerpop.gremlin.process.computer.traversal.TraversalVertexProgram;
import org.apache.tinkerpop.gremlin.process.computer.util.StaticMapReduce;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.step.MapReducer;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.ReducingBarrierStep;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.TraverserRequirement;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.function.ConstantSupplier;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiFunction;

import static org.apache.tinkerpop.gremlin.process.traversal.NumberHelper.add;
import static org.apache.tinkerpop.gremlin.process.traversal.NumberHelper.mul;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class SumGlobalStep<S extends Number> extends ReducingBarrierStep<S, S> implements MapReducer {

    private static final Set<TraverserRequirement> REQUIREMENTS = EnumSet.of(
            TraverserRequirement.BULK,
            TraverserRequirement.OBJECT
    );

    public SumGlobalStep(final Traversal.Admin traversal) {
        super(traversal);
        this.setSeedSupplier(new ConstantSupplier<>((S) Integer.valueOf(0)));
        this.setBiFunction(SumGlobalBiFunction.instance());
    }


    @Override
    public Set<TraverserRequirement> getRequirements() {
        return REQUIREMENTS;
    }

    @Override
    public MapReduce<MapReduce.NullObject, Number, MapReduce.NullObject, Number, Number> getMapReduce() {
        return SumGlobalMapReduce.instance();
    }

    /////

    private static class SumGlobalBiFunction<S extends Number> implements BiFunction<S, Traverser<S>, S>, Serializable {

        private static final SumGlobalBiFunction INSTANCE = new SumGlobalBiFunction<>();

        private SumGlobalBiFunction() {

        }

        @Override
        public S apply(final S mutatingSeed, final Traverser<S> traverser) {
            return (S) add(mutatingSeed, mul(traverser.get(), traverser.bulk()));
        }

        public static <S extends Number> SumGlobalBiFunction<S> instance() {
            return INSTANCE;
        }
    }

    ///////////

    private static class SumGlobalMapReduce extends StaticMapReduce<MapReduce.NullObject, Number, MapReduce.NullObject, Number, Number> {

        private static final SumGlobalMapReduce INSTANCE = new SumGlobalMapReduce();

        private SumGlobalMapReduce() {

        }

        @Override
        public boolean doStage(final MapReduce.Stage stage) {
            return true;
        }

        @Override
        public void map(final Vertex vertex, final MapEmitter<NullObject, Number> emitter) {
            final Iterator<Number> values = IteratorUtils.map(vertex.<Set<Traverser.Admin<Number>>>property(TraversalVertexProgram.HALTED_TRAVERSERS).orElse(Collections.emptySet()).iterator(),
                    traverser -> mul(traverser.get(), traverser.bulk()));
            if (values.hasNext())
                emitter.emit(getSum(values));
        }

        @Override
        public void combine(final NullObject key, final Iterator<Number> values, final ReduceEmitter<NullObject, Number> emitter) {
            this.reduce(key, values, emitter);
        }

        @Override
        public void reduce(final NullObject key, final Iterator<Number> values, final ReduceEmitter<NullObject, Number> emitter) {
            if (values.hasNext())
                emitter.emit(getSum(values));
        }

        private Number getSum(final Iterator<Number> numbers) {
            Number sum = numbers.next();
            while (numbers.hasNext()) {
                sum = add(sum, numbers.next());
            }
            return sum;
        }

        @Override
        public String getMemoryKey() {
            return REDUCING;
        }

        @Override
        public Number generateFinalResult(final Iterator<KeyValue<NullObject, Number>> keyValues) {
            return keyValues.hasNext() ? keyValues.next().getValue() : 0;
        }

        public static SumGlobalMapReduce instance() {
            return INSTANCE;
        }
    }
}