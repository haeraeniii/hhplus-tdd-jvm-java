package io.hhplus.tdd.repository;

import io.hhplus.tdd.point.UserPoint;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPointRepository {
    UserPoint selectById(Long id);

    UserPoint insertOrUpdate(long id, long amount);
}
