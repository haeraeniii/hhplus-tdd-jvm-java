package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.exception.PointShortageException;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private UserPointRepository userPointRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    private UserPoint userPoint(long id, long amount) {
        return UserPoint.builder()
                .id(id)
                .point(amount)
                .build();
    }

    private PointHistory pointHistory(long id, long userId, long amount, TransactionType type, long updateMillis) {
        return PointHistory.builder()
                .id(id)
                .userId(userId)
                .amount(amount)
                .type(type)
                .updateMillis(updateMillis)
                .build();
    }

    @Test
    @DisplayName("유저 아이디로 포인트 조회")
    public void getUserPointTest() {
        //given
        long id = 1L;
        UserPoint userPoint = userPoint(id, 30000);
        when(userPointRepository.selectById(id)).thenReturn(userPoint);

        //when, then
        assertThat(pointService.getUserPoint(id)).isEqualTo(userPoint);
    }

    @Test
    @DisplayName("유저 아이디로 포인트 내역 조회")
    public void getUserHistoryListTest() {
        //given
        long userId = 2L;
        PointHistory pointHistory1 = pointHistory(0L, userId, 3000, TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory pointHistory2 = pointHistory(1L, userId, 4000, TransactionType.CHARGE, System.currentTimeMillis());
        PointHistory pointHistory3 = pointHistory(2L, userId, 2000, TransactionType.USE, System.currentTimeMillis());

        List<PointHistory> list = new ArrayList<>();
        list.add(pointHistory1);
        list.add(pointHistory2);
        list.add(pointHistory3);

        given(pointHistoryRepository.selectAllByUserId(userId)).willReturn(list);

        //when
        List<PointHistory> pointHistories = pointService.getUserHistoryList(userId);

        //then
        assertThat(pointHistories.size()).isEqualTo(list.size());
    }

    private void checkPoint (long amount, long point) throws PointShortageException {
        if(amount > point) {
            throw new PointShortageException("포인트가 부족합니다.");
        }
    }

    @Test
    @DisplayName("포인트 부족할 경우 포인트 사용 실패")
    public void usePointTest() {
        //given
        long amount = 3000L;
        UserPoint userPoint = userPoint(1L, 2000L);

        //when, then
        Assertions.assertThrows(PointShortageException.class, () -> checkPoint(amount, userPoint.point()));

    }
}