package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    @Query("SELECT g FROM Group g LEFT JOIN FETCH g.users")
    List<Group> findAllWithUsers();
}
