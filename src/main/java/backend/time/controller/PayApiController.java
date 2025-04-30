package backend.time.controller;

import backend.time.config.auth.PrincipalDetail;
import backend.time.dto.request.PayRequestDto.PostPrepareDto;
import backend.time.dto.request.PayRequestDto.VerifyAndChargeDto;
import backend.time.dto.response.PayResponseDto;
import backend.time.dto.ResponseDto;
import backend.time.service.PayService;
import com.siot.IamportRestClient.exception.IamportResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class PayApiController {

    private final PayService payService;

    @PostMapping("/pay/prepare")
    public ResponseDto postPrepare(@RequestBody PostPrepareDto request, @AuthenticationPrincipal PrincipalDetail principalDetail)
            throws IamportResponseException, IOException {
        payService.postPrepare(request, principalDetail.getMember().getId());
        return new ResponseDto<>(HttpStatus.OK.value(), "사전등록 완료");
    }

    @PostMapping("pay/{imp_uid}")
    public ResponseDto<PayResponseDto> verifyAndChargeV1(@PathVariable("imp_uid") String imp_uid, @AuthenticationPrincipal PrincipalDetail principalDetail) throws IamportResponseException, IOException {
        PayResponseDto payResponseDto = payService.verifyAndChargePay(principalDetail.getMember().getId(), imp_uid);
        return new ResponseDto<>(HttpStatus.OK.value(), payResponseDto);
    }

    @PostMapping("pay/{imp_uid}/v2")
    public ResponseDto<PayResponseDto> verifyAndChargeV2(@PathVariable("imp_uid") String imp_uid, @RequestBody VerifyAndChargeDto request, @AuthenticationPrincipal PrincipalDetail principalDetail) throws IamportResponseException, IOException {
        PayResponseDto payResponseDto = payService.verifyAndChargePayV2(principalDetail.getMember().getId(), imp_uid, request);
        return new ResponseDto<>(HttpStatus.OK.value(), payResponseDto);
    }
}
