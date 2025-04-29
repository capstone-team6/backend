package backend.time.dto.request;

import lombok.Data;

@Data
public class PostPrepareDto {
    private String merchant_uid;
    private Long amount;
}
