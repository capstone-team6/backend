package backend.time.model.pay;

import static backend.time.model.pay.PayState.*;

import backend.time.model.Member.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class PayCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="member_id")
    private Member member; // 충전을 한 사람

    private Long amount; // 충전 한 금액

    @Column(name = "merchant_uid")
    private String merchantUid;

    @Column(name = "imp_uid")
    private String impUid; //결제 고유 번호

    private PayState payState;

    @CreationTimestamp
    private Timestamp createDate;

    public void charge(String impUid) {
        this.impUid = impUid;
        payState = COMPLETE;
    }
}
