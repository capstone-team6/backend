package backend.time.service;

import backend.time.dto.ChatDto;
import backend.time.model.ChatMessage;
import backend.time.model.ChatRoom;
import backend.time.model.ChatType;
import backend.time.model.Member.Member;
import backend.time.model.board.Board;
import backend.time.repository.BoardRepository;
import backend.time.repository.ChatRepository;
import backend.time.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChattingServiceImpl implements ChattingService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRepository chatRepository;
    private final BoardRepository boardRepository;

    @Override
    public List<ChatRoom> findChatRoomByMember(String kakaoId) {
        return chatRoomRepository.findChatRoomByMember(kakaoId);
    }

    @Override
    public ChatRoom findChatRoomByBuyer(Long boardId, Long buyerId) {
        return chatRoomRepository.findByBoardIdAndBuyerId(boardId, buyerId)
                .orElseGet(() -> new ChatRoom());
    }

    @Override
    @Transactional
    public Long saveChat(ChatDto chatDto) {
        log.info("1");
        if (chatDto.getType().equals(ChatType.JOIN)) {
            Board board = boardRepository.findById(chatDto.getBoardId())
                    .orElseThrow(() -> new IllegalArgumentException("해당글이 존재하지 않습니다."));

            board.setChatCount(board.getChatCount()+ 1);
        }
        ChatRoom chatRoom = chatRoomRepository.findById(chatDto.getRoomId()).get(); //chatDto의 room_id로 repository에 저장되어있는 chatRoom가져오기
        log.info("2");
        ChatMessage chat = chatDto.toEntity(chatRoom); //chatDto에 정보를 가지고 리포지토리에서 찾은 chatRoom을 같이 엔티티로 전환하여 chat반환
        log.info("chat.getType = {}", chat.getType().toString());
        chatRepository.save(chat); //DB에 chat저장(spring data jpa)
        return chat.getId();
    }

    @Override
    public List<ChatMessage> findChatList(Long roomId) {
        return chatRepository.findByChatRoomId(roomId);
    }

    @Override
    @Transactional
    public ChatRoom findChatRoomByName(Member member, String roomName, Long boardId) {
        Optional<ChatRoom> findChatRoom = chatRoomRepository.findByName(roomName); //UUID랜덤으로 생성한 방이름?으로 ChatRoom 가져옴
        Board board = boardRepository.findById(boardId).get(); //게시판 아이디로 게시판 가져옴
        if (!findChatRoom.isPresent()) { //만약 가져온 ChatRoom이 존재하지않으면 새로운 채팅방으로 인식해서 DB에 저장
            ChatRoom newChatRoom = ChatRoom.builder()
                    .roomName(roomName)
                    .buyer(member)
                    .board(board).build();
            chatRoomRepository.save(newChatRoom);
            return newChatRoom;
        }
        return findChatRoom.get(); //만약 이미 생성된방이 있으면 그방을 return
    }

    @Override
    public Optional<ChatRoom> findChatRoomById(Long roomId) {
        return chatRoomRepository.findById(roomId);
    }

    @Override
    public ChatMessage findChatMessageById(Long chatMessageId) {
        return chatRepository.findChatMessageById(chatMessageId);
    }

    @Override //@Transaction??
    @Transactional
    public void saveUserTypeReadId(String userType, Long roomId) {
        Optional<ChatRoom> chatRoom = chatRoomRepository.findById(roomId);
        try {
            ChatMessage lastChat = chatRoom.get().getLastChat();
            if (userType.equals("SELLER")) {
                lastChat.setSellerRead(lastChat.getMessageId());
            } else {
                lastChat.setBuyerRead(lastChat.getMessageId());
            }
        } catch (Exception e) {
            log.info("첫 채팅없음");
        }
    }
}
