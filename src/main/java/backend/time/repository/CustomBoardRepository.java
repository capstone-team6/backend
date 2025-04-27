package backend.time.repository;

import backend.time.dto.request.BoardSearchDto;
import backend.time.dto.response.BoardResponseDto.BoardSearchHaversine;
import backend.time.dto.response.BoardResponseDto.BoardSearchSpatial;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomBoardRepository {
    Page<BoardSearchHaversine> searchBoardsHaversine(BoardSearchDto requestDto, double userLongitude, double userLatitude,
                                                     Pageable pageable);
    Page<BoardSearchSpatial> searchBoardsSpatial(BoardSearchDto requestDto, Point userLocation, Pageable pageable);
    Page<BoardSearchSpatial> searchBoardsSpatialNative(BoardSearchDto requestDto, Point userLocation, Pageable pageable);
}

