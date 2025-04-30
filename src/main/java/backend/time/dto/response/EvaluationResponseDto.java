package backend.time.dto.response;

import backend.time.model.Member.MannerEvaluationCategory;
import backend.time.model.Member.ServiceEvaluationCategory;
import backend.time.model.board.BoardCategory;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class EvaluationResponseDto {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class getEvaluationResponseDto {
        private List<MannerEvaluationDto> mannerEvaluationList;
        private List<ServiceEvaluationStarDto> serviceEvaluationStarDtoList; //별 개수(평점)
//    private List<ServiceEvaluationDto> serviceEvaluationList;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MannerEvaluationDto {
        @Enumerated(EnumType.STRING)
        private MannerEvaluationCategory mannerEvaluationCategory;

        private Integer mannerEvaluationCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceEvaluationStarDto {
        @Enumerated(EnumType.STRING)
        private BoardCategory boardCategory;

        private Integer starCount; //별 개수

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceEvaluationResponseDto {
        private List<ServiceEvaluationDto> serviceEvaluationList;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceEvaluationDto {
        @Enumerated(EnumType.STRING)
        private BoardCategory boardCategory;

        @Enumerated(EnumType.STRING)
        private ServiceEvaluationCategory serviceEvaluationCategory;

        private Integer serviceEvaluationCount;
    }
}
