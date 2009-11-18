package org.voodoo.test.testng;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.voodoo.test.binding.annotation.VoodooModuleBinding;
import org.voodoo.test.binding.annotation.VoodooModuleBindings;
import org.voodoo.test.binding.provider.BindingProvider;
import org.voodoo.test.data.provider.DataProvider;
import org.voodoo.test.data.provider.DataProviderManaged;
import org.voodoo.test.data.provider.annotation.VoodooDataProvider;
import org.voodoo.test.data.provider.annotation.VoodooDataProviders;

import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * VoodooTestNG
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
public abstract class VoodooTestNG {
    
    private Injector injector;
    
    @SuppressWarnings("unchecked")
    private Map<Class<? extends DataProvider>, Object> dataProviders = new HashMap<Class<? extends DataProvider>, Object>();
    
    /**
     * @frameworkUseOnly
     */
    protected VoodooTestNG() {
    }

    /**
     * @return The override module to use for this test.
     */
    @Nonnull
    protected Module getOverrideModule() {
        return new AbstractModule() {
            @Override
            protected void configure() {
                // Empty module:
            }
        };
    }
    
    /**
     * {@inheritDoc}
     */
    @BeforeClass
    public void beforeClassVoodooSetup() {
    
        // 1) Setup VooDoo module bindings:
        initializeModuleBindings();
        
        // 2) Setup VooDoo managed data providers:
        initializeManagedDataProviders();
    }
    
    /**
     * {@inheritDoc}
     */
    @BeforeMethod
    public void beforeMethodGuiceInjection() {        
        this.injector.injectMembers(this);
    }
    
    /**
     * Initialize module bindings.
     */
    protected void initializeModuleBindings() {
        List<Module> modules = new ArrayList<Module>();
        
        try {
            if (getClass().isAnnotationPresent(VoodooModuleBindings.class)) {
                VoodooModuleBindings bindings = getClass().getAnnotation(VoodooModuleBindings.class);
                for (VoodooModuleBinding binding : bindings.value()) {
                    Class<? extends BindingProvider> bindingClass = binding.clazz();
                    BindingProvider provider = bindingClass.newInstance();
                    String configStr = binding.config();
                    Properties config = new Properties();
                    if (!configStr.equals("")) {
                        InputStream configInputStream = getClass().getClassLoader().getResourceAsStream(configStr);
                        if (configInputStream == null) {
                            throw new RuntimeException("Resource: " + configStr + " is not found on the classpath.");
                        }
                        config.loadFromXML(configInputStream);
                    }
                    modules.add(provider.getModule(config));
                }
            }
        }
        catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        catch (InvalidPropertiesFormatException e) {
            throw new RuntimeException(e);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        Module combined = Modules.combine(modules);
        Module overrideModule = getOverrideModule();
        Module finalModule = Modules.override(combined).with(overrideModule);
        
        this.injector = Guice.createInjector(finalModule);
    }
    
    /**
     * Initialize managed data providers.
     */
    @SuppressWarnings("unchecked")
    protected void initializeManagedDataProviders() {
        try {
            if (getClass().isAnnotationPresent(VoodooDataProviders.class)) {
                VoodooDataProviders providersAnnotation = getClass().getAnnotation(VoodooDataProviders.class);
                for (VoodooDataProvider providerAnnotation : providersAnnotation.value()) {
                    Class<? extends DataProvider> providerClass = providerAnnotation.clazz();
                    String descriminatorId = providerAnnotation.descriminatorId();
                    // Check the injector first (to support providers being part of other module bindings):
                    DataProvider<Object> dataProvider = this.injector.getInstance(providerClass);
                    if (dataProvider == null) {
                        dataProvider = providerClass.newInstance();
                    }
                    if (dataProvider instanceof DataProviderManaged) {
                        DataProviderManaged dataProviderManager = (DataProviderManaged) dataProvider;
                        Object persistenceManager = getPersistenceManager(dataProviderManager, descriminatorId);
                        Object managedData = dataProviderManager.buildManagedData(persistenceManager);
                        this.dataProviders.put(providerClass, managedData);
                    }
                    else {
                        Object unmanagedData = dataProvider.getUnmanagedData();
                        this.dataProviders.put(providerClass, unmanagedData);
                    }
                }
            }
        }
        catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * @return The injector for this test.
     */
    protected Injector getInjector() {
        return injector;
    }
    
    /**
     * @param dataProviderClass
     *            The data provider class to request data for.
     * @return The data for the requested provider class.
     */
    protected Object getData(Class<? extends DataProvider<? extends Object>> dataProviderClass) {
        return this.dataProviders.get(dataProviderClass);
    }
    
    /**
     * @param provider
     *            The provider to get the persistence manager for.
     * @param descriminatorId
     *            The discriminator used for selecting the correct persistence
     *            manager.
     * @return The persistence manager.
     */
    private Object getPersistenceManager(DataProviderManaged<Object, Object> provider, String descriminatorId) {
        Class<Object> persistenceManagerClass = provider.getPersistenceManagerClass();
        String key = persistenceManagerClass.getCanonicalName() + "_" + descriminatorId;
        Map<Key<?>, Binding<?>> bindings = getInjector().getBindings();
        Binding<?> binding = bindings.get(key);
        if (binding == null) {
            binding = getInjector().getBinding(persistenceManagerClass);
        }
        if (binding == null) {
            throw new IllegalStateException("VoodooManagedDataProvider " + provider.getClass() + " specified but the associated " + persistenceManagerClass + " is not available in the injector (If JPA (w/Hibernate) is being used, perhaps @VoodooBindings of ProviderJpaHibernate needs to be added).");            
        }
        return binding.getProvider().get();
    }

}
