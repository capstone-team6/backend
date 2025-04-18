package backend.time.controller;

import backend.time.config.auth.PrincipalDetail;
import backend.time.dto.PayResponseDto;
import backend.time.service.PayService;
import com.siot.IamportRestClient.exception.IamportResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class PayApiController {

    private final PayService payService;

//    @PostMapping("api/charge/point")
//    public ResponseDto charge(@RequestBody PayDto payDto, @AuthenticationPrincipal PrincipalDetail principalDetail) {
//        payService.chargePay(principalDetail.getMember(), payDto);
//        return new ResponseDto<String>(HttpStatus.OK.value(),"틈새페이 충전 완료");
//    }

    //검증 후 db 저장 로직
    @PostMapping("pay/{imp_uid}")
    public PayResponseDto charge(@PathVariable("imp_uid") String imp_uid, @AuthenticationPrincipal PrincipalDetail principalDetail) throws IamportResponseException, IOException {
        PayResponseDto payResponseDto = payService.verifyPay(principalDetail.getMember().getId(), imp_uid);
        return payResponseDto;
    }

}
