package backend.time.controller;

import backend.time.dto.ChatDto;
import backend.time.dto.request.ChatRequestDto.RoomEnterDto;
import backend.time.dto.response.ChatResponseDto.ChatRoomResponseDto;
import backend.time.model.ChatImage;
import backend.time.model.ChatMessage;
import backend.time.model.ChatRoom;
import backend.time.model.ChatType;
import backend.time.model.Member.Member;
import backend.time.model.board.Board;
import backend.time.model.board.BoardType;
import backend.time.service.BoardService;
import backend.time.service.ChattingService;
import backend.time.service.MemberService;
import backend.time.service.NotificationService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/chat")
@Slf4j
public class ChatRoomController {

    private final ChattingService chattingService;
    private final MemberService memberService;
    private final BoardService boardService;
    private final NotificationService notificationService;
    private final EntityManager entityManager;

    /**
     * 채팅방 페이지 ('채팅하기'를 눌렀을 때)
     * 상품 상세페이지에서 넘겨받은 roomName값으로 채팅방을 찾고, 없으면 해당 roomName을 가지는 ChatRoom 객체를 생성하여 DB에 저장
     */
    @PostMapping("/room")
    public ChatRoomResponseDto enterPage(@AuthenticationPrincipal UserDetails userDetails, @RequestBody RoomEnterDto roomEnterDto){
        Member member = memberService.findMember(userDetails.getUsername()); //Member kakaoId로 member가져오기
        log.info("userKakaoId = {}", userDetails.getUsername());
//        이미 roomName이 이미 존재하는 채팅방이 있으면 가져오고 아니면 member(buyer, roomName, boardId)를 가지고 새로운 chatroom을 DB에 저장후 가져오기
        log.info("roomName = {}", roomEnterDto.getRoomName());
        log.info("boardId = {}", roomEnterDto.getBoardId());
        ChatRoom room = chattingService.findChatRoomByName(member, roomEnterDto.getRoomName(), roomEnterDto.getBoardId());
        Board board = boardService.findOne(roomEnterDto.getBoardId());
        String nickname = member.getNickname();
        String userKakaoId = userDetails.getUsername();
        Long boardId = board.getId();
        String roomName = room.getName();
        ChatRoomResponseDto chatRoomResponseDto = new ChatRoomResponseDto();

        String userType = "";
        String receiverKakaoId = "";
        //내가 BUYER인지 SELLER인지 판단
        if (board.getBoardType().equals(BoardType.BUY) && board.getMember().getKakaoId().equals(userKakaoId)) {
            chatRoomResponseDto.setRoleType("BUYER");
            log.info("BUYER1");
            userType = "BUYER";
            receiverKakaoId = room.getBuyer().getKakaoId();
        }else if(board.getBoardType().equals(BoardType.BUY)){
            chatRoomResponseDto.setRoleType("SELLER");
            log.info("SELLER1");
            userType = "SELLER";
            receiverKakaoId = board.getMember().getKakaoId();
        } else if (board.getBoardType().equals(BoardType.SELL) && board.getMember().getKakaoId().equals(userKakaoId)) {
            chatRoomResponseDto.setRoleType("SELLER");
            log.info("SELLER2");
            userType = "SELLER";
            receiverKakaoId = room.getBuyer().getKakaoId();
        } else if (board.getBoardType().equals(BoardType.SELL)) {
            chatRoomResponseDto.setRoleType("BUYER");
            log.info("BUYER2");
            userType = "BUYER";
            receiverKakaoId = board.getMember().getKakaoId();
        }

        ChatDto chatDto = new ChatDto();

        try {
            ChatMessage lastChat = room.getLastChat();
        } catch (Exception e) {
            if (userType.equals("BUYER")) {
                chatDto = ChatDto.builder()
                        .roomId(room.getId())
                        .boardId(boardId)
                        .message(nickname + "님이 입장하셨습니다.")
                        .writer(nickname)
                        .type(ChatType.JOIN)
                        .messageId(1L)
                        .buyerRead(1L)
                        .sellerRead(0L).build();
            } else if(userType.equals("SELLER")){
                chatDto = ChatDto.builder()
                        .roomId(room.getId())
                        .boardId(boardId)
                        .message(nickname + "님이 입장하셨습니다.")
                        .writer(nickname)
                        .type(ChatType.JOIN)
                        .messageId(1L)
                        .buyerRead(0L)
                        .sellerRead(1L).build();
            }

            Long savedChat = chattingService.saveChat(chatDto);
            notificationService.noReadChatNumberPerChatRoomNotification(userDetails.getUsername(), userType, savedChat, receiverKakaoId);
        }

        Long roomId = room.getId();
        notificationService.whenEnterChatRoomNotificiation(userType, roomId, userKakaoId);
        List<ChatMessage> chatMessageList = new ArrayList<>();
        ChatMessage chatMessage = new ChatMessage();
        chatMessageList.add(chatMessage);

//        List<ChatMessage> chatList = (chattingService.findChatList(room.getId())
//                .orElseGet(() -> null));

        List<ChatDto> collect = new ArrayList<>();
        if (chattingService.findChatList(room.getId()).isEmpty()) {
            List<ChatMessage> chatList = new ArrayList<>();
            ChatMessage CM = new ChatMessage();
            chatList.add(CM);
        } else {
            List<ChatMessage> chatList = chattingService.findChatList(room.getId());
            log.info("chatList.size() = {}", chatList.size());
            collect = chatList.stream()
                    .map(m -> {
                        ChatDto cm = new ChatDto();
                        cm.setRoomId(m.getChatRoom().getId());
                        cm.setMessage(m.getMessage());
                        cm.setWriter(m.getWriter());
                        cm.setType(m.getType());
                        if (!(m.getChatImages() == null || m.getChatImages().isEmpty())) {
                            List<ChatImage> chatImages = m.getChatImages();

                            List<String> collect2 = chatImages.stream().map(c -> {
                                String images = "";
                                images += c.getStoredFileName();

                                return images;
                            }).collect(Collectors.toList());
                            cm.setImage(collect2);
//                            for (ChatImage chatImage : chatImages) {
//                                cm.setImage(chatImage.getStoredFileName());
//                            }
                        }
                        cm.setTime(m.getCreateDate().getHours() + ":" + m.getCreateDate().getMinutes());
//                    cm.setLocalDateTime(m.getCreateDate().toLocalDateTime());
                        return cm;
                    }).collect(Collectors.toList());


        }



        String otherChatPersonKakaoId = "";
        //상대방 Id
        if (room.getBuyer().getKakaoId().equals(userKakaoId)) {
            otherChatPersonKakaoId = room.getBoard().getMember().getKakaoId();
        } else {
            otherChatPersonKakaoId = room.getBuyer().getKakaoId();
        }
        Long otherChatPersonId = memberService.findMember(otherChatPersonKakaoId).getId();

        chatRoomResponseDto.setChatlist(collect);
        chatRoomResponseDto.setRoomId(roomId);
        chatRoomResponseDto.setOtherUserId(otherChatPersonId);
        chatRoomResponseDto.setNickName(nickname);
        chatRoomResponseDto.setBoardId(boardId);
        chatRoomResponseDto.setRoomName(roomName);

        return chatRoomResponseDto;
    }

}
