package backend.time.controller;

import backend.time.dto.response.ChatResponseDto.ChatRoomDetailDto;
import backend.time.model.ChatRoom;
import backend.time.model.board.BoardType;
import backend.time.service.ChattingService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("my-page")
@Slf4j
public class MyPageController {

    private final ChattingService chattingService;

    /**
     * 채팅 목록 조회 페이지
     */
    @GetMapping("/chat")
    public Result findRoomsByMemberPage(@AuthenticationPrincipal UserDetails userDetails){
        // 1. 채팅 목록 조회 SELECT
        List<ChatRoom> chatRoomList = chattingService.findChatRoomByMember(userDetails.getUsername());



        final Long[] totalNoReadChat = {0L};
        List<ChatRoomDetailDto> collect = chatRoomList.stream()
                .map(m -> {
                    ChatRoomDetailDto dto = new ChatRoomDetailDto();

                    if (m.getBoard().getBoardType().equals(BoardType.BUY) //게시물이 BUY고 게시글 작성자가 나라면 BUYER
                            && m.getBoard().getMember().getKakaoId().equals(userDetails.getUsername())) {
                        dto.setRoomId(m.getId());
                        dto.setBoardId(m.getBoard().getId());
                        dto.setOtherUserId(m.getBuyer().getId());//상대방은 chatroom의 buyer(채팅누름사람)
                        dto.setRoomName(m.getName());
                        dto.setName(m.getBuyer().getNickname());   //상대방 닉네임
                        dto.setMessage(m.getLastChat().getMessage());  //마지막 채팅
                        dto.setNoReadChat(m.getLastChat().getMessageId() - m.getLastChat().getBuyerRead()); //내가 BUYER면
                        dto.setTime(Time.calculateTime(m.getLastChat().getCreateDate())); //마지막 채팅시간 ex)몇분전
                        if ((m.getLastChat().getMessageId() - m.getLastChat().getBuyerRead() >= 0)) {
//                            totalNoReadChat.set(totalNoReadChat.get() + (m.getLastChat().getMessageId() - m.getLastChat().getBuyerRead()));
                            totalNoReadChat[0] += (m.getLastChat().getMessageId()-m.getLastChat().getBuyerRead());
                        }
                    } else if (m.getBoard().getBoardType().equals(BoardType.BUY) //게시물이 BUY이고 게시글 작성자가 내가 아니면 SELLER
                            && !m.getBoard().getMember().getKakaoId().equals(userDetails.getUsername())) {
                        dto.setRoomId(m.getId());
                        dto.setBoardId(m.getBoard().getId());
                        dto.setOtherUserId(m.getBoard().getMember().getId());//상대방은 게시글 작성자
                        dto.setRoomName(m.getName());
                        dto.setName(m.getBoard().getMember().getNickname());   //상대방 닉네임
                        dto.setMessage(m.getLastChat().getMessage());  //마지막 채팅
                        dto.setNoReadChat(m.getLastChat().getMessageId() - m.getLastChat().getSellerRead());
                        dto.setTime(Time.calculateTime(m.getLastChat().getCreateDate())); //마지막 채팅시간 ex)몇분전
                        if ((m.getLastChat().getMessageId() - m.getLastChat().getSellerRead() >= 0)) {
//                            totalNoReadChat.set(totalNoReadChat.get() + (m.getLastChat().getMessageId() - m.getLastChat().getBuyerRead()));
                            totalNoReadChat[0] += (m.getLastChat().getMessageId() - m.getLastChat().getSellerRead());
                        }
                    } else if (m.getBoard().getBoardType().equals(BoardType.SELL) //게시물이 SELL이고 게시글 작성자가 나라면 SELLER
                            && m.getBoard().getMember().getKakaoId().equals(userDetails.getUsername())) {
                        dto.setRoomId(m.getId());
                        dto.setBoardId(m.getBoard().getId());
                        dto.setOtherUserId(m.getBuyer().getId()); //상대방은 채팅방의 buyer(채탱누른사람)
                        dto.setRoomName(m.getName());
                        dto.setName(m.getBuyer().getNickname());   //상대방 닉네임
                        dto.setMessage(m.getLastChat().getMessage());  //마지막 채팅
                        dto.setNoReadChat(m.getLastChat().getMessageId() - m.getLastChat().getSellerRead());
                        dto.setTime(Time.calculateTime(m.getLastChat().getCreateDate())); //마지막 채팅시간 ex)몇분전
                        if ((m.getLastChat().getMessageId() - m.getLastChat().getSellerRead() >= 0)) {
//                            totalNoReadChat.set(totalNoReadChat.get() + (m.getLastChat().getMessageId() - m.getLastChat().getBuyerRead()));
                            totalNoReadChat[0] += (m.getLastChat().getMessageId() - m.getLastChat().getSellerRead());
                        }
                    } else if (m.getBoard().getBoardType().equals(BoardType.SELL) //게시물이 SELL이고 게시물 작성자가 내가 아니면 BUYER
                            && !m.getBoard().getMember().getKakaoId().equals(userDetails.getUsername())) {
                        //상대방은 게시글 작성자
                        dto.setRoomId(m.getId());
                        dto.setBoardId(m.getBoard().getId());
                        dto.setOtherUserId(m.getBoard().getMember().getId());
                        dto.setRoomName(m.getName());
                        dto.setName(m.getBoard().getMember().getNickname());   //상대방 닉네임
                        dto.setMessage(m.getLastChat().getMessage());  //마지막 채팅
                        dto.setNoReadChat(m.getLastChat().getMessageId() - m.getLastChat().getBuyerRead());
                        dto.setTime(Time.calculateTime(m.getLastChat().getCreateDate())); //마지막 채팅시간 ex)몇분전
                        if ((m.getLastChat().getMessageId() - m.getLastChat().getBuyerRead() >= 0)) {
//                            totalNoReadChat.set(totalNoReadChat.get() + (m.getLastChat().getMessageId() - m.getLastChat().getBuyerRead()));
                            totalNoReadChat[0] += (m.getLastChat().getMessageId() - m.getLastChat().getBuyerRead());
                        }
                    }

                    return dto;
                }).collect(Collectors.toList());


        ChatRoomListDto chatRoomListDto = new ChatRoomListDto();
        chatRoomListDto.setChatRoomDetails(collect);
        chatRoomListDto.setTotalNoReadChat(totalNoReadChat[0]);
        return new Result<>(chatRoomListDto);
    }

    @Data
    static class ChatRoomListDto {
        private Long totalNoReadChat;
        private List<ChatRoomDetailDto> chatRoomDetails;
    }



    @Data
    @AllArgsConstructor
    static public class Result<T> {
        private T data;
    }




    public static class Time {
        private static class TIME_MAXIMUM {
            public static final int SEC = 60;
            public static final int MIN = 60;
            public static final int HOUR = 24;
            public static final int DAY = 30;
            public static final int MONTH = 12;
        }
        public static String calculateTime(Date date) {
            long curTime = System.currentTimeMillis();
            long regTime = date.getTime();
            long diffTime = (curTime - regTime) / 1000;
            String msg = null;
            if (diffTime < TIME_MAXIMUM.SEC) {
                // sec
                msg = diffTime + "초 전";
            } else if ((diffTime /= TIME_MAXIMUM.SEC) < TIME_MAXIMUM.MIN) {
                // min
                msg = diffTime + "분 전";
            } else if ((diffTime /= TIME_MAXIMUM.MIN) < TIME_MAXIMUM.HOUR) {
                // hour
                msg = (diffTime) + "시간 전";
            } else if ((diffTime /= TIME_MAXIMUM.HOUR) < TIME_MAXIMUM.DAY) {
                // day
                msg = (diffTime) + "일 전";
            } else if ((diffTime /= TIME_MAXIMUM.DAY) < TIME_MAXIMUM.MONTH) {
                // day
                msg = (diffTime) + "달 전";
            } else {
                msg = (diffTime) + "년 전";
            }
            return msg;
        }
    }
}
