package com.aiblog.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用搜索/筛选 Specification 构造器。
 * 支持对若干文本字段做关键字模糊匹配，并按 tag / category 精确（包含）筛选。
 */
public final class SearchSpecs {

    private SearchSpecs() {}

    public static <T> Specification<T> build(String q, String tag, String category,
                                             List<String> searchFields) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                List<Predicate> ors = new ArrayList<>();
                for (String field : searchFields) {
                    ors.add(cb.like(cb.lower(root.get(field).as(String.class)), like));
                }
                predicates.add(cb.or(ors.toArray(new Predicate[0])));
            }

            if (tag != null && !tag.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("tags").as(String.class)),
                        "%" + tag.toLowerCase() + "%"));
            }

            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
