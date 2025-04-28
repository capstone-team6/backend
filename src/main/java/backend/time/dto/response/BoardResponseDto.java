package backend.time.dto.response;

import backend.time.model.board.BoardCategory;
import backend.time.model.board.BoardState;
import backend.time.model.board.BoardType;
import com.querydsl.core.annotations.QueryProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.List;
import lombok.Builder;
import lombok.Data;

public class BoardResponseDto {
    @Data
    @Builder
    public static class BoardSearchSpatial {
        private Long boardId;
        private String title;
        private String itemTime;
        private Long itemPrice;
        private Timestamp createdDate;
        private int chatCount;
        private int scrapCount;
        private Double distance;
        private String address;
        private BoardState boardState;
        private String firstImage;

        @QueryProjection
        public BoardSearchSpatial(Long boardId, String title, String itemTime, Long itemPrice,
                                  Timestamp createdDate,
                                  int chatCount, int scrapCount, Double distance, String address,
                                  BoardState boardState,
                                  String firstImage) {
            this.boardId = boardId;
            this.title = title;
            this.itemTime = itemTime;
            this.itemPrice = itemPrice;
            this.createdDate = createdDate;
            this.chatCount = chatCount;
            this.scrapCount = scrapCount;
            this.distance = distance;
            if (distance != null) {
                this.distance = BigDecimal.valueOf(distance / 1000.0)
                        .setScale(1, RoundingMode.DOWN)
                        .doubleValue();
            }
            this.address = address;
            this.boardState = boardState;
            this.firstImage = firstImage;
        }
    }

    @Data
    @Builder
    public static class BoardSearchHaversine {
        private Long boardId;
        private String title;
        private String itemTime;
        private Long itemPrice;
        private Timestamp createdDate;
        private int chatCount;
        private int scrapCount;
        private Double distance;
        private String address;
        private BoardState boardState;
        private String firstImage;

        @QueryProjection
        public BoardSearchHaversine(Long boardId, String title, String itemTime, Long itemPrice,
                                  Timestamp createdDate,
                                  int chatCount, int scrapCount, Double distance, String address,
                                  BoardState boardState,
                                  String firstImage) {
            this.boardId = boardId;
            this.title = title;
            this.itemTime = itemTime;
            this.itemPrice = itemPrice;
            this.createdDate = createdDate;
            this.chatCount = chatCount;
            this.scrapCount = scrapCount;
            this.distance = BigDecimal.valueOf(distance)
                    .setScale(1, RoundingMode.DOWN)
                    .doubleValue();
            this.address = address;
            this.boardState = boardState;
            this.firstImage = firstImage;
        }
    }

    @Data
    @Builder
    public static class BoardListResponseDto {
        private Long boardId;
        private String title;
        private String itemTime;
        private Long itemPrice;
        private Timestamp createdDate;
        private int chatCount;
        private int scrapCount;
        private Double distance;
        private String address;
        private BoardState boardState;
        private String firstImage;
        private BoardType boardType;
    }

    @Data
    @Builder
    public static class ScrapListResponseDto {
        private Long boardId;
        private String title;
        private String itemTime;
        private Long itemPrice;
        private Timestamp createdDate;
        private int chatCount;
        private int scrapCount;
        private Double distance;
        private String address;
        private BoardState boardState;
        private String firstImage;
        private BoardType boardType;

        @QueryProjection
        public ScrapListResponseDto(Long boardId, String title, String itemTime, Long itemPrice,
                                    Timestamp createdDate,
                                    int chatCount, int scrapCount, Double distance, String address,
                                    BoardState boardState,
                                    String firstImage, BoardType boardType) {
            this.boardId = boardId;
            this.title = title;
            this.itemTime = itemTime;
            this.itemPrice = itemPrice;
            this.createdDate = createdDate;
            this.chatCount = chatCount;
            this.scrapCount = scrapCount;
            this.distance = BigDecimal.valueOf(distance / 1000.0)
                    .setScale(1, RoundingMode.DOWN)
                    .doubleValue();
            if (this.distance > 1000) {
                this.distance = 0D;
            }
            this.address = address;
            this.boardState = boardState;
            this.firstImage = firstImage;
            this.boardType = boardType;
        }
    }

    @Data
    @Builder
    public static class BoardDetailResponseDto {
        //roomName 채팅방 있으면 채팅방이름 넘겨주고 없으면 null
        private String roomName;
        //본인이 쓴 글인지 확인
        private String who; //reader, writer
        //boardId 게시글 식별자
        private Long boardId;
        private String scrapStus;
        //글쓴 사람 닉네임, 틈새시간
        private Long userId;
        private String nickname;
        private Long mannerTime;
        //글의 기본 정보 (제목,내용,글쓴날짜)
        private String title;
        private String content;
        private Timestamp createdDate;
        private String itemTime;
        private Long itemPrice;
        //채팅수, 스크랩수
        private int chatCount;
        private int scrapCount;
        //글에 담겨있는 주소 정보 (주소, 경도, 위도)
        private String address;
        private Double longitude;
        private Double latitude;
        //board 가테고리, state, type
        private BoardState boardState;
        private BoardCategory category;
        private BoardType boardType;
        //이미지들
        private List<String> images;
    }

    @Data
    @Builder
    public static class UserAddressResponseDto {
        private Double userLongitude;
        private Double userLatitude;
        private String address;
    }

    @Data
    @Builder
    public static class AccountResponseDto {
        private String holder; // 예금주
        private String bank; // 은행
        private Long accountNumber; //계좌번호
    }

    @Data
    public static class WhoResponseDto {
        private String role;
    }
}
