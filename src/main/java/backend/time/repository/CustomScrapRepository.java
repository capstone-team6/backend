package backend.time.repository;

import backend.time.dto.response.BoardResponseDto;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomScrapRepository {
    Page<BoardResponseDto.ScrapListResponseDto> getScrapList(Pageable pageable, Long memberId, Point userLocation);
}
