package org.voodoo.test.data.provider;

import javax.persistence.EntityManager;

/**
 * DataProviderManagedJpa
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
public interface DataProviderManagedJpa<T_DATA> extends DataProviderManaged<T_DATA, EntityManager> {   
}
