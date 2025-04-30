package backend.time.model.Member;

import backend.time.model.ActivityNotification;
import backend.time.model.KeywordNotification;
import backend.time.model.board.Board;
import backend.time.model.board.BoardCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.locationtech.jts.geom.Point;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String kakaoId; //카카오에서 주는 회원 고유 번호, 2018년 이후에 출시된 앱은 해당 회원이 탈퇴를 해도 같은 회원 번호를 부여 받음

    @Column(unique = true, length = 10)
    private String nickname; // 닉네임 10자 제한

    @Enumerated(EnumType.STRING)
    private Member_Role role;

    @Builder.Default
    private Long mannerTime = 15L; // 그 사람의 시간(like 당근 온도) //기본 15분

    @CreationTimestamp
    private Timestamp createDate;

    //지도관련
    private String address;

    @Column(nullable = false, columnDefinition = "POINT SRID 4326")
    private Point location;

    private Double latitude;
    private Double longitude;

    @Builder.Default
    private Long timePay = 0L; // 기본값으로 0L 설정


    //매너 평가

    @OneToMany(mappedBy = "member",cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MannerEvaluation> mannerEvaluationList = new ArrayList<>();

    //서비스 평가
    @OneToMany(mappedBy = "member",cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceEvaluation> serviceEvaluationList = new ArrayList<>();

    private Integer mannerEvaluationScore = 0;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityNotification> activityNotifications = new ArrayList<>();

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KeywordNotification> keywordNotifications = new ArrayList<>();

    @ElementCollection
    private Map<ServiceEvaluationCategory, Integer> serviceCount = new HashMap<>();

    public void addTimePay(Long amount) {
        timePay += amount;
    }
}
