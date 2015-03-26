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

import org.apache.tinkerpop.gremlin.process.traversal.Scopeable;

import java.util.function.BiPredicate;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class TraversalVariable extends Number implements CharSequence, BiPredicate, Scopeable {
    private String variableName;

    public TraversalVariable(final String variableName) {
        this.variableName = variableName;
    }

    public String getVariableName() {
        return variableName;
    }

    public String getDefaultString() {
        return toString();
    }

    @Override
    public String toString() {
        return "{" + variableName + "}";
    }

    ///// BiPredicate dummy implementations
    @Override
    public boolean test(Object o, Object o2) {
        return false;
    }

    ///// CharSequence dummy implementations
    @Override
    public int length() {
        return 0;
    }

    @Override
    public char charAt(int index) {
        return 0;
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return null;
    }

    ///// Number dummy implementations - Number isn't an interface...wondering if we will run into other issues
    //                                   like that and be stuck without multiple inheritance

    @Override
    public int intValue() {
        return 0;
    }

    @Override
    public long longValue() {
        return 0;
    }

    @Override
    public float floatValue() {
        return 0;
    }

    @Override
    public double doubleValue() {
        return 0;
    }
}
