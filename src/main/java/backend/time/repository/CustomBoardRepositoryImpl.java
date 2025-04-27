package backend.time.repository;

import static backend.time.model.board.QBoard.board;
import static backend.time.model.board.QImage.image;

import backend.time.dto.request.BoardSearchDto;
import backend.time.dto.response.BoardResponseDto.BoardSearchHaversine;
import backend.time.dto.response.BoardResponseDto.BoardSearchSpatial;
import backend.time.dto.response.QBoardResponseDto_BoardSearchHaversine;
import backend.time.dto.response.QBoardResponseDto_BoardSearchSpatial;
import backend.time.model.board.BoardCategory;
import backend.time.model.board.BoardState;
import backend.time.model.board.BoardType;
import backend.time.model.board.QImage;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanTemplate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.sql.Timestamp;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;

@Slf4j
public class CustomBoardRepositoryImpl implements CustomBoardRepository {
    @PersistenceContext
    private EntityManager entityManager;

    private final JPAQueryFactory queryFactory;

    public CustomBoardRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<BoardSearchHaversine> searchBoardsHaversine(BoardSearchDto requestDto, double userLongitude,
                                                            double userLatitude, Pageable pageable) {
        QImage minImage = new QImage("minImage");

        NumberTemplate<Double> distanceExpr = Expressions.numberTemplate(Double.class,
                "6371 * acos(cos(radians({0})) * cos(radians({1})) * cos(radians({2}) - radians({3})) + sin(radians({4})) * sin(radians({5})))",
                userLatitude, board.latitude, board.longitude, userLongitude, userLatitude, board.latitude);

        JPAQuery<BoardSearchHaversine> query = queryFactory
                .select(new QBoardResponseDto_BoardSearchHaversine(
                        board.id,
                        board.title,
                        board.itemTime,
                        board.itemPrice,
                        board.createDate,
                        board.chatCount,
                        board.ScrapCount,
                        distanceExpr,
                        board.address,
                        board.boardState,
                        image.storedFileName
                ))
                .from(board)
                .leftJoin(image).on(image.board.eq(board)
                        .and(image.id.eq(
                                JPAExpressions
                                        .select(minImage.id.min())
                                        .from(minImage)
                                        .where(minImage.board.eq(board))
                        ))
                )
                .where(
                        withBoardType(requestDto.getBoardType()),
                        withDistanceHaversine(distanceExpr),
                        withKeyword(requestDto.getKeyword()),
                        withCategory(requestDto.getCategory())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        for (Sort.Order o : pageable.getSort()) {
            PathBuilder pathBuilder = new PathBuilder(board.getType(), board.getMetadata());
            query.orderBy(
                    new OrderSpecifier(o.isAscending() ? Order.ASC : Order.DESC, pathBuilder.get(o.getProperty())));
        }

        List<BoardSearchHaversine> content = query.fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(board.count())
                .from(board)
                .leftJoin(board.images, image)
                .where(
                        withBoardType(requestDto.getBoardType()),
                        withDistanceHaversine(distanceExpr),
                        withKeyword(requestDto.getKeyword()),
                        withCategory(requestDto.getCategory())
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression withDistanceHaversine(NumberTemplate<Double> distanceExpr) {
        return board.longitude.eq(0.0)
                .and(board.latitude.eq(90.0))
                .or(
                        distanceExpr.loe(10.0)
                );
    }

    @Override
    public Page<BoardSearchSpatial> searchBoardsSpatial(BoardSearchDto requestDto, Point userLocation,
                                                        Pageable pageable) {
        QImage minImage = new QImage("minImage");

        NumberTemplate<Double> distanceExpr = Expressions.numberTemplate(Double.class,
                "ST_Distance_Sphere({0}, {1})",
                board.location, userLocation
        );
        JPAQuery<BoardSearchSpatial> query = queryFactory
                .select(new QBoardResponseDto_BoardSearchSpatial(
                        board.id,
                        board.title,
                        board.itemTime,
                        board.itemPrice,
                        board.createDate,
                        board.chatCount,
                        board.ScrapCount,
                        distanceExpr,
                        board.address,
                        board.boardState,
                        image.storedFileName
                ))
                .from(board)
                .leftJoin(image).on(image.board.eq(board)
                        .and(image.id.eq(
                                JPAExpressions
                                        .select(minImage.id.min())
                                        .from(minImage)
                                        .where(minImage.board.eq(board))
                        ))
                )
                .where(
                        withKeyword(requestDto.getKeyword()),
                        withCategory(requestDto.getCategory()),
                        withBoardType(requestDto.getBoardType()),
                        withSpatialCondition(userLocation)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        for (Sort.Order o : pageable.getSort()) {
            PathBuilder pathBuilder = new PathBuilder(board.getType(), board.getMetadata());
            query.orderBy(
                    new OrderSpecifier(o.isAscending() ? Order.ASC : Order.DESC, pathBuilder.get(o.getProperty())));
        }

        List<BoardSearchSpatial> content = query.fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(board.count())
                .from(board)
                .leftJoin(board.images, image)
                .where(
                        withKeyword(requestDto.getKeyword()),
                        withCategory(requestDto.getCategory()),
                        withBoardType(requestDto.getBoardType()),
                        withSpatialCondition(userLocation)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private BooleanExpression withSpatialCondition(Point userLocation) {
        BooleanTemplate spatialExpr = Expressions.booleanTemplate(
                "ST_Contains(ST_Buffer({0}, {1}), {2})",
                userLocation, 0.1, board.location
        );

        return board.longitude.eq(0.0)
                .and(board.latitude.eq(90.0))
                .or(spatialExpr);
    }

    private BooleanExpression withKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return null;
        }
        return board.title.containsIgnoreCase(keyword)
                .or(board.content.containsIgnoreCase(keyword));
    }

    private BooleanExpression withCategory(String category) {
        if (category == null || category.isEmpty()) {
            return null;
        }
        return board.category.eq(BoardCategory.valueOf(category));
    }

    private BooleanExpression withBoardType(String boardType) {
        if (boardType == null || boardType.isEmpty()) {
            return null;
        }
        return board.boardType.eq(BoardType.valueOf(boardType));
    }

    @Override
    public Page<BoardSearchSpatial> searchBoardsSpatialNative(BoardSearchDto requestDto, Point userLocation,
                                                              Pageable pageable) {
        String baseQuery = """
                (SELECT b.id, b.title, b.item_time, b.item_price, b.created_date, b.chat_count, b.scrap_count,
                        ST_Distance_Sphere(b.location, ST_GeomFromText(:point, 4326)) AS distance,
                        b.address, b.board_state, fi.stored_file_name AS firstImage
                 FROM board b
                 LEFT JOIN (
                     SELECT i.board_id, i.stored_file_name
                     FROM image i
                     INNER JOIN (
                         SELECT board_id, MIN(id) AS min_id
                         FROM image
                         GROUP BY board_id
                      ) mi ON i.board_id = mi.board_id AND i.id = mi.min_id
                  ) fi ON b.id = fi.board_id
                 WHERE ST_Contains(ST_Buffer(ST_GeomFromText(:point, 4326), 0.1), b.location)
                   AND (:keyword IS NULL OR b.title LIKE CONCAT('%', :keyword, '%') OR b.content LIKE CONCAT('%', :keyword, '%'))
                   AND (:category IS NULL OR b.category = :category)
                   AND (:boardType IS NULL OR b.board_type = :boardType))

                UNION ALL

                (SELECT b.id, b.title, b.item_time, b.item_price, b.created_date, b.chat_count, b.scrap_count,
                        NULL AS distance,
                        b.address, b.board_state, fi.stored_file_name AS firstImage
                 FROM board b
                 LEFT JOIN (
                     SELECT i.board_id, i.stored_file_name
                     FROM image i
                     INNER JOIN (
                         SELECT board_id, MIN(id) AS min_id
                         FROM image
                         GROUP BY board_id
                       ) mi ON i.board_id = mi.board_id AND i.id = mi.min_id
                  ) fi ON b.id = fi.board_id
                 WHERE b.longitude = 0.0 AND b.latitude = 90.0
                   AND (:keyword IS NULL OR b.title LIKE CONCAT('%', :keyword, '%') OR b.content LIKE CONCAT('%', :keyword, '%'))
                   AND (:category IS NULL OR b.category = :category)
                   AND (:boardType IS NULL OR b.board_type = :boardType))
                 ORDER BY created_date DESC, id DESC

                """;

        String FormattedUserLocation = String.format("POINT(%f %f)", userLocation.getX(), userLocation.getY());

        Query query = entityManager.createNativeQuery(baseQuery);
        query.setParameter("point", FormattedUserLocation);
        query.setParameter("keyword", requestDto.getKeyword());
        query.setParameter("category", requestDto.getCategory());
        query.setParameter("boardType", requestDto.getBoardType());

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Object[]> resultList = query.getResultList();

        List<BoardSearchSpatial> content = resultList.stream()
                .map(row -> new BoardSearchSpatial(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        row[3] != null ? ((Number) row[3]).longValue() : null,
                        (Timestamp) row[4],
                        ((Number) row[5]).intValue(),
                        ((Number) row[6]).intValue(),
                        row[7] != null ? ((Number) row[7]).doubleValue() : null,
                        (String) row[8],
                        BoardState.valueOf((String) row[9]),
                        (String) row[10]
                ))
                .toList();

        String countQuery = """
                SELECT COUNT(*) FROM (
                    SELECT id FROM board b
                    WHERE ST_Contains(ST_Buffer(ST_GeomFromText(:point, 4326), 0.1), b.location)
                      AND (:keyword IS NULL OR b.title LIKE CONCAT('%', :keyword, '%') OR b.content LIKE CONCAT('%', :keyword, '%'))
                      AND (:category IS NULL OR b.category = :category)
                      AND (:boardType IS NULL OR b.board_type = :boardType)

                    UNION ALL

                    SELECT id FROM board b
                    WHERE b.longitude = 0.0 AND b.latitude = 90.0
                      AND (:keyword IS NULL OR b.title LIKE CONCAT('%', :keyword, '%') OR b.content LIKE CONCAT('%', :keyword, '%'))
                      AND (:category IS NULL OR b.category = :category)
                      AND (:boardType IS NULL OR b.board_type = :boardType)
                ) AS total
                """;

        Query countQ = entityManager.createNativeQuery(countQuery);
        countQ.setParameter("point", FormattedUserLocation);
        countQ.setParameter("keyword", requestDto.getKeyword());
        countQ.setParameter("category", requestDto.getCategory());
        countQ.setParameter("boardType", requestDto.getBoardType());

        long total = ((Number) countQ.getSingleResult()).longValue();

        return PageableExecutionUtils.getPage(content, pageable, () -> total);
    }
}

