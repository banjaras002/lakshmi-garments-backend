package com.lakshmigarments.repository.specification;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.lakshmigarments.model.User;
import com.lakshmigarments.model.WorkflowRequest;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class WorfklowRequestSpecification {
	
	public static Specification<WorkflowRequest> filterByRequestedNames(List<String> requestedByNames) {
        return (root, query, criteriaBuilder) -> {
            if (requestedByNames != null && !requestedByNames.isEmpty()) {
                Join<WorkflowRequest, User> requestedByJoin = root.join("requestedBy", JoinType.INNER);
                Predicate[] predicates = requestedByNames.stream()
                        .map(requestedName -> criteriaBuilder.equal(requestedByJoin.get("name"), requestedName))
                        .toArray(Predicate[]::new);
                return criteriaBuilder.or(predicates);
            }
            return criteriaBuilder.conjunction();
        };
    }

}
