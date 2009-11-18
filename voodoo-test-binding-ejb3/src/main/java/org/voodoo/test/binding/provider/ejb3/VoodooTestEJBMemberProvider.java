/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.voodoo.test.binding.provider.ejb3;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import javax.ejb.EJB;

import org.guiceyfruit.Injectors;
import org.guiceyfruit.support.AnnotationMemberProviderSupport;

import com.google.inject.Binding;
import com.google.inject.ConfigurationException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

/**
 * VoodooTestEJBMemberProvider
 *
 * Temporary until changes pushed upstream/reconciled with GuiceyFruit.
 */
public class VoodooTestEJBMemberProvider extends AnnotationMemberProviderSupport<EJB> {

    @Inject
    private Injector injector;

    public boolean isNullParameterAllowed(EJB annotation, Method method, Class<?> parameterType, int parameterIndex) {
        return false;
    }

    protected Object provide(EJB annotation, Member member, TypeLiteral<?> requiredType, Class<?> memberType, Annotation[] annotations) {
        String name = getValueName(annotation.beanName(), member);
        return provideObjectFromNamedBindingOrJndi(requiredType, name);
    }

    @SuppressWarnings("unchecked")
    protected Object provideObjectFromNamedBindingOrJndi(TypeLiteral<?> requiredType, String name) {
        Binding<?> binding = Injectors.getBinding(injector, Key.get(requiredType, Names.named(name)));
        if (binding != null) {
            return binding.getProvider().get();
        }
        Class typeClass = requiredType.getRawType();
        Object ret;
        try {
            ret = injector.getInstance(typeClass);
            if (ret != null) {
                return ret;
            }
        }
        catch (ConfigurationException e) {
            // ignore this and fall back to name check
        }
        throw new IllegalArgumentException("Cannot find class named " + name + " of type " + requiredType.toString());
    }

    /**
     * if no valid name is present on the annotation then use the member name
     */
    protected String getValueName(String nameFromAnnotation, Member member) {
        if (nameFromAnnotation == null || nameFromAnnotation.length() == 0) {
            nameFromAnnotation = member.getName();
        }
        if (nameFromAnnotation == null || nameFromAnnotation.length() == 0) {
            throw new IllegalArgumentException("No name defined");
        }
        return nameFromAnnotation;
    }
}
