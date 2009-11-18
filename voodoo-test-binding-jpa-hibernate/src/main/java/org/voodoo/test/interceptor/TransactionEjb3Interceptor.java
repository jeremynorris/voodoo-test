package org.voodoo.test.interceptor;

import java.lang.reflect.Method;
import java.util.Arrays;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.persistence.EntityManager;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * TransactionEjb3Interceptor
 * 
 * <br>
 * Patterns:
 * 
 * <br>
 * Revisions: jnorris: Oct 27, 2009: Initial revision.
 * 
 * @author jnorris
 */
public class TransactionEjb3Interceptor implements MethodInterceptor {

    private static final Logger LOG = Logger.getLogger(TransactionEjb3Interceptor.class);

    private EntityManager entityManager;

    /**
     * @param entityManager
     *            The entity manager to use for this interceptor.
     */
    public TransactionEjb3Interceptor(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {

        // Assumption:
        // this interceptor is only wired to public method on EJB3 components
        // (ie: @Stateless, @Stafeful, @MessageDriven).
        Class<?> declaringClass = methodInvocation.getThis().getClass();
        Class<?>[] interfaces = declaringClass.getInterfaces();
        // This is a temporary workaround for Guice's subclass-based proxying system.
        if (interfaces.length == 0) {
            declaringClass = declaringClass.getSuperclass();
            interfaces = declaringClass.getInterfaces();
        }
        Method invokedMethod = methodInvocation.getMethod();
        Class<?> invokedBusinessInterface = null;

        if (interfaces.length == 1) {
            // This is the business interface:
            if (isInterfaceMethod(invokedMethod, interfaces[0])) {
                invokedBusinessInterface = interfaces[0];
            }
        }
        else {
            for (Class<?> xInterface : interfaces) {
                if (xInterface.isAnnotationPresent(Local.class) || xInterface.isAnnotationPresent(Remote.class)) {
                    if (isInterfaceMethod(invokedMethod, xInterface)) {
                        invokedBusinessInterface = xInterface;
                        break;
                    }
                }
            }
        }

        Object result = null;
        if (invokedBusinessInterface != null) {

            Session session = (Session) this.entityManager.getDelegate();
            // TODO: Read @TransactionAttribute to perform correct behavior.
            // Currently the attribute is ignored and assumed to be REQUIRED.
            if (session.getTransaction().isActive()) {
                return methodInvocation.proceed();
            }

            Transaction transaction = session.beginTransaction();
            LOG.info("Started transaction: " + invokedBusinessInterface + "." + methodInvocation.getMethod().getName());
            try {
                result = methodInvocation.proceed();
            }
            catch (Exception e) {
                transaction.rollback();
                LOG.info("Rolled back transaction: " + invokedBusinessInterface + "." + methodInvocation.getMethod().getName());
                throw e;
            }

            transaction.commit();
            LOG.info("Committed transaction: " + invokedBusinessInterface + "." + methodInvocation.getMethod().getName());
        }
        else {
            result = methodInvocation.proceed();
        }

        return result;
    }

    /**
     * Check if a method is part of an interface.
     * 
     * @param method
     *            The method to check.
     * @param xInterface
     *            The interface to check against.
     * @return true if the method is part of the interface, false if it is not.
     */
    private boolean isInterfaceMethod(Method method, Class<?> xInterface) {
        boolean result = true;
        for (Method methodCandidate : xInterface.getMethods()) {
            // These methods cannot vary by return type, otherwise it would not
            // have compiled.
            if (methodCandidate.getName().equals(method.getName()) && Arrays.deepEquals(methodCandidate.getParameterTypes(), method.getParameterTypes())) {
                result = true;
                break;
            }
        }
        return result;
    }

}
