/*
 * Copyright © 2013-2018, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.seedstack.jpa.internal;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import org.seedstack.seed.transaction.spi.TransactionHandler;
import org.seedstack.seed.transaction.spi.TransactionMetadata;

class JpaTransactionHandler implements TransactionHandler<EntityTransaction> {

    private final EntityManagerLink entityManagerLink;
    private final EntityManagerFactory entityManagerFactory;

    JpaTransactionHandler(EntityManagerLink entityManagerLink,
            EntityManagerFactory entityManagerFactory) {
        this.entityManagerLink = entityManagerLink;
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void doInitialize(TransactionMetadata transactionMetadata) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        this.entityManagerLink.push(entityManager);
    }

    @Override
    public void doJoinGlobalTransaction() {
        this.entityManagerLink.get().joinTransaction();
    }

    @Override
    public EntityTransaction doCreateTransaction() {
        return this.entityManagerLink.get().getTransaction();
    }

    @Override
    public void doBeginTransaction(EntityTransaction entityTransaction) {
        entityTransaction.begin();
    }

    @Override
    public void doCommitTransaction(EntityTransaction entityTransaction) {
        entityTransaction.commit();
    }

    @Override
    public void doMarkTransactionAsRollbackOnly(EntityTransaction entityTransaction) {
        entityTransaction.setRollbackOnly();
    }

    @Override
    public void doRollbackTransaction(EntityTransaction entityTransaction) {
        entityTransaction.rollback();
    }

    @Override
    public void doReleaseTransaction(EntityTransaction entityTransaction) {
        if (entityTransaction.isActive()) {
            entityTransaction.rollback();
        }
    }

    @Override
    public void doCleanup() {
        this.entityManagerLink.pop().close();
    }

    @Override
    public EntityTransaction getCurrentTransaction() {
        return this.entityManagerLink.getCurrentTransaction();
    }
}
