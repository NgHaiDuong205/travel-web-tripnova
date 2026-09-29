package com.duong.travelweb.repository.custom;

import com.duong.travelweb.builder.PostSearchBuilder;
import com.duong.travelweb.model.entity.PostEntity;

import java.util.List;

public interface PostRepositoryCustom {
    /** Kèm sẵn tác giả (JOIN FETCH). */
    List<PostEntity> findPosts(PostSearchBuilder criteria, int page, int limit);

    long countPosts(PostSearchBuilder criteria);
}
