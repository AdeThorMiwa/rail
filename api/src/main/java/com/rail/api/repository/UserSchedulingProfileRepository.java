package com.rail.api.repository;

import com.rail.api.entity.User;
import com.rail.api.entity.UserSchedulingProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSchedulingProfileRepository
    extends JpaRepository<UserSchedulingProfile, Long>
{
    Optional<UserSchedulingProfile> findByUser(User user);
    Optional<UserSchedulingProfile> findByPid(UUID pid);
    boolean existsByUser(User user);
}
