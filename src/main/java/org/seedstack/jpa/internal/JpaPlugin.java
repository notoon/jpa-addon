/**
 * Copyright (c) 2013-2016, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.jpa.internal;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.nuun.kernel.api.plugin.InitState;
import io.nuun.kernel.api.plugin.context.InitContext;
import io.nuun.kernel.api.plugin.request.ClasspathScanRequest;
import io.nuun.kernel.api.plugin.request.ClasspathScanRequestBuilder;
import org.seedstack.jdbc.spi.JdbcProvider;
import org.seedstack.jpa.JpaConfig;
import org.seedstack.jpa.JpaExceptionHandler;
import org.seedstack.seed.core.internal.AbstractSeedPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This plugin enables JPA support by creating an {@link javax.persistence.EntityManagerFactory} per persistence unit configured.
 */
public class JpaPlugin extends AbstractSeedPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaPlugin.class);
    private final Map<String, EntityManagerFactory> entityManagerFactories = new HashMap<>();
    private final Map<String, Class<? extends JpaExceptionHandler>> exceptionHandlerClasses = new HashMap<>();

    @Override
    public String name() {
        return "jpa";
    }

    @Override
    public Collection<Class<?>> dependencies() {
        return Lists.newArrayList(JdbcProvider.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public InitState initialize(InitContext initContext) {
        JpaConfig jpaConfig = getConfiguration(JpaConfig.class);

        if (jpaConfig.getUnits().isEmpty()) {
            LOGGER.info("No JPA persistence unit configured, JPA support disabled");
            return InitState.INITIALIZED;
        }

        EntityManagerFactoryFactory entityManagerFactoryFactory = new EntityManagerFactoryFactory(initContext.dependency(JdbcProvider.class), getApplication());
        for (Map.Entry<String, JpaConfig.PersistenceUnitConfig> entry : jpaConfig.getUnits().entrySet()) {
            String persistenceUnitName = entry.getKey();
            JpaConfig.PersistenceUnitConfig persistenceUnitConfig = entry.getValue();

            EntityManagerFactory emf;
            if (persistenceUnitConfig.isUsingDatasource()) {
                Set<Class<?>> scannedClasses = new HashSet<>();
                if (initContext.scannedClassesByAnnotationClass().get(Entity.class) != null) {
                    scannedClasses.addAll(initContext.scannedClassesByAnnotationClass().get(Entity.class));
                }
                if (initContext.scannedClassesByAnnotationClass().get(Embeddable.class) != null) {
                    scannedClasses.addAll(initContext.scannedClassesByAnnotationClass().get(Embeddable.class));
                }
                emf = entityManagerFactoryFactory.createEntityManagerFactory(persistenceUnitName, persistenceUnitConfig, scannedClasses);
            } else {
                emf = entityManagerFactoryFactory.createEntityManagerFactory(persistenceUnitName, persistenceUnitConfig);
            }
            entityManagerFactories.put(persistenceUnitName, emf);

            if (persistenceUnitConfig.hasExceptionHandler()) {
                exceptionHandlerClasses.put(persistenceUnitName, persistenceUnitConfig.getExceptionHandler());
            }
        }

        if (!Strings.isNullOrEmpty(jpaConfig.getDefaultUnit())) {
            JpaTransactionMetadataResolver.defaultJpaUnit = jpaConfig.getDefaultUnit();
        }

        return InitState.INITIALIZED;
    }

    @Override
    public void stop() {
        for (Map.Entry<String, EntityManagerFactory> entityManagerFactory : entityManagerFactories.entrySet()) {
            LOGGER.info("Closing entity manager factory for persistence unit {}", entityManagerFactory.getKey());
            try {
                entityManagerFactory.getValue().close();
            } catch (Exception e) {
                LOGGER.error(String.format("Unable to properly close entity manager factory for persistence unit %s", entityManagerFactory.getKey()), e);
            }
        }
    }

    @Override
    public Object nativeUnitModule() {
        return new JpaModule(entityManagerFactories, exceptionHandlerClasses);
    }

    @Override
    public Collection<ClasspathScanRequest> classpathScanRequests() {
        return new ClasspathScanRequestBuilder().annotationType(Entity.class).annotationType(Embeddable.class).build();
    }

}
