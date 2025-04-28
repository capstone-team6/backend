package backend.time.controller;

import backend.time.config.auth.PrincipalDetail;
import backend.time.dto.ResponseDto;
import backend.time.dto.request.ScrapDto;
import backend.time.dto.response.BoardResponseDto.ScrapListResponseDto;
import backend.time.repository.ScrapRepository;
import backend.time.service.ScrapService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
public class ScrapApiController {
    private final ScrapService scrapService;
    private final ScrapRepository scrapRepository;

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

    @GetMapping("api/scrap-list")
    public Result scrapList(@ModelAttribute @Valid ScrapDto scrapDto,
                            @AuthenticationPrincipal PrincipalDetail principalDetail) {
        Pageable pageable = PageRequest.of(scrapDto.getPageNum(), 8, Sort.by(Sort.Direction.DESC, "createDate"));
        Page<ScrapListResponseDto> collect = scrapRepository.getScrapList(pageable,
                principalDetail.getMember().getId(), principalDetail.getMember()
                        .getLocation());

        return new Result(collect);
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private T data;
    }
}
