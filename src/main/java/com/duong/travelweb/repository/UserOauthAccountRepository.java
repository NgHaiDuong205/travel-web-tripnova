package com.duong.travelweb.repository;

import com.duong.travelweb.model.entity.UserOauthAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserOauthAccountRepository extends JpaRepository<UserOauthAccountEntity, UUID> {

    @Query("SELECT a FROM UserOauthAccountEntity a WHERE a.provider = :provider AND a.providerUserId = :providerUserId")
    Optional<UserOauthAccountEntity> findByProviderAccount(@Param("provider") String provider,
                                                          @Param("providerUserId") String providerUserId);

    @Query("SELECT a FROM UserOauthAccountEntity a WHERE a.userId = :userId ORDER BY a.linkedAt")
    List<UserOauthAccountEntity> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT a FROM UserOauthAccountEntity a WHERE a.userId = :userId AND a.provider = :provider")
    Optional<UserOauthAccountEntity> findByUserIdAndProvider(@Param("userId") UUID userId, @Param("provider") String provider);

    @Modifying
    @Query("DELETE FROM UserOauthAccountEntity a WHERE a.userId = :userId")
    int deleteByUserId(@Param("userId") UUID userId);
}
