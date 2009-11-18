package org.voodoo.test.binding.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.voodoo.test.binding.provider.BindingProvider;

/**
 * VoodooModuleBinding
 *
 * <br>
 * Patterns:
 * 
 * <br>
 * Revisions:
 * jnorris: Sep 22, 2009: Initial revision.
 *
 * @author dangleton
 * @author jnorris
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VoodooModuleBinding {
    Class<? extends BindingProvider> clazz();
    String config() default "";
}
