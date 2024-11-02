package com.beyt.jdq.query;

import com.beyt.jdq.annotation.model.JdqModel;
import com.beyt.jdq.annotation.model.JdqField;
import com.beyt.jdq.annotation.model.JdqIgnoreField;
import com.beyt.jdq.annotation.model.JdqSubModel;
import com.beyt.jdq.dto.Criteria;
import com.beyt.jdq.dto.DynamicQuery;
import com.beyt.jdq.dto.enums.CriteriaOperator;
import com.beyt.jdq.exception.DynamicQueryIllegalArgumentException;
import com.beyt.jdq.query.rule.specification.*;
import com.beyt.jdq.repository.DynamicSpecificationRepositoryImpl;
import com.beyt.jdq.util.ApplicationContextUtil;
import com.beyt.jdq.util.field.FieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.tree.domain.AbstractSqmPath;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.data.util.Pair;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by tdilber at 28-Aug-19
 */
@Slf4j
public class DynamicQueryManager {

    public final static Map<CriteriaOperator, ISpecificationFilterRule> specificationRuleMap = new HashMap<>();

    static {
        specificationRuleMap.put(CriteriaOperator.CONTAIN, new SpecificationFilterDoubleLikeRule());
        specificationRuleMap.put(CriteriaOperator.DOES_NOT_CONTAIN, new SpecificationFilterDoesNotContainRule());
        specificationRuleMap.put(CriteriaOperator.END_WITH, new SpecificationFilterLeftLikeRule());
        specificationRuleMap.put(CriteriaOperator.START_WITH, new SpecificationFilterRightLikeRule());
        specificationRuleMap.put(CriteriaOperator.SPECIFIED, new SpecificationFilterSpecifiedRule());
        specificationRuleMap.put(CriteriaOperator.EQUAL, new SpecificationFilterEqualRule());
        specificationRuleMap.put(CriteriaOperator.NOT_EQUAL, new SpecificationFilterNotEqualRule());
        specificationRuleMap.put(CriteriaOperator.GREATER_THAN, new SpecificationFilterGreaterThanRule());
        specificationRuleMap.put(CriteriaOperator.GREATER_THAN_OR_EQUAL, new SpecificationFilterGreaterThanOrEqualToRule());
        specificationRuleMap.put(CriteriaOperator.LESS_THAN, new SpecificationFilterLessThanRule());
        specificationRuleMap.put(CriteriaOperator.LESS_THAN_OR_EQUAL, new SpecificationFilterLessThanOrEqualToRule());
        specificationRuleMap.put(CriteriaOperator.OR, null);
        specificationRuleMap.put(CriteriaOperator.PARENTHES, null);
        specificationRuleMap.put(CriteriaOperator.IN, new SpecificationFilterInRule());
        specificationRuleMap.put(CriteriaOperator.NOT_IN, new SpecificationFilterNotInRule());
    }

    public static <Entity> List<Entity> findAll(JpaSpecificationExecutor<Entity> repositoryExecutor,
                                                List<Criteria> searchCriteriaList) {
        List<Entity> result;
        List<Criteria> specificationRules = getMapSpecificRules(specificationRuleMap, searchCriteriaList);
        DynamicSpecification<Entity> specification = null;

        if (!specificationRules.isEmpty()) {
            specification = new DynamicSpecification<>(specificationRules);
        }

        return repositoryExecutor.findAll(specification);
    }

    public static <Entity> Page<Entity> findAll(JpaSpecificationExecutor<Entity> repositoryExecutor,
                                                List<Criteria> searchCriteriaList, Pageable pageable) {
        List<Criteria> specificationRules = getMapSpecificRules(specificationRuleMap, searchCriteriaList);

        DynamicSpecification<Entity> specification = null;
        if (!specificationRules.isEmpty()) {
            specification = new DynamicSpecification<>(specificationRules);
        }

        return repositoryExecutor.findAll(specification, pageable);
    }

    public static <Entity> Specification<Entity> getSpecification(List<Criteria> searchCriteriaList) {
        List<Criteria> specificationRules = getMapSpecificRules(specificationRuleMap, searchCriteriaList);

        DynamicSpecification<Entity> specification = null;
        if (!specificationRules.isEmpty()) {
            specification = new DynamicSpecification<>(specificationRules);
        }

        return specification;
    }

    public static <Entity> long count(JpaSpecificationExecutor<Entity> repositoryExecutor,
                                      List<Criteria> searchCriteriaList) {
        Long result = 0l;
        List<Criteria> specificationRules = getMapSpecificRules(specificationRuleMap, searchCriteriaList);
        DynamicSpecification<Entity> specification = null;
        if (specificationRules == null || !specificationRules.isEmpty()) {
            specification = new DynamicSpecification<>(specificationRules);
        }
        result = repositoryExecutor.count(specification);

        return result;
    }

    public static <Entity> List<Entity> getEntityListBySelectableFilterAsList(JpaSpecificationExecutor<Entity> repositoryExecutor, DynamicQuery dynamicQuery) {
        return (List<Entity>) DynamicQueryManager.getEntityListBySelectableFilter(repositoryExecutor, dynamicQuery, false);
    }

    public static <Entity> Page<Entity> getEntityListBySelectableFilterAsPage(JpaSpecificationExecutor<Entity> repositoryExecutor, DynamicQuery dynamicQuery) {
        return (Page<Entity>) DynamicQueryManager.getEntityListBySelectableFilter(repositoryExecutor, dynamicQuery, true);
    }

    protected static <Entity> Iterable<Entity> getEntityListBySelectableFilter(JpaSpecificationExecutor<Entity> repositoryExecutor, DynamicQuery dynamicQuery, boolean isPage) {
        Class<Entity> entityClass = getEntityClass(repositoryExecutor);
        return getEntityListBySelectableFilterWithReturnType(repositoryExecutor, dynamicQuery, entityClass, isPage);
    }

    public static <Entity> List<Tuple> getEntityListBySelectableFilterWithTupleAsList(JpaSpecificationExecutor<Entity> repositoryExecutor, DynamicQuery dynamicQuery) {
        return (List<Tuple>) DynamicQueryManager.getEntityListBySelectableFilterWithTuple(repositoryExecutor, dynamicQuery, false);
    }

    public static <Entity> Page<Tuple> getEntityListBySelectableFilterWithTupleAsPage(JpaSpecificationExecutor<Entity> repositoryExecutor, DynamicQuery dynamicQuery) {
        return (Page<Tuple>) DynamicQueryManager.getEntityListBySelectableFilterWithTuple(repositoryExecutor, dynamicQuery, true);
    }

    protected static <Entity> Iterable<Tuple> getEntityListBySelectableFilterWithTuple(JpaSpecificationExecutor<Entity> repositoryExecutor, DynamicQuery dynamicQuery, boolean isPage) {
        return getEntityListWithReturnClass(repositoryExecutor, dynamicQuery, Tuple.class, isPage);
    }

    public static <Entity, ResultType> List<ResultType> getEntityListBySelectableFilterWithReturnTypeAsList(JpaSpecificationExecutor<Entity> repositoryExecutor, DynamicQuery dynamicQuery, Class<ResultType> resultTypeClass) {
        return (List<ResultType>) DynamicQueryManager.getEntityListBySelectableFilterWithReturnType(repositoryExecutor, dynamicQuery, resultTypeClass, false);
    }

    public static <Entity, ResultType> Page<ResultType> getEntityListBySelectableFilterWithReturnTypeAsPage(JpaSpecificationExecutor<Entity> repositoryExecutor, DynamicQuery dynamicQuery, Class<ResultType> resultTypeClass) {
        return (Page<ResultType>) DynamicQueryManager.getEntityListBySelectableFilterWithReturnType(repositoryExecutor, dynamicQuery, resultTypeClass, true);
    }

    protected static <Entity, ResultType> Iterable<ResultType> getEntityListBySelectableFilterWithReturnType(JpaSpecificationExecutor<Entity> repositoryExecutor, DynamicQuery dynamicQuery, Class<ResultType> resultTypeClass, boolean isPage) {
        Class<Entity> entityClass = getEntityClass(repositoryExecutor);
        if (resultTypeClass.equals(entityClass) && CollectionUtils.isEmpty(dynamicQuery.getSelect())) {
            return getEntityListWithReturnClass(repositoryExecutor, dynamicQuery, resultTypeClass, isPage);
        } else {
            extractIfJdqModel(dynamicQuery, resultTypeClass);
            Iterable<Tuple> entityListBySelectableFilter = getEntityListWithReturnClass(repositoryExecutor, dynamicQuery, Tuple.class, isPage);

            if (!CollectionUtils.isEmpty(dynamicQuery.getSelect())) {
                return convertResultToResultTypeList(dynamicQuery.getSelect(), resultTypeClass, entityListBySelectableFilter, isPage);
            } else {

                if (!IterableUtils.isEmpty(entityListBySelectableFilter)) {
                    List<Pair<String, String>> parameters = entityListBySelectableFilter.iterator().next().getElements().stream().filter(e -> AbstractSqmPath.class.isAssignableFrom(e.getClass()))
                            .map(e -> Pair.of(((AbstractSqmPath) e).getModel().getPathName(), Objects.isNull(e.getAlias()) ? ((AbstractSqmPath) e).getModel().getPathName() : e.getAlias())).collect(Collectors.toList());
                    return convertResultToResultTypeList(parameters, resultTypeClass, entityListBySelectableFilter, isPage);
                } else {
                    return new ArrayList<>();
                }
            }
        }
    }

    private static <ResultType> void extractIfJdqModel(DynamicQuery dynamicQuery, Class<ResultType> resultTypeClass) {
        if (!resultTypeClass.isAnnotationPresent(JdqModel.class)) {
            return;
        }

        List<Pair<String, String>> select = new ArrayList<>();
        recursiveSupModelFiller(resultTypeClass, select, new ArrayList<>(), "");
        dynamicQuery.setSelect(select);
    }

    private static <ResultType> void recursiveSupModelFiller(Class<ResultType> resultTypeClass, List<Pair<String, String>> select, List<String> dbPrefixList, String entityPrefix) {
        for (Field declaredField : resultTypeClass.getDeclaredFields()) {
            if (declaredField.isAnnotationPresent(JdqSubModel.class)) {
                String subModelValue = declaredField.getAnnotation(JdqSubModel.class).value();
                ArrayList<String> newPrefixList = new ArrayList<>(dbPrefixList);
                if (StringUtils.isNotBlank(subModelValue)) {
                    newPrefixList.add(subModelValue);
                }
                recursiveSupModelFiller(declaredField.getType(), select, newPrefixList, entityPrefix + declaredField.getName() + ".");
            } else if (FieldUtil.isSupportedType(declaredField.getType())) {
                if (declaredField.isAnnotationPresent(JdqIgnoreField.class)) {
                    if (resultTypeClass.isRecord()) {
                        throw new DynamicQueryIllegalArgumentException("Record class can not have @JdqIgnoreField annotation");
                    }
                    continue;
                }

                if (declaredField.isAnnotationPresent(JdqField.class)) {
                    select.add(Pair.of(prefixCreator(dbPrefixList) + declaredField.getAnnotation(JdqField.class).value(), entityPrefix + declaredField.getName()));
                } else {
                    select.add(Pair.of(prefixCreator(dbPrefixList) + declaredField.getName(), entityPrefix + declaredField.getName()));
                }
            } else {
                if (resultTypeClass.isRecord()) {
                    throw new DynamicQueryIllegalArgumentException("Record didnt support nested model type: " + declaredField.getType().getName());
                }
            }
        }
    }

    private static String prefixCreator(List<String> prefixList) {
        String collect = String.join(".", prefixList);
        if (StringUtils.isNotBlank(collect)) {
            collect += ".";
        }
        return collect;
    }

    protected static <Entity, ResultType> Iterable<ResultType> getEntityListWithReturnClass(JpaSpecificationExecutor<Entity> repositoryExecutor, DynamicQuery dynamicQuery, Class<ResultType> resultTypeClass, boolean isPage) {
        Class<Entity> entityClass = getEntityClass(repositoryExecutor);
        EntityManager entityManager = ApplicationContextUtil.getEntityManager();
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ResultType> query = builder.createQuery(resultTypeClass);
        Root<Entity> root = query.from(entityClass);
        DynamicSpecification<Entity> specification = new DynamicSpecification<>(dynamicQuery.getWhere());
        Pageable pageable = Pageable.unpaged();

        query.distinct(dynamicQuery.isDistinct());

        if (!CollectionUtils.isEmpty(dynamicQuery.getSelect())) {
            List<Selection<?>> selectionList = new ArrayList<>();

            dynamicQuery.getSelect().forEach(selectField -> {
                selectionList.add(specification.createLocalFrom(root, selectField.getFirst()).get(DynamicSpecification.getFieldName(selectField.getFirst())).alias(selectField.getSecond()));
            });
            query.multiselect(selectionList);
        } else if (!resultTypeClass.equals(entityClass)) {
            List<Selection<?>> selectionList = new ArrayList<>();
            Set<Attribute> declaredAttributes = ((JpaRoot) root).getModel().getDeclaredAttributes();
            for (Attribute declaredAttribute : declaredAttributes) {
                selectionList.add(root.get(declaredAttribute.getName()));
            }
            query.multiselect(selectionList);
        }

        if (!CollectionUtils.isEmpty(dynamicQuery.getWhere())) {
            query.where(specification.toPredicate(root, query, builder));
        }

        if (!CollectionUtils.isEmpty(dynamicQuery.getOrderBy())) {
            List<Order> orderList = new ArrayList<>();
            dynamicQuery.getOrderBy().forEach((orderPair) -> {
                orderList.add(orderPair.getSecond() == com.beyt.jdq.dto.enums.Order.DESC ? builder.desc(specification.createLocalFrom(root, orderPair.getFirst()).get(DynamicSpecification.getFieldName(orderPair.getFirst()))) : builder.asc(specification.createLocalFrom(root, orderPair.getFirst()).get(DynamicSpecification.getFieldName(orderPair.getFirst()))));
            });
            query.orderBy(orderList);
        }

        TypedQuery<ResultType> typedQuery = entityManager.createQuery(query);


        if (Objects.nonNull(dynamicQuery.getPageSize())) {
            typedQuery.setMaxResults(dynamicQuery.getPageSize());

            if (Objects.nonNull(dynamicQuery.getPageNumber())) {
                typedQuery.setFirstResult(dynamicQuery.getPageSize() * dynamicQuery.getPageNumber());

                if (!CollectionUtils.isEmpty(dynamicQuery.getOrderBy())) {
                    pageable = PageRequest.of(dynamicQuery.getPageNumber(), dynamicQuery.getPageSize(), dynamicQuery.getOrderBy().get(0).getSecond().getDirection(), dynamicQuery.getOrderBy().stream().map(Pair::getFirst).toArray(String[]::new));
                } else {
                    pageable = PageRequest.of(dynamicQuery.getPageNumber(), dynamicQuery.getPageSize());
                }
            }
        }

        if (isPage) {
            return PageableExecutionUtils.getPage(typedQuery.getResultList(), pageable,
                    () -> executeCountQuery(getCountQuery(specification, entityClass)));
        } else {
            return typedQuery.getResultList();
        }
    }


    protected static <S> TypedQuery<Long> getCountQuery(@Nullable Specification<S> spec, Class<S> domainClass) {
        EntityManager entityManager = ApplicationContextUtil.getEntityManager();
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);

        Root<S> root = applySpecificationToCriteria(spec, domainClass, query);

        if (query.isDistinct()) {
            query.select(builder.countDistinct(root));
        } else {
            query.select(builder.count(root));
        }

        // Remove all Orders the Specifications might have applied
        query.orderBy(Collections.<Order>emptyList());

        return entityManager.createQuery(query);
    }

    protected static <S, U> Root<U> applySpecificationToCriteria(@Nullable Specification<U> spec, Class<U> domainClass,
                                                                 CriteriaQuery<S> query) {

        Assert.notNull(domainClass, "Domain class must not be null!");
        Assert.notNull(query, "CriteriaQuery must not be null!");
        EntityManager entityManager = ApplicationContextUtil.getEntityManager();
        Root<U> root = query.from(domainClass);

        if (spec == null) {
            return root;
        }

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        Predicate predicate = spec.toPredicate(root, query, builder);

        if (predicate != null) {
            query.where(predicate);
        }

        return root;
    }


    protected static long executeCountQuery(TypedQuery<Long> query) {

        Assert.notNull(query, "TypedQuery must not be null!");

        List<Long> totals = query.getResultList();
        long total = 0L;

        for (Long element : totals) {
            total += element == null ? 0 : element;
        }

        return total;
    }

    protected static <ResultType> Iterable<ResultType> convertResultToResultTypeList(List<Pair<String, String>> querySelects, Class<ResultType> resultTypeClass, Iterable<Tuple> entityListBySelectableFilter, boolean isPage) {
        Stream<Tuple> stream = isPage ? ((Page<Tuple>) entityListBySelectableFilter).stream() : ((List<Tuple>) entityListBySelectableFilter).stream();

        List<ResultType> resultTypeList;

        Map<String, Integer> selectsWithIndex = new HashMap<>();
        for (int i = 0; i < querySelects.size(); i++) {
            selectsWithIndex.put(querySelects.get(i).getSecond(), i);
        }

        Map<Class<?>, Map<Integer, Method>> classSetterMethodsMap = new HashMap<>();
        Map<Class<?>, Constructor<?>> recordConstructorMap = new HashMap<>();

        resultTypeList = stream.map(t -> fillModel(resultTypeClass, t, selectsWithIndex, classSetterMethodsMap, recordConstructorMap)).filter(Objects::nonNull).collect(Collectors.toList());


        if (isPage) {
            Page<Tuple> tuplePage = (Page<Tuple>) entityListBySelectableFilter;
            return new PageImpl<>(resultTypeList, tuplePage.getPageable(), tuplePage.getTotalElements());
        } else {
            return resultTypeList;
        }
    }

    protected static <ModelType> ModelType fillModel(Class<ModelType> modelType, Tuple t, Map<String, Integer> selectsWithIndex, Map<Class<?>, Map<Integer, Method>> classSetterMethodsMap, Map<Class<?>, Constructor<?>> recordConstructorMap) {
        Map<String, Object> subModelMap = new HashMap<>();
        for (Field declaredField : modelType.getDeclaredFields()) {
            if (declaredField.isAnnotationPresent(JdqSubModel.class)) {
                subModelMap.put(declaredField.getName(), fillModel(declaredField.getType(), t, selectsWithIndex.entrySet().stream().filter(e -> e.getKey().startsWith(declaredField.getName() + "."))
                        .collect(Collectors.toMap(k -> k.getKey().substring(declaredField.getName().length() + 1), Map.Entry::getValue)), classSetterMethodsMap, recordConstructorMap));
            }
        }


        if (modelType.isRecord()) {
            try {
                Constructor<ModelType> constructor = (Constructor<ModelType>) recordConstructorMap.get(modelType);
                if (Objects.isNull(constructor)) {
                    constructor = modelType.getConstructor(Arrays.stream(modelType.getRecordComponents())
                            .map(RecordComponent::getType)
                            .toArray(Class[]::new));
                    recordConstructorMap.put(modelType, constructor);
                }

                Parameter[] parameters = constructor.getParameters();
                Object[] args = new Object[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    if (selectsWithIndex.containsKey(parameters[i].getName())) {
                        Integer index = selectsWithIndex.get(parameters[i].getName());
                        args[i] = t.get(index);
                    } else {
                        args[i] = subModelMap.get(parameters[i].getName());
                    }
                }

                return constructor.newInstance(args);
            } catch (Exception e) {
                return null;
            }
        } else {
            List<Map.Entry<String, Integer>> fieldList = selectsWithIndex.entrySet().stream().filter(e -> !e.getKey().contains(".")).distinct().sorted(Comparator.comparing(Map.Entry::getValue)).toList();
            Map<Integer, Method> setterMethods = getIntegerMethodMap(fieldList.stream().map(e -> Pair.of(e.getValue(), e.getKey())).collect(Collectors.toList()), modelType, classSetterMethodsMap);
            try {
                ModelType resultObj = modelType.getConstructor().newInstance();
                for (Map.Entry<Integer, Method> entry : setterMethods.entrySet()) {
                    entry.getValue().invoke(resultObj, t.get(entry.getKey()));
                }
                for (Map.Entry<String, Object> stringObjectEntry : subModelMap.entrySet()) {
                    Field declaredField = resultObj.getClass().getDeclaredField(stringObjectEntry.getKey());
                    boolean canAccess = declaredField.canAccess(resultObj);
                    declaredField.setAccessible(true);
                    declaredField.set(resultObj, stringObjectEntry.getValue());
                    declaredField.setAccessible(canAccess);
                }
                return resultObj;
            } catch (Exception e) {
                return null;
            }
        }
    }


    private static <ResultType> Map<Integer, Method> getIntegerMethodMap(List<Pair<Integer, String>> querySelects, Class<ResultType> resultTypeClass, Map<Class<?>, Map<Integer, Method>> classSetterMethodsMap) {
        Map<Integer, Method> setterMethods = new HashMap<>();
        if (classSetterMethodsMap.containsKey(resultTypeClass)) {
            setterMethods = classSetterMethodsMap.get(resultTypeClass);
        } else {
            for (int i = 0; i < querySelects.size(); i++) {
                String select = querySelects.get(i).getSecond();

                Optional<Method> methodOptional = Arrays.stream(resultTypeClass.getMethods())
                        .filter(c -> c.getName().equalsIgnoreCase("set" + select)
                                && c.getParameterCount() == 1).findFirst();

                if (methodOptional.isPresent()) {
                    setterMethods.put(querySelects.get(i).getFirst(), methodOptional.get());
                }
            }
            classSetterMethodsMap.put(resultTypeClass, setterMethods);
        }
        return setterMethods;
    }

    @SuppressWarnings("unchecked")
    protected static <Entity> Class<Entity> getEntityClass(JpaSpecificationExecutor<Entity> repositoryExecutor) {
        Class<Entity> entityClass = (Class<Entity>) GenericTypeResolver.resolveTypeArgument(repositoryExecutor.getClass(), JpaSpecificationExecutor.class);
        if (Objects.nonNull(entityClass)) {
            return entityClass;
        } else if (repositoryExecutor instanceof DynamicSpecificationRepositoryImpl) {
            return ((DynamicSpecificationRepositoryImpl) repositoryExecutor).getDomainClass();
        } else {
            throw new IllegalStateException("Entity Class Type Detection Failed!");
        }
    }

    protected static <M extends Map<CriteriaOperator, ?>> List<Criteria> getMapSpecificRules(M map, List<Criteria> searchCriteriaList) {
        List<Criteria> result = new ArrayList<>();

        for (Criteria criteria : searchCriteriaList) {
            if (map.containsKey(criteria.getOperation())) {
                result.add(criteria);
            }
        }

        return result;
    }
}
