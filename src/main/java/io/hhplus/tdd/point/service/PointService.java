package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.exception.PointShortageException;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    // 유저 포인트 조회
    public UserPoint getUserPoint(long id) {
        return userPointRepository.selectById(id);
    }

    // 유저 포인트 내역 조회
    public List<PointHistory> getUserHistoryList(long id) {
        return pointHistoryRepository.selectAllByUserId(id);
    }

    // 유저 포인트 충전
    @Synchronized
    public UserPoint chargePoint(long id, long amount) {
        // 유저 포인트 내역에 추가
        pointHistoryRepository.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return userPointRepository.insertOrUpdate(id, amount);
    }

    // 유저 포인트 사용성
    @Synchronized
    public UserPoint usePoint(long id, long amount) throws PointShortageException {
        // 상황 1. 포인트가 부족할 경우
        // 1. 유저의 보유 포인트 체크, 2. 사용할 amount와 비교
        UserPoint userPoint = userPointRepository.selectById(id);

        if(userPoint.point() < amount) {
            throw new PointShortageException("포인트가 부족합니다.");
        }

        pointHistoryRepository.insert(id, amount, TransactionType.USE, System.currentTimeMillis());

        return userPointRepository.insertOrUpdate(id, userPoint.point() - amount);
    }
}
