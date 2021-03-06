/*
 * Copyright © 2013-2018, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.seedstack.jpa.internal.specification;

import javax.persistence.criteria.Predicate;
import org.seedstack.business.specification.NotSpecification;
import org.seedstack.business.spi.SpecificationConverter;
import org.seedstack.business.spi.SpecificationTranslator;

class JpaNotConverter implements
        SpecificationConverter<NotSpecification<?>, JpaTranslationContext<?>, Predicate> {

    @Override
    public Predicate convert(NotSpecification<?> specification, JpaTranslationContext<?> context,
            SpecificationTranslator<JpaTranslationContext<?>, Predicate> translator) {
        return context.getCriteriaBuilder()
                .not(translator.translate(specification.getSpecification(), context));
    }
}
