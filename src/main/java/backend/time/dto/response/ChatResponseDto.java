package backend.time.dto.response;

import backend.time.dto.ChatDto;
import backend.time.model.ChatType;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class ChatResponseDto {
    @Data
    public static class ChatSendResponseDto {
        private Long roomId;
        private Long boardId;
        private Long messageId;
        private String writer;
        private String message;
        private ChatType type;
        private String time;
        private Long buyerRead;
        private Long sellerRead;
        private List<String> images;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ChatRoomDetailDto {
        private Long roomId;
        private Long boardId;
        private Long otherUserId;
        private String roomName;
        private String name; //구매자 이름
        private String message; //마지막 채팅
        private String time; //마지막 채팅시간 ex)몇분전
        private Long noReadChat;
    }

    @Getter
    @Setter
    public static class ChatRoomResponseDto {

        Long roomId;
        String roleType;
        Long OtherUserId;
        String nickName;
        Long boardId;
        String roomName;
        List<ChatDto> chatlist;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ChatRoomResponseDto that = (ChatRoomResponseDto) o;
            return Objects.equals(getRoomId(), that.getRoomId()) && Objects.equals(getRoleType(), that.getRoleType())
                    && Objects.equals(getOtherUserId(), that.getOtherUserId()) && Objects.equals(getNickName(),
                    that.getNickName()) && Objects.equals(getBoardId(), that.getBoardId()) && Objects.equals(
                    getRoomName(), that.getRoomName()) && Objects.equals(getChatlist(), that.getChatlist());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getRoomId(), getRoleType(), getOtherUserId(), getNickName(), getBoardId(),
                    getRoomName(), getChatlist());
        }

        @Override
        public String toString() {
            return "ChatRoomResponseDto{" +
                    "roomId=" + roomId +
                    ", roleType='" + roleType + '\'' +
                    ", OtherUserId=" + OtherUserId +
                    ", nickName='" + nickName + '\'' +
                    ", boardId=" + boardId +
                    ", roomName='" + roomName + '\'' +
                    ", chatlist=" + chatlist +
                    '}';
        }
    }
}
