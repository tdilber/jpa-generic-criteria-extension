package com.beyt.jdq.query.rule.specification;

import com.beyt.jdq.dto.Criteria;
import com.beyt.jdq.util.SpecificationUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by chezard at 02-Nov-24
 */
@Slf4j
public class SpecificationFilterInRule implements ISpecificationFilterRule {

    @Override
    public Predicate generatePredicate(Path<?> root, CriteriaBuilder builder, Criteria criteria) {
        SpecificationUtil.checkHasFirstValue(criteria);
        var predicate = root.get(criteria.getKey()).in(criteria.getValues());
        return builder.or(predicate);
    }
}
