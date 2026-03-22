package com.xflprflx.neokioski.repositories;

import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.models.UserGroupState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserGroupStateRepository extends JpaRepository<UserGroupState, Long> {
    Optional<UserGroupState> findByUserAndGroup(User user, Group group);
}
