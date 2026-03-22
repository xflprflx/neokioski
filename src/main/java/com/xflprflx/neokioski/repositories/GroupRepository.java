package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.users")
    List<Group> findAllWithUsers();

    @Query("SELECT g FROM Group g JOIN g.users u WHERE u.id = :userId AND g.isActive = true")
    List<Group> findByUsersIdAndIsActiveTrue(@Param("userId") Long userId);

    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.users WHERE g.id = :id")
    java.util.Optional<Group> findByIdWithUsers(@Param("id") Long id);
}
