package org.voodoo.test.data.provider;

/**
 * DataProviderManaged
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
public interface DataProviderManaged<T_DATA, T_PERSISTENCE_MANAGER> extends DataProvider<T_DATA> {
    
    /**
     * @return The class of the persistence manager of this data provider.
     */
    public Class<T_PERSISTENCE_MANAGER> getPersistenceManagerClass();
    
    /**
     * Build and store data (ie: data that is persisted and managed by an
     * underlying store. In the case of JPA, this may include things like @GeneratedValue @Id
     * being set correctly by the data store).
     * 
     * @param persistenceManager
     *            The persistence manager to store the data with.
     * @return The resulting managed data.
     */
    public T_DATA buildManagedData(T_PERSISTENCE_MANAGER persistenceManager);

    
}
