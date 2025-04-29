package backend.time.repository;

import backend.time.model.pay.PayCharge;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayChargeRepository extends JpaRepository<PayCharge, Long> {
    Optional<PayCharge> findByMerchantUid(String merchantUid);

    boolean existsByImpUid(String impUid);
}

