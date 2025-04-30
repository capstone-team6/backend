package backend.time.controller;

import backend.time.config.auth.PrincipalDetail;
import backend.time.dto.request.ObjectionRequestDto;
import backend.time.dto.response.ObjectionResponseDto;
import backend.time.dto.ResponseDto;
import backend.time.service.ObjectionService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
public class ObjectionApiController {
    private final ObjectionService objectionService;
    //이의신청
    @PostMapping("/post/objection") //이름 바꾸기
    public ResponseDto<Map<String,Object>> createObjection(@AuthenticationPrincipal PrincipalDetail principalDetail, @ModelAttribute @Valid ObjectionRequestDto objectionDto) throws IOException {
        objectionService.createObjection(principalDetail.getMember(), objectionDto);
        Map<String, Object> data = new HashMap<>();
        data.put("objection",true);

        return new ResponseDto<>(HttpStatus.OK.value(), data);
    }
    //이의신청 목록
    @GetMapping("/objection")
    public Result<Object> getObjection(@AuthenticationPrincipal PrincipalDetail principalDetail){
//        Page<ObjectionDto> objectionPage = objectionService.getObjections(principalDetail.getMember().getId());
        List<ObjectionResponseDto> objectionResponseDtoList = objectionService.getObjections(principalDetail.getMember().getId());
        return new Result<>(objectionResponseDtoList);

    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private T data;
    }
}
