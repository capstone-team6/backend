package backend.time.model.board;

import backend.time.model.ChatRoom;
import backend.time.model.KeywordNotification;
import backend.time.model.Member.Member;
import backend.time.model.Scrap;
import backend.time.model.pay.Account;
import backend.time.model.pay.PayMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Point;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trader_id")
    private Member trader;

    private Long itemPrice; // 해당 금액

    private String itemTime; // 해당 시간

    @OneToMany(mappedBy = "board",cascade = CascadeType.ALL)
    private List<Scrap> scraps = new ArrayList<>();

    private int chatCount = 0; // 채팅한 사람 수

    //엔티티를 scrap이라해서 그냥 통일하겠음
    private Integer ScrapCount=0; //관심 수

    @Enumerated(EnumType.STRING)
    private BoardState boardState = BoardState.SALE;

    @Enumerated(EnumType.STRING)
    private BoardType boardType = BoardType.BUY;

    @Enumerated(EnumType.STRING)
    private BoardCategory category;

    @Enumerated(EnumType.STRING)
    private PayMethod payMethod;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KeywordNotification> keywordNotifications = new ArrayList<>();

    @Column(name = "created_date")
    @CreationTimestamp
    private Timestamp createDate;

    @OneToOne(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private Account account; // 글에 대한 계좌 정보

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatRoom> chatRooms; // 글 삭제 시 chatroom들 삭제

    private String address;

    @Column(nullable = false, columnDefinition = "POINT SRID 4326")
    private Point location;

    private Double longitude;

    private Double latitude;

    //신고와 이의 신청은 다르지 ?
    // 신고는 게시글 신고, 유저 신고
    // 이의 신청은 거래 과정에서 생기는 아이
   private Integer reportCount=0 ; //신고 수


    /*@Enumerated(EnumType.STRING)
    private ReportStatus reportStatus;*/

    //이미지 연관 관계 메소드
    public void addImage(Image image) {
        this.images.add(image);
        image.setBoard(this);
    }

    public void removeImage(Image image) {
        this.images.remove(image);
        image.setBoard(null);
    }
}
