package com.xflprflx.neokioski.services;

import com.xflprflx.neokioski.models.Group;
import com.xflprflx.neokioski.models.User;
import com.xflprflx.neokioski.models.UserGroupState;
import com.xflprflx.neokioski.repositories.UserGroupStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class UserGroupStateService {
    
    private final UserGroupStateRepository repository;

    public UserGroupStateService(UserGroupStateRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void updateLastRead(User user, Group group, Instant timestamp) {
        Optional<UserGroupState> opt = repository.findByUserAndGroup(user, group);
        if (opt.isPresent()) {
            UserGroupState state = opt.get();
            if (state.getLastReadTimestamp() == null || state.getLastReadTimestamp().isBefore(timestamp)) {
                state.setLastReadTimestamp(timestamp);
                repository.save(state);
            }
        } else {
            UserGroupState state = new UserGroupState(user, group, timestamp);
            repository.save(state);
        }
    }

    public Instant getLastReadTimestamp(User user, Group group) {
        return repository.findByUserAndGroup(user, group)
                .map(UserGroupState::getLastReadTimestamp)
                .orElse(Instant.EPOCH);
    }
}
