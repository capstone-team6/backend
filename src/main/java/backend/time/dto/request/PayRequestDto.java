package backend.time.dto.request;

import lombok.Data;

public class PayRequestDto {
    @Data
    public static class PayMethDto {
        private String payMeth;
        //계좌이체일때
        private String holder; // 예금주
        private String bank; // 은행
        private Long accountNumber; //계좌번호
    }

    @Data
    public static class PostPrepareDto {
        private String merchant_uid;
        private Long amount;
    }

    @Data
    public static class VerifyAndChargeDto {
        private String merchant_uid;
    }
}
