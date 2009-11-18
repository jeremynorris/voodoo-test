package org.voodoo.test.matcher;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.log4j.Logger;

import com.google.inject.matcher.AbstractMatcher;

/**
 * MatcherPublicMethod
 *
 * <br>
 * Patterns:
 * 
 * <br>
 * Revisions:
 * jnorris: Oct 27, 2009: Initial revision.
 *
 * @author jnorris
 */
public class MatcherPublicMethod extends AbstractMatcher<Method> {
    
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(MatcherPublicMethod.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(Method method) {
        return Modifier.isPublic(method.getModifiers());
    }
    
}
