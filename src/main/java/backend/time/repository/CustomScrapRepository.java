package backend.time.repository;

import backend.time.dto.response.BoardResponseDto.ScrapListResponseDto;
import java.util.List;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Pageable;

public interface CustomScrapRepository {
    List<ScrapListResponseDto> getScrapList(Pageable pageable, Long memberId, Point userLocation);
}
