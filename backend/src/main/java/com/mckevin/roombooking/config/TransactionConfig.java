package com.mckevin.roombooking.config;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * Chaque cas d'usage public (classe {@code *Service} de la couche application) s'exécute dans une transaction,
 * sans que la couche application ne dépende de Spring : la frontière transactionnelle est déclarée ici.
 */
@Configuration(proxyBeanMethods = false)
public class TransactionConfig {

    static final String APPLICATION_PACKAGE = "com.mckevin.roombooking.application";
    /** Cas d'usage en lecture seule. */
    static final Set<String> QUERIES = Set.of("sites", "search", "planning", "mine", "rooms", "of", "me", "login");

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public static Advisor useCaseTransactionAdvisor(TransactionManager transactionManager) {
        TransactionAttributeSource attributes = (method, targetClass) -> {
            DefaultTransactionAttribute attribute = new DefaultTransactionAttribute(TransactionAttribute.PROPAGATION_REQUIRED);
            attribute.setReadOnly(QUERIES.contains(method.getName()));
            return attribute;
        };
        return new DefaultPointcutAdvisor(new UseCasePointcut(), new TransactionInterceptor(transactionManager, attributes));
    }

    static final class UseCasePointcut extends StaticMethodMatcherPointcut {

        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            return targetClass.getPackageName().startsWith(APPLICATION_PACKAGE)
                    && targetClass.getSimpleName().endsWith("Service")
                    && Modifier.isPublic(method.getModifiers())
                    && method.getDeclaringClass() != Object.class;
        }
    }
}
