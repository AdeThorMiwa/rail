package com.rail.api.repository;

import com.rail.api.entity.UserSchedulingDay;
import com.rail.api.entity.UserSchedulingProfile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSchedulingDayRepository
    extends JpaRepository<UserSchedulingDay, Long>
{
    List<UserSchedulingDay> findByUserSchedulingProfile(
        UserSchedulingProfile profile
    );
    void deleteByUserSchedulingProfile(UserSchedulingProfile profile);
}
