package backend.time.controller;

import backend.time.config.auth.PrincipalDetail;
import backend.time.dto.BoardDistanceDto;
import backend.time.dto.ResponseDto;
import backend.time.dto.request.*;
import backend.time.dto.response.BoardResponseDto.AccountResponseDto;
import backend.time.dto.response.BoardResponseDto.BoardDetailResponseDto;
import backend.time.dto.response.BoardResponseDto.BoardListResponseDto;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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

    //user 위치 넣기
    @PostMapping("/api/auth/point")
    public ResponseDto<String> addPoint(@RequestBody @Valid PointDto pointDto) throws IOException {
        boardService.point(pointDto);
        return new ResponseDto<String>(HttpStatus.OK.value(), "위치 설정 성공");
    }

    //게시글 작성
    @PostMapping("/api/auth/board")
    public ResponseDto<String> writeBoard(@ModelAttribute @Valid BoardDto boardDto,
                                          @AuthenticationPrincipal PrincipalDetail principalDetail) throws IOException {
        Long boardId = boardService.write(boardDto, principalDetail.getMember());
        notificationService.keywordNotification(boardId);
        return new ResponseDto<String>(HttpStatus.OK.value(), "게시글 작성 완료");
    }

    //글 조회(검색)
    @GetMapping("/api/board")
    public Result findAll(@ModelAttribute @Valid BoardSearchDto requestDto,
                          @AuthenticationPrincipal PrincipalDetail principalDetail) {
        Page<Board> boards = boardService.searchBoards(requestDto, principalDetail.getMember());
        // BoardDistanceDto 리스트를 생성
        List<BoardDistanceDto> boardDistanceDtos = boardRepository.findNearbyOrUnspecifiedLocationBoardsWithDistance(
                principalDetail.getMember().getLongitude(), principalDetail.getMember().getLatitude());
        // id를 key로 distance를 값으로 매핑
        Map<Long, Double> boardIdToDistanceMap = boardDistanceDtos.stream()
                .collect(Collectors.toMap(BoardDistanceDto::getId, BoardDistanceDto::getDistance));

        BoardResponseWrapper responseWrapper = getBoardResponseWrapper(
                principalDetail, boards, boardIdToDistanceMap);

        return new Result<>(responseWrapper);
    }

    private BoardResponseWrapper getBoardResponseWrapper(PrincipalDetail principalDetail, Page<Board> boards,
                                                         Map<Long, Double> boardIdToDistanceMap) {
        UserAddressResponseDto userAddressResponseDto = UserAddressResponseDto.builder()
                .userLatitude(principalDetail.getMember().getLatitude())
                .userLongitude(principalDetail.getMember().getLongitude())
                .address(principalDetail.getMember().getAddress())
                .build();

        List<BoardListResponseDto> collect = boards.getContent().stream().map(board -> {
            BoardListResponseDto dto = BoardListResponseDto.builder()
                    .boardId(board.getId())
                    .title(board.getTitle())
                    .itemTime(board.getItemTime())
                    .itemPrice(board.getItemPrice())
                    .createdDate(board.getCreateDate())
                    .chatCount(board.getChatCount())
                    .scrapCount(board.getScrapCount())
                    .boardState(board.getBoardState())
                    .distance(boardIdToDistanceMap.getOrDefault(board.getId(), null))
                    .address(board.getAddress() == null ? null : board.getAddress())
                    .firstImage(board.getImages().isEmpty() ? null : board.getImages().get(0).getStoredFileName())
                    .build();

            return dto;
        }).collect(Collectors.toList());

        BoardResponseWrapper responseWrapper = new BoardResponseWrapper();
        responseWrapper.setUserAddress(userAddressResponseDto);
        responseWrapper.setBoards(collect);
        return responseWrapper;
    }

    //글 상세보기
    @GetMapping("/api/board/{id}")
    public Result boardDetail(@PathVariable("id") Long id, @AuthenticationPrincipal PrincipalDetail principalDetail) {
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

        return new Result<>(boardDetailResponseDto);
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

    //게시글 수정
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
        return new ResponseDto<String>(HttpStatus.OK.value(), "게시글 수정 완료");
    }

    //게시글 삭제
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
        return new ResponseDto<String>(HttpStatus.OK.value(), "게시글 삭제 완료");
    }

    //<------------------채팅 버튼 별 board 상태 변경-------------------->
    //결제 방법 선택
    @PostMapping("api/board/{boardId}/chat/{chatId}/pay")
    public ResponseDto payMeth(@RequestBody PayMethDto paymethdto, @PathVariable("boardId") Long boardId,
                               @PathVariable("chatId") Long chatId,
                               @AuthenticationPrincipal PrincipalDetail principalDetail) throws IOException {
        boardService.payMeth(paymethdto, boardId, chatId, principalDetail.getMember());
        return new ResponseDto<String>(HttpStatus.OK.value(), "거래중으로 변경 됨");
    }

    //거래 취소
    @PutMapping("api/board/{boardId}/chat/{chatId}/cancel")
    public ResponseDto cancel(@PathVariable("boardId") Long boardId, @PathVariable("chatId") Long chatId)
            throws IOException {
        boardService.cancel(boardId, chatId);
        //틈새페이는 다시 환불해주는 로직 작성해야함
        return new ResponseDto<String>(HttpStatus.OK.value(), "판매중으로 변경 됨");
    }

    //거래 완료 틈새페이 상대방에게 이동
    @PutMapping("api/board/{boardId}/chat/{chatId}/complete")
    public ResponseDto complete(@PathVariable("boardId") Long boardId, @PathVariable("chatId") Long chatId)
            throws IOException {
        boardService.complete(boardId, chatId);
        notificationService.transactionComplete(chatId);
        //틈새페이는 다시 환불해주는 로직 작성해야함
        return new ResponseDto<String>(HttpStatus.OK.value(), "판매완료로 변경 됨");
    }

    //로그인한사람이 seller/buyer 인지 식별
    @GetMapping("/api/board/{boardId}/chat/{chatId}/who")
    public Result who(@PathVariable("boardId") Long boardId, @PathVariable("chatId") Long chatId,
                      @AuthenticationPrincipal PrincipalDetail principalDetail) throws IOException {
        WhoResponseDto whoResponseDto = boardService.buyWho(boardId, chatId, principalDetail.getMember());
        return new Result<>(whoResponseDto);
    }

    //계좌 내보내기
    @GetMapping("/api/board/{boardId}/chat/{chatId}/account")
    public Result getAccount(@PathVariable("boardId") Long boardId, @PathVariable("chatId") Long chatId) {
        AccountResponseDto dto = boardService.getAccount(boardId, chatId);
        return new Result<>(dto);
    }

    //<------------------거래한 글, 구매한 글-------------------->
    //작성한 내역(판매글, 구매글)
    @GetMapping("users/{userId}/boards/write")
    public Result writeList(@PathVariable("userId") Long userId) {
        List<Board> boards = boardService.writeList(userId);

        List<BoardListResponseDto> collect = boards.stream().map(BoardApiController::getBoardListResponseDto)
                .collect(Collectors.toList());

        return new Result(collect);
    }

    //거래한 내역
    @GetMapping("users/{userId}/boards/trade")
    public Result tradeList(@PathVariable("userId") Long userId) {
        List<Board> boards = boardService.tradeList(userId);

        List<BoardListResponseDto> collect = boards.stream().map(BoardApiController::getBoardListResponseDto)
                .collect(Collectors.toList());

        return new Result(collect);
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

    @Data
    public class BoardResponseWrapper {
        private UserAddressResponseDto userAddress;
        private List<BoardListResponseDto> boards;
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private T data;
    }
}
