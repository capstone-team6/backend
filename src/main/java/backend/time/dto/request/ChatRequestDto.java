package backend.time.dto.request;

import lombok.Data;

public class ChatRequestDto {
    @Data
    public static class RoomEnterDto {

        private String roomName;
        private Long boardId;
    }
}
