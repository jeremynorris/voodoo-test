package org.voodoo.test.data.provider.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.voodoo.test.data.provider.DataProvider;

/**
 * VoodooManagedDataProvider
 *
 * <br>
 * Patterns:
 * 
 * <br>
 * Revisions:
 * jnorris: Sep 22, 2009: Initial revision.
 *
 * @author jnorris
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VoodooDataProvider {
    @SuppressWarnings("unchecked")
    Class<? extends DataProvider> clazz();
    String descriminatorId();
}
