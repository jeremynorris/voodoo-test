package org.voodoo.test.data.provider;

/**
 * DataProvider
 *
 * <br>
 * Patterns:
 * 
 * <br>
 * Revisions:
 * jnorris: Nov 17, 2009: Initial revision.
 *
 * @author jnorris
 */
public interface DataProvider<T_DATA extends Object> {

    /**
     * @return The unmanaged data of this provider.
     */
    public T_DATA getUnmanagedData();
    
}
