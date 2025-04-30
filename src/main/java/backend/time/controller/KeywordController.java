package backend.time.controller;

import backend.time.dto.request.KeywordRequestDto;
import backend.time.dto.ResponseDto;
import backend.time.model.Keyword;
import backend.time.model.Member.Member;
import backend.time.service.KeywordService;
import backend.time.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
public class KeywordController {

    private final KeywordService keywordService;
    private final MemberService memberService;

    @PostMapping("/keyword")
    public ResponseDto<String> keyword(@AuthenticationPrincipal UserDetails userDetails, @RequestBody KeywordRequestDto keywordDto) {
        log.info("keyword = {}", userDetails.getUsername());
        log.info("keyword = {}", keywordDto.getKeyword());
        String keyword = keywordService.save(keywordDto, userDetails.getUsername());
        log.info("keyword = {}", keyword);
        //중복이 있을때도 생각
        return new ResponseDto<String>(HttpStatus.OK.value(), keyword);
    }

    @GetMapping("/keyword")
    public KeywordResponseDto keywordList(@AuthenticationPrincipal UserDetails userDetails) {
        String kakaoId = userDetails.getUsername();
        Member member = memberService.findMember(kakaoId);
        Long memberId = member.getId();

        List<Keyword> keywordList = keywordService.findByMemberId(memberId);

        List<KeywordResponse> collect = keywordList.stream()
                .map(m -> {
                    KeywordResponse dto = new KeywordResponse();
                    dto.setId(m.getId());
                    dto.setMemberId(m.getMember().getId());
                    dto.setKeyword(m.getKeyword());

                    return dto;
                }).collect(Collectors.toList());

        KeywordResponseDto keywordResponseDto = new KeywordResponseDto();
        keywordResponseDto.setKeywordResponses(collect);

        return keywordResponseDto;
    }

    @DeleteMapping("/keyword/{id}")
    public ResponseDto<String> deleteKeyword(@PathVariable("id") Long id) {
        log.info("id = {}", id);
        keywordService.deleteKeword(id);
        return new ResponseDto<String>(HttpStatus.OK.value(), "키워드 삭제 완료");
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeywordIdDto {
        private Long keywordId;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class KeywordResponseDto {
        List<KeywordResponse> keywordResponses;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class KeywordResponse{
        private Long id;
        private Long memberId;
        private String keyword;
    }
}
