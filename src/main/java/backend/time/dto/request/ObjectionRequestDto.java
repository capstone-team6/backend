package backend.time.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObjectionRequestDto {
    @NotEmpty
    @Length(max = 30)
    String title;

    @NotEmpty
    @Length(max = 500)
    String content;

    Long objectedId; // 이의제기된 사람 id

    List<MultipartFile> images;
}
