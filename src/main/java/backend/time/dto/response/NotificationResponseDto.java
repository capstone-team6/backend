package backend.time.dto.response;

import backend.time.model.ActivityType;
import java.util.List;
import lombok.Data;

public class NotificationResponseDto {
    @Data
    public static class ActivityNotificationDto {
        List<ActivityNotificationListDto> activityNotificationListDtoList;
    }

    @Data
    public static class ActivityNotificationListDto {

        private Long activityId;

        private ActivityType activityType;

        //스크랩 부분
        private String title;

        private String nickName;

        //거래완료 부분
        private String traderName;

        //공통 시간부분
        private String time;

    }

    @Data
    public static class KeywordNotificationListDto {

        private Long boardId;

        private Long keywordId;

        private String title;

        private String keyword;

        private String image;

        private String time;
    }

    @Data
    public static class KeywordNotificationDto {

        private List<KeywordNotificationListDto> keywordNotificationListDtos;

    }
}
