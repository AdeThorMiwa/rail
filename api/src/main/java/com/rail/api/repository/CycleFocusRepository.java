package com.rail.api.repository;

import com.rail.api.entity.CycleFocus;
import com.rail.api.entity.UserCycle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CycleFocusRepository extends JpaRepository<CycleFocus, Long> {

    List<CycleFocus> findByCycleOrderByPositionAsc(UserCycle cycle);
}
