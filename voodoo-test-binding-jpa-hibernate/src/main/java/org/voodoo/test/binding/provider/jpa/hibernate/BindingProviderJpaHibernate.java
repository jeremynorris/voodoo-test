package org.voodoo.test.binding.provider.jpa.hibernate;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.ejb.MessageDriven;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.log4j.Logger;
import org.guiceyfruit.jpa.JpaModule;
import org.hibernate.Session;
import org.hibernate.ejb.Ejb3Configuration;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;
import org.voodoo.test.binding.provider.BindingProvider;
import org.voodoo.test.interceptor.TransactionEjb3Interceptor;
import org.voodoo.test.matcher.MatcherPublicMethod;

import com.google.inject.Module;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;

/**
 * BindingProviderJpaHibernate
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
public class BindingProviderJpaHibernate implements BindingProvider {

    private static final Logger LOG = Logger.getLogger(BindingProviderJpaHibernate.class);
    
    private static final String KEY_PERSISTENCE_DESCRIMINATOR_ID = "persistence-descriminator-id";
    private static final Set<String> SEARCH_RESOURCES = new HashSet<String>(Arrays.asList(new String[] { "META-INF/orm.xml" }));
    
    /**
     * Default constructor.
     */
    public BindingProviderJpaHibernate() {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Module getModule(Properties config) {
        
        final AnnotationDB annotationDb = buildAnnotationDb();
        Set<URL> urls = getUrls(SEARCH_RESOURCES, Collections.<Class<?>>emptySet());
        try {
            annotationDb.scanArchives(urls.toArray(new URL[] {}));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        String keyDescriminatorId = config.getProperty(KEY_PERSISTENCE_DESCRIMINATOR_ID);
        
        Ejb3Configuration ejb3Configuration = new Ejb3Configuration();
        addEntitiesToConfig(annotationDb, ejb3Configuration);
        
        ejb3Configuration.setProperties(config);
        
        EntityManagerFactory emf = ejb3Configuration.buildEntityManagerFactory();
        final EntityManager em = emf.createEntityManager();
        
        return getJpaModule(em, keyDescriminatorId);
    }

    /**
     * Build a JPA module.
     * 
     * @param entityManager
     *            The entity manager to use.
     * @param descriminatorId
     *            The descriminator to bind the entity manager into the module
     *            with.
     */
    private Module getJpaModule(final EntityManager entityManager, String descriminatorId) {
        return new JpaModule() {
            protected void configure() {
                super.configure();
                Session hibernateSession = (Session) entityManager.getDelegate();
                
                // TODO: Manage the lifecycle of entity manager and session
                // correctly.
                bind(EntityManager.class).toInstance(entityManager);
                bind(Session.class).toInstance(hibernateSession);
                   
                // Setup EJB3 transaction demarcation boundaries:
                Matcher<AnnotatedElement> matcherEjb3Component = Matchers.annotatedWith(Stateless.class).or(Matchers.annotatedWith(Stateful.class)).or(Matchers.annotatedWith(MessageDriven.class));
                
                
                bindInterceptor(matcherEjb3Component, new MatcherPublicMethod(), new TransactionEjb3Interceptor(entityManager));                
            }
        };
    }

    /**
     * Setup the annotation scanning configuration.
     * 
     * @return The configuration DB, ready for scanning.
     */
    private AnnotationDB buildAnnotationDb() {
        AnnotationDB db = new AnnotationDB();
        db.setScanClassAnnotations(true);
        db.setScanFieldAnnotations(false);
        db.setScanMethodAnnotations(false);
        db.setScanParameterAnnotations(false);
        return db;
    }
    
    /**
     * gets urls that contain the package this class is located in.
     * 
     * @return the urls
     */
    @SuppressWarnings("unchecked")
    private Set<URL> getUrls(@Nonnull Set<String> urls, @Nonnull Set<Class<?>> additionalClasses) {
        Set<URL> ret = new HashSet<URL>();
        for (String resource : urls) {
            ret.addAll(Arrays.asList(ClasspathUrlFinder.findResourceBases(resource)));
        }
        for (Class clazz : additionalClasses) {
            ret.addAll(Arrays.asList(ClasspathUrlFinder.findClassBase(clazz)));
        }
        return ret;
    }
    
    @SuppressWarnings("unchecked")
    private void addEntitiesToConfig(AnnotationDB db, Ejb3Configuration cfg) {
        Set<String> foundClasses = db.getAnnotationIndex().get(Entity.class.getCanonicalName());
        if (foundClasses != null) {
            Set<String> packages = new HashSet<String>();
            for (String entityClassName : foundClasses) {
                try {
                    Class entityClass = Class.forName(entityClassName, false, this.getClass().getClassLoader());
                    if (entityClass.isAnnotationPresent(Entity.class)) {
                        try {
                            Class.forName(entityClass.getPackage().getName() + ".package-info", false, this.getClass().getClassLoader());
                            packages.add(entityClass.getPackage().getName());
                        }
                        catch (ClassNotFoundException e) {
                            // nothing to do here check for class definition
                        }
                        cfg.addAnnotatedClass(entityClass);
                    }
                }
                catch (ClassNotFoundException e1) {
                    throw new RuntimeException(e1);
                }
            }
            // adds custom hibernate mappings
            for (String packageName : packages) {
                cfg.addPackage(packageName);
            }
        }
    }
    
}
