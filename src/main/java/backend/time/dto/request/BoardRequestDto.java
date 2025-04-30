package backend.time.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.springframework.web.multipart.MultipartFile;

public class BoardRequestDto {
    @Data
    public static class WriteBoardDto {

        private String category;

        private String boardType;

        @NotEmpty
        @Length(max = 20)
        private String title;

        @NotEmpty
        private String time;

        //나눔일때는 null로
        private Long price;

        @NotEmpty
        @Length(max = 500)
        private String content;

        //지도
        private String address;

        private Double latitude;

        private Double longitude;


        List<MultipartFile> images;
    }

    @Data
    public static class BoardSearchDto {
        private String keyword;
        private int pageNum = 0;
        private String category;
        private String boardType = "BUY";
    }

    @Data
    public static class BoardUpdateDto {

        private String title;
        private String content;

        private String time;
        private Long price;

        //글에 담겨있는 주소 정보 (주소, 경도, 위도)
        private String address;
        private Double longitude;
        private Double latitude;

        //board 가테고리, type
        private String category;
        private String boardType;

        //이미지들
        List<MultipartFile> images;
    }

    @Data
    public static class PointDto {

        private Double longitude;
        private Double latitude;
        private String address;
        private String kakaoId;
    }
}
