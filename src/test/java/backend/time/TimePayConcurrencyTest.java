package backend.time;

import backend.time.model.Member.Member;
import backend.time.model.Member.Member_Role;
import backend.time.repository.MemberRepository;
import com.siot.IamportRestClient.IamportClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 틈새페이 동시성 테스트 — 비관적 잠금(SELECT FOR UPDATE) 검증
 *
 */
@SpringBootTest
class TimePayConcurrencyTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TransactionTemplate txTemplate;

    @MockBean
    private IamportClient iamportClient;

    private Long memberId;

    @BeforeEach
    void setUp() {
        GeometryFactory geometryFactory = new GeometryFactory();
        Point defaultLocation = geometryFactory.createPoint(new Coordinate(0, 90));
        defaultLocation.setSRID(4326);

        Member member = Member.builder()
                .kakaoId("test_kakao_" + System.nanoTime())
                .nickname("hello")
                .role(Member_Role.USER)
                .timePay(10_000L)
                .location(defaultLocation)
                .build();

        memberId = memberRepository.save(member).getId();
    }

    @Test
    @DisplayName("[비관적 잠금] 동시 충전+차감 시 잔액이 정확히 계산되어야 한다")
    void 동시_충전과_차감_잔액_정확성_검증() throws InterruptedException {
        long chargeAmount = 5_000L;
        long useAmount = 8_000L;
        // 기대값: 10,000 + 5,000 - 8,000 = 7,000

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Thread A: 충전 — findByIdWithLock으로 row 선점 후 잔액 추가
        executor.submit(() -> {
            try {
                readyLatch.countDown();
                startLatch.await();
                txTemplate.execute(status -> {
                    Member m = memberRepository.findByIdWithLock(memberId).orElseThrow();
                    m.addTimePay(chargeAmount);
                    return null;
                });
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread B: 차감 — findByIdWithLock으로 row 선점 후 잔액 차감
        executor.submit(() -> {
            try {
                readyLatch.countDown();
                startLatch.await();
                txTemplate.execute(status -> {
                    Member m = memberRepository.findByIdWithLock(memberId).orElseThrow();
                    m.setTimePay(m.getTimePay() - useAmount);
                    return null;
                });
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                doneLatch.countDown();
            }
        });

        readyLatch.await();
        startLatch.countDown(); // 동시 출발
        doneLatch.await();
        executor.shutdown();

        Member result = memberRepository.findById(memberId).orElseThrow();
        long expected = 10_000L + chargeAmount - useAmount; // 7,000

        System.out.println("===========================================");
        System.out.println("[동시성 테스트 결과]");
        System.out.println("초기 잔액:    10,000원");
        System.out.println("충전 금액:    " + chargeAmount + "원");
        System.out.println("차감 금액:    " + useAmount + "원");
        System.out.println("기대 잔액:    " + expected + "원");
        System.out.println("실제 잔액:    " + result.getTimePay() + "원");
        System.out.println("===========================================");

        assertThat(result.getTimePay()).isEqualTo(expected);
    }
}
