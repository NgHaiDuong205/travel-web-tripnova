package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.SearchQueryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SearchQueryRepository extends JpaRepository<SearchQueryEntity, Long>, JpaSpecificationExecutor<SearchQueryEntity> {
}
