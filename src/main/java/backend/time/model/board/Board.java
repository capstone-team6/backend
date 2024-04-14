package backend.time.model.board;

import backend.time.model.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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

    private Long itemPrice; // 해당 금액

    private String itemTime; // 해당 시간

    //불필요
    /*@OneToMany(mappedBy = "board",cascade = CascadeType.ALL)
    private List<Scrap> scraps = new ArrayList<>();*/

    private int chatCount = 0; // 채팅한 사람 수

    //엔티티를 scrap이라해서 그냥 통일하겠음
    private Integer ScrapCount=0; //관심 수

    @Enumerated(EnumType.STRING)
    private BoardState boardState = BoardState.SALE;

    @Enumerated(EnumType.STRING)
    private BoardCategory boardCategory;

    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();

    @Column(name = "created_date")
    @CreationTimestamp
    private Timestamp createDate;

    //지도 관련
    private String address;
    private Double latitude;
    private Double longitude;

    //불필요
/*
    @Column(name = "modified_date")
    @CreationTimestamp
    private Timestamp modifiedDate;
*/

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