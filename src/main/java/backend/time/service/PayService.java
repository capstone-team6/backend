package backend.time.service;

import static backend.time.model.pay.PayState.BEFORE;
import static backend.time.model.pay.PayState.COMPLETE;

import backend.time.dto.PayResponseDto;
import backend.time.dto.request.VerifyAndChargeDto;
import backend.time.dto.request.PostPrepareDto;
import backend.time.model.Member.Member;
import backend.time.model.pay.PayCharge;
import backend.time.repository.MemberRepository;
import backend.time.repository.PayChargeRepository;
import com.siot.IamportRestClient.IamportClient;
import com.siot.IamportRestClient.exception.IamportResponseException;
import com.siot.IamportRestClient.request.CancelData;
import com.siot.IamportRestClient.request.PrepareData;
import com.siot.IamportRestClient.response.IamportResponse;
import com.siot.IamportRestClient.response.Payment;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
@Transactional(readOnly = true)
public class PayService {

    private final IamportClient iamportClient;
    private final PayChargeRepository payChargeRepository;
    private final MemberRepository memberRepository;

    public PayService(@Value("${REST_API_KEY}") String restApiKey, @Value("${REST_API_SECRET}") String restApiSecret,
                      PayChargeRepository payChargeRepository, MemberRepository memberRepository) {

        this.iamportClient = new IamportClient(restApiKey, restApiSecret);
        this.payChargeRepository = payChargeRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public PayResponseDto verifyAndChargePay(Long userId, String impUid)
            throws IamportResponseException, IOException {
        IamportResponse<Payment> iamportResponse = iamportClient.paymentByImpUid(impUid);
        Long amount = (iamportResponse.getResponse().getAmount()).longValue();
        String status = iamportResponse.getResponse().getStatus();

        if (!iamportResponse.getResponse().getStatus().equals("paid")) {
            throw new IllegalArgumentException("결제 오류입니다.");
        }

        if (payChargeRepository.existsByImpUid(impUid)) {
            throw new IllegalArgumentException("이미 결제 되었습니다.");
        }

        PayResponseDto payResponseDto = PayResponseDto.builder()
                .imp_uid(impUid)
                .amount(amount)
                .status(status)
                .member_id(userId)
                .build();

        chargePay(userId, impUid, amount);

        return payResponseDto;
    }

    private void chargePay(Long userId, String imp_uid, Long amount) {
        Member findMember = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 멤버가 존재하지 않습니다."));
        findMember.addTimePay(amount);

        PayCharge payCharge = PayCharge.builder()
                .member(findMember)
                .amount(amount)
                .impUid(imp_uid)
                .payState(COMPLETE)
                .build();
        payChargeRepository.save(payCharge);
    }

    //<---------------------------------------------------------------------------------->

    public void postPrepare(PostPrepareDto request, Long userId) throws IamportResponseException, IOException {
        Member findMember = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 멤버가 존재하지 않습니다."));
        PrepareData prepareData = new PrepareData(request.getMerchant_uid(), BigDecimal.valueOf(request.getAmount()));
        iamportClient.postPrepare(prepareData);

        PayCharge payCharge = PayCharge.builder()
                .member(findMember)
                .amount(request.getAmount())
                .merchantUid(request.getMerchant_uid())
                .payState(BEFORE)
                .build();
        payChargeRepository.save(payCharge);
    }

    @Transactional
    public PayResponseDto verifyAndChargePayV2(Long userId, String impUid, VerifyAndChargeDto request)
            throws IamportResponseException, IOException {
        PayCharge findPayCharge = payChargeRepository.findByMerchantUid(request.getMerchant_uid())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 merchant_uid입니다."));
        IamportResponse<Payment> iamportResponse = iamportClient.paymentByImpUid(impUid);
        Long amount = (iamportResponse.getResponse().getAmount()).longValue();
        String status = iamportResponse.getResponse().getStatus();

        if (!iamportResponse.getResponse().getStatus().equals("paid")) {
            throw new IllegalArgumentException("결제 오류입니다.");
        }
        if (!amount.equals(findPayCharge.getAmount())) {
            CancelData cancelData = new CancelData(iamportResponse.getResponse().getImpUid(), true);
            iamportClient.cancelPaymentByImpUid(cancelData);
            throw new IllegalArgumentException("위변조 의심 결제 건 입니다.");
        }
        if (payChargeRepository.existsByImpUid(impUid)) {
            throw new IllegalArgumentException("이미 결제 되었습니다.");
        }

        PayResponseDto payResponseDto = PayResponseDto.builder()
                .imp_uid(impUid)
                .amount(amount)
                .status(status)
                .member_id(userId)
                .build();

        chargePayV2(userId, impUid, amount, findPayCharge);
        return payResponseDto;
    }

    private void chargePayV2(Long userId, String imp_uid, Long amount, PayCharge payCharge) {
        Member findMember = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 멤버가 존재하지 않습니다."));
        findMember.addTimePay(amount);

        payCharge.charge(imp_uid);
    }
}
