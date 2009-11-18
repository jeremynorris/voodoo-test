package org.voodoo.test.binding.provider.ejb3;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateful;
import javax.ejb.Stateless;

import org.apache.log4j.Logger;
import org.guiceyfruit.support.GuiceyFruitModule;
import org.scannotation.AnnotationDB;
import org.scannotation.ClasspathUrlFinder;
import org.voodoo.test.binding.provider.BindingProvider;

import com.google.inject.Module;

/**
 * BindingProviderEjb3
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
public class BindingProviderEjb3 implements BindingProvider {

    private static final Logger LOG = Logger.getLogger(BindingProviderEjb3.class);
    
    private static final Set<String> SEARCH_RESOURCES = new HashSet<String>(Arrays.asList(new String[] { "META-INF/ejb-jar.xml" }));
    
    /**
     * Default constructor.
     */
    public BindingProviderEjb3() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Module getModule(@Nonnull Properties config) {
        
        final AnnotationDB annotationDb = buildAnnotationDb();
        Set<URL> urls = getUrls(SEARCH_RESOURCES, Collections.<Class<?>>emptySet());
        try {
            annotationDb.scanArchives(urls.toArray(new URL[] {}));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        Set<String> ejbClasses = new HashSet<String>();
        Set<String> statelessSet = annotationDb.getAnnotationIndex().get(Stateless.class.getCanonicalName());
        if (statelessSet != null) {
            ejbClasses.addAll(statelessSet);
        }
        Set<String> statefulSet = annotationDb.getAnnotationIndex().get(Stateful.class.getCanonicalName());
        if (statefulSet != null) {
            ejbClasses.addAll(statefulSet);
        }

        return new GuiceyFruitModule() {
            @SuppressWarnings("unchecked")
            protected void configure() {
                super.configure();
                bindAnnotationInjector(EJB.class, VoodooTestEJBMemberProvider.class);

                Set<String> localInterfaces = annotationDb.getAnnotationIndex().get(Local.class.getCanonicalName());
                Map<Class<?>, Class<?>> interfaceToImplMap;
                try {
                    interfaceToImplMap = mapEjbs(annotationDb);
                    if (localInterfaces != null) {
                        for (String ifClassName : localInterfaces) {
                            Class ifClass = Class.forName(ifClassName, false, this.getClass().getClassLoader());
                            if (interfaceToImplMap.containsKey(ifClass)) {
                                Class implClass = interfaceToImplMap.get(ifClass);
                                bind(ifClass).to(implClass);
                                LOG.info("Binding " + ifClass.getCanonicalName() + " to " + implClass.getCanonicalName());
                            }
                        }
                    }
                }
                catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
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
    
    /**
     * finds all the classes annotated with the passed in classes
     * 
     * @throws ClassNotFoundException
     */
    private Map<Class<?>, Class<?>> mapEjbs(AnnotationDB db) throws ClassNotFoundException {
        Set<String> ejbClasses = new HashSet<String>();

        if (db.getAnnotationIndex().get(Stateless.class.getCanonicalName()) != null) {
            ejbClasses.addAll(db.getAnnotationIndex().get(Stateless.class.getCanonicalName()));
        }
        if (db.getAnnotationIndex().get(Stateful.class.getCanonicalName()) != null) {
            ejbClasses.addAll(db.getAnnotationIndex().get(Stateful.class.getCanonicalName()));
        }
        Map<Class<?>, Class<?>> map = new HashMap<Class<?>, Class<?>>();
        for (String ejbClass : ejbClasses) {
            Class<?> implClass = Class.forName(ejbClass, false, this.getClass().getClassLoader());
            if (!Modifier.isAbstract(implClass.getModifiers())) {
                for (Class<?> ifClass : implClass.getInterfaces()) {
                    if (ifClass.isAnnotationPresent(Local.class)) {
                        if (map.containsKey(ifClass)) {
                            throw new IllegalStateException("Local interface " + ifClass.getCanonicalName() + " is mapped to two implementations classes. ["
                                    + map.get(ifClass).getCanonicalName() + ", " + implClass.getCanonicalName() + "]");

                        }
                        map.put(ifClass, implClass);
                    }
                }
            }
        }

        return map;
    }

}
