package backend.time.controller;

import backend.time.config.auth.PrincipalDetail;
import backend.time.config.auth.PrincipalDetailService;
import backend.time.config.jwt.JwtTokenUtil;
import backend.time.dto.*;
import backend.time.model.Member;
import backend.time.service.MemberService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@RequiredArgsConstructor
@RestController
@PropertySource(value={"application-mysql.properties"})
public class MemberApiController {
    private final MemberService memberService;

    @Autowired
    private PrincipalDetailService principalDetailService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private final StringRedisTemplate redisTemplate;

 // 프론트 없이 토큰 받아올 때 쓴 거
/*   @GetMapping("/oauth/kakao")
    public ResponseDto ex1(@RequestParam(value = "code") String code){
        System.out.println("token "+code);
        String token = memberService.getReturnAccessToken(code);

        Map<String, Object> data = new HashMap<>();
        data.put("token",token);
        return new ResponseDto(HttpStatus.OK.value(), data);
    }*/

    //카카오에서 사용자 정보 갖고오기
    @PostMapping("kakao/getinfo")
    public ResponseDto getInfo(@RequestBody TokenDto token) {
        Map<String, Object> data = new HashMap<>();
        System.out.println("token " + token.getToken());
        Member member = memberService.getUserInfo(token.getToken());
        if(member == null){
            return new ResponseDto(HttpStatus.FORBIDDEN.value(), "잘못된 토큰입니다.");
        }
        else{
            data.put("kakaoId",member.getKakaoId());
            return new ResponseDto(HttpStatus.OK.value(), data);
        }
    }

    @PutMapping("/nickname/change")
    public ResponseDto changeName(@AuthenticationPrincipal PrincipalDetail principalDetail,@RequestBody @Valid NicknameDto nicknameDto) {
        Boolean isChange = memberService.changeNickname(principalDetail.getMember(), nicknameDto.getNickname());
        Map<String,Object> data = new HashMap<>();
        data.put("isChange",isChange);
        if(isChange){
            return new ResponseDto(HttpStatus.OK.value(), data);
        }
        else{
            return new ResponseDto(HttpStatus.FORBIDDEN.value(), data);
        }
    }

    @DeleteMapping("/delete/member")
    public ResponseDto deleteMember(@AuthenticationPrincipal PrincipalDetail principalDetail){
        memberService.deleteMember(principalDetail.getMember());
        Map<String,Object> data = new HashMap<>();
        data.put("isDelete",true);

        return new ResponseDto(HttpStatus.OK.value(), data);

    }


    // 회원 가입 완료 버튼 눌렀을 때 (위치 미포함)
    @PutMapping("/sign-up")
    public ResponseDto saveMember(@RequestBody @Valid UnfinishedMemberDto unfinishedMemberDto) throws Exception{
        Map<String, Object> data = new HashMap<>();
        memberService.saveMember(unfinishedMemberDto.getKakaoId(), unfinishedMemberDto.getNickname());

        data.put("SignUpSuccess", true);
        return new ResponseDto(HttpStatus.OK.value(), data);
    }


    // 중복 검사 버튼 눌렀을 때
    @PostMapping("/sign-up/nicknameCheck") //nickname/nicknameCheck로 바꾸고싶
    public ResponseDto nicknameDuplicated(@RequestBody @Valid NicknameDto nicknameDto){
        Map<String, Object> data = new HashMap<>();
        if(memberService.isNicknameDuplicated(nicknameDto.getNickname())){ //중복됨
            data.put("success",false);
            return new ResponseDto(HttpStatus.METHOD_NOT_ALLOWED.value(), data);
        }
        else{
            data.put("success",true);
            return new ResponseDto(HttpStatus.OK.value(), data);

        }
    }


    @Data
    @AllArgsConstructor
    static class Result<T> {
        private T data;
    }

    //리프레시 토큰으로 엑세스 토큰 재발급

}
