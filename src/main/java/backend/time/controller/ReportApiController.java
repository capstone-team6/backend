package backend.time.controller;

import backend.time.config.auth.PrincipalDetail;
import backend.time.dto.ResponseDto;
import backend.time.dto.request.ReportRequestDto;
import backend.time.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
public class ReportApiController {
    private final ReportService reportService;
    @PostMapping("/board/{boardId}/report")
    public ResponseDto<Map<String, Object>> postReport(@AuthenticationPrincipal PrincipalDetail principalDetail, @PathVariable("boardId") Long boardId,@RequestBody @Valid ReportRequestDto reportDto){
        boolean isOk = reportService.postReport(principalDetail.getMember().getId(), boardId, reportDto);
        Map<String, Object> data = new HashMap<>();
        data.put("report",isOk);
        return new ResponseDto<>(HttpStatus.OK.value(), data);
    }
}
