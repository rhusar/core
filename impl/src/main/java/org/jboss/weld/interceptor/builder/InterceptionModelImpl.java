/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.weld.interceptor.builder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.weld.interceptor.spi.metadata.InterceptorMetadata;
import org.jboss.weld.interceptor.spi.model.InterceptionType;

/**
 * @author <a href="mailto:mariusb@redhat.com">Marius Bogoevici</a>
 * @author <a href="mailto:mluksa@redhat.com">Marko Luksa</a>
 */

class InterceptionModelImpl<T, I> implements BuildableInterceptionModel<T, I> {

    private final Map<InterceptionType, List<InterceptorMetadata<I>>> globalInterceptors = new HashMap<InterceptionType, List<InterceptorMetadata<I>>>();

    private final Map<InterceptionType, Map<MethodReference, List<InterceptorMetadata<I>>>> methodBoundInterceptors = new HashMap<InterceptionType, Map<MethodReference, List<InterceptorMetadata<I>>>>();

    private final Set<MethodReference> methodsIgnoringGlobals = new HashSet<MethodReference>();

    private final Set<InterceptorMetadata<I>> allInterceptors = new LinkedHashSet<InterceptorMetadata<I>>();

    private final T interceptedEntity;

    private boolean hasTargetClassInterceptors;

    private boolean hasExternalNonConstructorInterceptors;

    public InterceptionModelImpl(T interceptedEntity) {
        this.interceptedEntity = interceptedEntity;
    }

    public List<InterceptorMetadata<I>> getInterceptors(InterceptionType interceptionType, Method method) {
        if (InterceptionType.AROUND_CONSTRUCT.equals(interceptionType)) {
            throw new IllegalStateException("Cannot use getInterceptors() for @AroundConstruct interceptor lookup. Use getConstructorInvocationInterceptors() instead.");
        }
        if (interceptionType.isLifecycleCallback() && method != null) {
            throw new IllegalArgumentException("On a lifecycle callback, the associated method must be null");
        }

        if (!interceptionType.isLifecycleCallback() && method == null) {
            throw new IllegalArgumentException("Around-invoke and around-timeout interceptors are defined for a given method");
        }

        if (interceptionType.isLifecycleCallback()) {
            if (globalInterceptors.containsKey(interceptionType)) {
                return globalInterceptors.get(interceptionType);
            }
        } else {
            MethodReference methodReference = methodHolder(method);
            ArrayList<InterceptorMetadata<I>> returnedInterceptors = new ArrayList<InterceptorMetadata<I>>();
            if (!methodsIgnoringGlobals.contains(methodReference) && globalInterceptors.containsKey(interceptionType)) {
                returnedInterceptors.addAll(globalInterceptors.get(interceptionType));
            }
            Map<MethodReference, List<InterceptorMetadata<I>>> map = methodBoundInterceptors.get(interceptionType);
            if (map != null) {
                List<InterceptorMetadata<I>> list = map.get(methodReference);
                if (list != null) {
                    returnedInterceptors.addAll(list);
                }
            }
            return returnedInterceptors;
        }
        return Collections.emptyList();
    }

    public Set<InterceptorMetadata<I>> getAllInterceptors() {
        return Collections.unmodifiableSet(allInterceptors);
    }

    public T getInterceptedEntity() {
        return this.interceptedEntity;
    }

    public void setIgnoresGlobals(Method method, boolean ignoresGlobals) {
        if (ignoresGlobals) {
            methodsIgnoringGlobals.add(methodHolder(method));
        } else {
            methodsIgnoringGlobals.remove(methodHolder(method));
        }
    }

    public void appendInterceptors(InterceptionType interceptionType, Method method, InterceptorMetadata<I>... interceptors) {
        if (interceptionType != InterceptionType.AROUND_CONSTRUCT) {
            hasExternalNonConstructorInterceptors = true;
        }
        if (null == method) {
            List<InterceptorMetadata<I>> interceptorsList = globalInterceptors.get(interceptionType);
            if (interceptorsList == null) {
                interceptorsList = new ArrayList<InterceptorMetadata<I>>();
                globalInterceptors.put(interceptionType, interceptorsList);
            }
            appendInterceptorClassesToList(interceptionType, interceptorsList, interceptors);
        } else {
            MethodReference methodHolder = methodHolder(method);
            if (null == methodBoundInterceptors.get(interceptionType)) {
                methodBoundInterceptors.put(interceptionType, new HashMap<MethodReference, List<InterceptorMetadata<I>>>());
            }
            List<InterceptorMetadata<I>> interceptorsList = methodBoundInterceptors.get(interceptionType).get(methodHolder);
            if (interceptorsList == null) {
                interceptorsList = new ArrayList<InterceptorMetadata<I>>();
                methodBoundInterceptors.get(interceptionType).put(methodHolder, interceptorsList);
            }
            appendInterceptorClassesToList(interceptionType, interceptorsList, interceptors);
        }
        allInterceptors.addAll(Arrays.asList(interceptors));
    }

    private void appendInterceptorClassesToList(InterceptionType interceptionType, List<InterceptorMetadata<I>> interceptorsList, InterceptorMetadata<I>... interceptors) {
        interceptorsList.addAll(Arrays.asList(interceptors));
    }

    private static MethodReference methodHolder(Method method) {
        return MethodReference.of(method, true);
    }

    @Override
    public List<InterceptorMetadata<I>> getConstructorInvocationInterceptors() {
        if (globalInterceptors.containsKey(InterceptionType.AROUND_CONSTRUCT)) {
            return globalInterceptors.get(InterceptionType.AROUND_CONSTRUCT);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean hasExternalConstructorInterceptors() {
        return !getConstructorInvocationInterceptors().isEmpty();
    }

    @Override
    public boolean hasExternalNonConstructorInterceptors() {
        return hasExternalNonConstructorInterceptors;
    }

    public void setHasTargetClassInterceptors(boolean hasTargetClassInterceptors) {
        this.hasTargetClassInterceptors = hasTargetClassInterceptors;
    }

    @Override
    public boolean hasTargetClassInterceptors() {
        return hasTargetClassInterceptors;
    }
}
