package backend.time.controller;

import backend.time.config.auth.PrincipalDetail;
import backend.time.dto.ResponseDto;
import backend.time.dto.request.*;
import backend.time.dto.response.BoardResponseDto;
import backend.time.dto.response.BoardResponseDto.AccountResponseDto;
import backend.time.dto.response.BoardResponseDto.BoardDetailResponseDto;
import backend.time.dto.response.BoardResponseDto.BoardListResponseDto;
import backend.time.dto.response.BoardResponseDto.BoardSearchHaversine;
import backend.time.dto.response.BoardResponseDto.BoardSearchSpatial;
import backend.time.dto.response.BoardResponseDto.UserAddressResponseDto;
import backend.time.dto.response.BoardResponseDto.WhoResponseDto;
import backend.time.model.Scrap;
import backend.time.model.board.*;
import backend.time.repository.BoardRepository;
import backend.time.repository.ScrapRepository;
import backend.time.service.BoardService;
import backend.time.service.ChattingService;
import backend.time.service.NotificationService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class BoardApiController {

    private final BoardService boardService;
    private final BoardRepository boardRepository;
    private final ScrapRepository scrapRepository;
    private final ChattingService chattingService;
    private final NotificationService notificationService;

    @PostMapping("/api/auth/point")
    public ResponseDto<String> addPoint(@RequestBody @Valid PointDto pointDto) throws IOException {
        boardService.addPoint(pointDto);
        return new ResponseDto<>(HttpStatus.OK.value(), "위치 설정 성공");
    }

    @PostMapping("/api/auth/board")
    public ResponseDto<String> writeBoard(@ModelAttribute @Valid BoardDto boardDto,
                                          @AuthenticationPrincipal PrincipalDetail principalDetail) throws IOException {
        Long boardId = boardService.write(boardDto, principalDetail.getMember());
        notificationService.keywordNotification(boardId);
        return new ResponseDto<>(HttpStatus.OK.value(), "게시글 작성 완료");
    }

    @GetMapping("/api/board")
    public ResponseDto<BoardResponseWrapperSpatial> findAll(@ModelAttribute @Valid BoardSearchDto requestDto,
                                                            @AuthenticationPrincipal PrincipalDetail principalDetail) {
        Pageable pageable = PageRequest.of(requestDto.getPageNum(), 8);
        List<BoardResponseDto.BoardSearchSpatial> boardDistanceDtos = boardRepository.searchBoardsSpatialNative(
                requestDto, principalDetail.getMember().getLocation(), pageable);
        UserAddressResponseDto userAddressResponseDto = UserAddressResponseDto.builder()
                .userLatitude(principalDetail.getMember().getLatitude())
                .userLongitude(principalDetail.getMember().getLongitude())
                .address(principalDetail.getMember().getAddress())
                .build();
        BoardResponseWrapperSpatial responseWrapper = new BoardResponseWrapperSpatial();
        responseWrapper.setUserAddress(userAddressResponseDto);
        responseWrapper.setBoards(boardDistanceDtos);
        return new ResponseDto<>(HttpStatus.OK.value(), responseWrapper);
    }

    @GetMapping("/api/board/{id}")
    public ResponseDto<BoardDetailResponseDto> boardDetail(@PathVariable("id") Long id,
                                                           @AuthenticationPrincipal PrincipalDetail principalDetail) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 글이 존재하지 않습니다."));
        String scrapStus = "YES";
        String who = "writer";
        String roomName = null;
        Optional<Scrap> scrap = scrapRepository.findByMemberIdAndBoardId(principalDetail.getMember().getId(), id);
        if (scrap.isEmpty()) {
            scrapStus = "NO";
        }

        if (!Objects.equals(board.getMember().getId(), principalDetail.getMember().getId())) {
            who = "reader";
            roomName = chattingService.findChatRoomByBuyer(board.getId(), principalDetail.getMember().getId())
                    .getName();
        }

        List<Image> images = board.getImages();
        List<String> collect = images.stream().map(Image::getStoredFileName)
                .toList();

        BoardDetailResponseDto boardDetailResponseDto = getBoardDetailResponseDto(
                board, scrapStus, who, roomName, collect);

        return new ResponseDto<>(HttpStatus.OK.value(), boardDetailResponseDto);
    }

    @PutMapping("/api/auth/board/{id}")
    public ResponseDto<String> updateBoard(@PathVariable("id") Long id,
                                           @ModelAttribute @Valid BoardUpdateDto boardUpdateDto,
                                           @AuthenticationPrincipal PrincipalDetail principalDetail)
            throws IOException {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 글이 존재하지 않습니다."));

        if (!Objects.equals(principalDetail.getMember().getId(), board.getMember().getId())) {
            throw new IllegalArgumentException("잘못된 접근입니다.");
        }

        boardService.update(id, boardUpdateDto);
        return new ResponseDto<>(HttpStatus.OK.value(), "게시글 수정 완료");
    }

    @DeleteMapping("/api/auth/board/{id}")
    public ResponseDto<String> deleteBoard(@PathVariable("id") Long id,
                                           @AuthenticationPrincipal PrincipalDetail principalDetail)
            throws IOException {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 글이 존재하지 않습니다."));

        if (!Objects.equals(principalDetail.getMember().getId(), board.getMember().getId())) {
            throw new IllegalArgumentException("잘못된 접근입니다.");
        }

        boardService.delete(id);
        return new ResponseDto<>(HttpStatus.OK.value(), "게시글 삭제 완료");
    }

    //<------------------채팅 버튼 별 board 상태 변경-------------------->
    @PostMapping("api/board/{boardId}/chat/{chatId}/pay")
    public ResponseDto choosePayMeth(@RequestBody PayMethDto paymethdto, @PathVariable("boardId") Long boardId,
                                     @PathVariable("chatId") Long chatId,
                                     @AuthenticationPrincipal PrincipalDetail principalDetail) throws IOException {
        boardService.choosePayMeth(paymethdto, boardId, chatId, principalDetail.getMember());
        return new ResponseDto<>(HttpStatus.OK.value(), "거래중으로 변경 됨");
    }

    @PutMapping("api/board/{boardId}/chat/{chatId}/cancel")
    public ResponseDto cancelTrade(@PathVariable("boardId") Long boardId, @PathVariable("chatId") Long chatId)
            throws IOException {
        boardService.cancelTrade(boardId, chatId);
        return new ResponseDto<>(HttpStatus.OK.value(), "판매중으로 변경 됨");
    }

    @PutMapping("api/board/{boardId}/chat/{chatId}/complete")
    public ResponseDto completeTrade(@PathVariable("boardId") Long boardId, @PathVariable("chatId") Long chatId)
            throws IOException {
        boardService.completeTrade(boardId, chatId);
        notificationService.transactionComplete(chatId);
        return new ResponseDto<>(HttpStatus.OK.value(), "판매완료로 변경 됨");
    }

    //seller - 글 작성자, buyer - 상대방
    @GetMapping("/api/board/{boardId}/chat/{chatId}/who")
    public ResponseDto<WhoResponseDto> getRole(@PathVariable("boardId") Long boardId,
                                               @PathVariable("chatId") Long chatId,
                                               @AuthenticationPrincipal PrincipalDetail principalDetail)
            throws IOException {
        WhoResponseDto whoResponseDto = boardService.getRole(boardId, chatId, principalDetail.getMember());
        return new ResponseDto<>(HttpStatus.OK.value(), whoResponseDto);
    }

    @GetMapping("/api/board/{boardId}/chat/{chatId}/account")
    public ResponseDto<AccountResponseDto> getAccount(@PathVariable("chatId") Long chatId) {
        AccountResponseDto dto = boardService.getAccount(chatId);
        return new ResponseDto<>(HttpStatus.OK.value(), dto);
    }

    //<------------------거래한 글, 구매한 글-------------------->
    @GetMapping("users/{userId}/boards/write")
    public ResponseDto<List<BoardListResponseDto>> writeList(@PathVariable("userId") Long userId) {
        List<Board> boards = boardService.writeList(userId);

        List<BoardListResponseDto> collect = boards.stream().map(BoardApiController::getBoardListResponseDto)
                .collect(Collectors.toList());

        return new ResponseDto<>(HttpStatus.OK.value(), collect);
    }

    @GetMapping("users/{userId}/boards/trade")
    public ResponseDto<List<BoardListResponseDto>> tradeList(@PathVariable("userId") Long userId) {
        List<Board> boards = boardService.tradeList(userId);

        List<BoardListResponseDto> collect = boards.stream().map(BoardApiController::getBoardListResponseDto)
                .collect(Collectors.toList());

        return new ResponseDto<>(HttpStatus.OK.value(), collect);
    }

    private static BoardListResponseDto getBoardListResponseDto(Board board) {
        BoardListResponseDto dto = BoardListResponseDto.builder()
                .boardId(board.getId())
                .title(board.getTitle())
                .itemTime(board.getItemTime())
                .itemPrice(board.getItemPrice())
                .createdDate(board.getCreateDate())
                .chatCount(board.getChatCount())
                .scrapCount(board.getScrapCount())
                .boardState(board.getBoardState())
                .boardType(board.getBoardType())
                .address(board.getAddress() == null ? null : board.getAddress())
                .firstImage(board.getImages().isEmpty() ? null : board.getImages().get(0).getStoredFileName())
                .build();

        return dto;
    }

    private BoardDetailResponseDto getBoardDetailResponseDto(Board board, String scrapStus, String who, String roomName,
                                                             List<String> collect) {
        return BoardDetailResponseDto.builder()
                .boardId(board.getId())
                .scrapStus(scrapStus)
                .who(who)
                .roomName(roomName)
                .userId(board.getMember().getId())
                .nickname(board.getMember().getNickname())
                .mannerTime(board.getMember().getMannerTime())
                .title(board.getTitle())
                .content(board.getContent())
                .itemPrice(board.getItemPrice())
                .itemTime(board.getItemTime())
                .createdDate(board.getCreateDate())
                .chatCount(board.getChatCount())
                .scrapCount(board.getScrapCount())
                .address(board.getAddress())
                .longitude(board.getLongitude())
                .latitude(board.getLatitude())
                .boardState(board.getBoardState())
                .category(board.getCategory())
                .boardType(board.getBoardType())
                .images(collect)
                .build();
    }

    @Data
    public class BoardResponseWrapperHaversine {
        private UserAddressResponseDto userAddress;
        private Page<BoardSearchHaversine> boards;
    }

    @Data
    public class BoardResponseWrapperSpatial {
        private UserAddressResponseDto userAddress;
        private List<BoardSearchSpatial> boards;
    }
}
