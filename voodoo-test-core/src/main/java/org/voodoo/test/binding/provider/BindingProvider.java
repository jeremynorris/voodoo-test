package org.voodoo.test.binding.provider;

import java.util.Properties;

import javax.annotation.Nonnull;

import com.google.inject.Module;

/**
 * BindingProvider
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
public interface BindingProvider {

    /**
     * @param config
     *            The configuration properties for this provider.
     * @return The module for this provider.
     */
    @Nonnull 
    public Module getModule(@Nonnull Properties config);
    
}
