package backend.time.controller;

import backend.time.config.auth.PrincipalDetail;
import backend.time.dto.BoardDistanceDto;
import backend.time.dto.ResponseDto;
import backend.time.dto.request.ScrapDto;
import backend.time.dto.response.BoardResponseDto.BoardListResponseDto;
import backend.time.model.Scrap;
import backend.time.model.board.Board;
import backend.time.service.ScrapService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
public class ScrapApiController {
    private final ScrapService scrapService;

    // 스크랩 하기 & 스크랩 취소
    @PostMapping("api/board/{id}/scrap")
    public ResponseDto scrap(@AuthenticationPrincipal PrincipalDetail principalDetail, @PathVariable("id") Long id) {
        Boolean isScrap = scrapService.doScrap(principalDetail.getMember(), id);
        Map<String, Object> data = new HashMap<>();
        if (isScrap) {
            data.put("isScrap", true);
            return new ResponseDto(HttpStatus.OK.value(), data);
        } else {
            data.put("isScrap", false);
            return new ResponseDto(HttpStatus.OK.value(), data);
        }
    }

    // 스크랩 목록 가져오기
    @GetMapping("api/scrap-list")
    public Result scrapList(@ModelAttribute @Valid ScrapDto scrapDto,
                            @AuthenticationPrincipal PrincipalDetail principalDetail) {
        Page<Scrap> scraps = scrapService.getScrapList(scrapDto, principalDetail.getMember());

        List<Board> boards = scraps.stream()
                .map(Scrap::getBoard)
                .toList();

        List<BoardDistanceDto> boardDistanceDtos = boards.stream().map(board -> {
            if (board.getAddress() != null) {
                double distance = calculateDistanceInKm(principalDetail.getMember().getLatitude(),
                        principalDetail.getMember().getLongitude(), board.getLatitude(), board.getLongitude());
                return new BoardDistanceDto(board.getId(), Math.round(distance * 10) / 10.0);
            } else {
                return new BoardDistanceDto(board.getId(), 0D);
            }
        }).toList();

        // id를 key로 distance를 값으로 매핑
        Map<Long, Double> boardIdToDistanceMap = boardDistanceDtos.stream()
                .collect(Collectors.toMap(BoardDistanceDto::getId, BoardDistanceDto::getDistance));

        List<BoardListResponseDto> collect = boards.stream().map(board -> {
            BoardListResponseDto dto = getBoardListResponseDto(board,
                    boardIdToDistanceMap);

            return dto;
        }).collect(Collectors.toList());

        return new Result(collect);
    }

    private static BoardListResponseDto getBoardListResponseDto(Board board, Map<Long, Double> boardIdToDistanceMap) {
        BoardListResponseDto dto = BoardListResponseDto.builder()
                .boardId(board.getId())
                .title(board.getTitle())
                .itemTime(board.getItemTime())
                .itemPrice(board.getItemPrice())
                .createdDate(board.getCreateDate())
                .chatCount(board.getChatCount())
                .scrapCount(board.getScrapCount())
                .boardState(board.getBoardState())
                .distance(boardIdToDistanceMap.get(board.getId()))
                .address(board.getAddress() == null ? null : board.getAddress())
                .firstImage(board.getImages().isEmpty() ? null : board.getImages().get(0).getStoredFileName())
                .build();
        return dto;
    }

    double calculateDistanceInKm(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS_KM = 6371.01;
        // 위도, 경도를 라디안으로 변환
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // Haversine 공식
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.pow(Math.sin(dLon / 2), 2)
                * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private T data;
    }
}
