package backend.time.controller;

import static backend.time.dto.response.NotificationResponseDto.*;

import backend.time.dto.ResponseDto;
import backend.time.model.Member.Member;
import backend.time.service.MemberServiceImpl;
import backend.time.service.NotificationService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final MemberServiceImpl memberService;
    public static Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();


    //프론트 쪽에서 사용자가 '로그인'을 하면, 해당 사용자를 sseEmitters 에 등록
    @GetMapping(value = "/api/notification/subscribe", produces = "text/event-stream")
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetails userDetails, HttpServletResponse response) {
        log.info("subscribe");
        String kakaoId = userDetails.getUsername();
        SseEmitter sseEmitter = notificationService.subscribe(kakaoId, response);
        log.info("returnEmitter = {}" , sseEmitter);
        return sseEmitter;
    }

    //활동알림 조회
    @GetMapping("/notification/activity")
    public ActivityNotificationDto activityNotification(@AuthenticationPrincipal UserDetails userDetails) {
        return notificationService.activityNotificationList(userDetails.getUsername());
    }

    //활동알림 삭제
    @DeleteMapping("/notification/activity/{id}")
    public ResponseDto<String> deleteActivityNotification(@AuthenticationPrincipal UserDetails userDetails, @PathVariable("id") Long id) {
        String userKakaoId = userDetails.getUsername();
        Member member = memberService.findMember(userKakaoId);
        if (!member.getId().equals(notificationService.findActivityNotificationById(id))) {
            throw new IllegalArgumentException("잘못된 접근입니다.");
        }
        notificationService.deleteActivityNotification(id);
        return new ResponseDto<String>(HttpStatus.OK.value(), "활동 알림 삭제 성공");
    }


    //키워드알림 조회
    @GetMapping("/notification/keyword")
    public KeywordNotificationDto keywordNotification(@AuthenticationPrincipal UserDetails userDetails) {
        return notificationService.keywordNotificationList(userDetails.getUsername());
    }

    //키워드알림 삭제
    @DeleteMapping("/notification/keyword/{id}")
    public ResponseDto<String> keywordNotificationDelete(@AuthenticationPrincipal UserDetails userDetails, @PathVariable("id") Long id) {
        String userKakaoId = userDetails.getUsername();
        Member member = memberService.findMember(userKakaoId);
        if (!member.getId().equals(notificationService.findMemberById(id))) {
            throw new IllegalArgumentException("잘못된 접근입니다.");
        }

        notificationService.deleteKeywordNotification(id);
        return new ResponseDto<String>(HttpStatus.OK.value(), "키워드 알림 삭제 성공");
    }
}
