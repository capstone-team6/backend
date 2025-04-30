package backend.time.dto.request;

import backend.time.model.Member.MannerEvaluationCategory;
import backend.time.model.Member.ServiceEvaluationCategory;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class MemberRequestDto {
    public static class EvaluationDto {
        public List<MannerEvaluationCategory> mannerEvaluationDtoList;

        public List<ServiceEvaluationCategory> serviceEvaluationDtoList;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class NicknameDto {
        String nickname;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TokenDto {
        String token;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnfinishedMemberDto {
        String kakaoId;
        String nickname;
    }
}
