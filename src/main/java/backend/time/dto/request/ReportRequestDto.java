package backend.time.dto.request;

import backend.time.model.ReportCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportRequestDto {
    private ReportCategory reportCategory; // 신고 사유

}
