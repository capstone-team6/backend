package backend.time.repository;

import static backend.time.model.board.QBoard.board;
import static backend.time.model.board.QImage.image;

import backend.time.dto.response.BoardResponseDto.BoardListResponseDto;
import backend.time.dto.response.BoardResponseDto.BoardSearchSpatial;
import backend.time.dto.response.BoardResponseDto.ScrapListResponseDto;
import backend.time.dto.response.QBoardResponseDto_BoardListResponseDto;
import backend.time.dto.response.QBoardResponseDto_ScrapListResponseDto;
import backend.time.model.board.QImage;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;

public class CustomScrapRepositoryImpl implements CustomScrapRepository {
    private final JPAQueryFactory queryFactory;

    public CustomScrapRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<ScrapListResponseDto> getScrapList(Pageable pageable, Long memberId, Point userLocation) {
        QImage minImage = new QImage("minImage");
        NumberTemplate<Double> distanceExpr = Expressions.numberTemplate(Double.class,
                "ST_Distance_Sphere({0}, {1})",
                board.location, userLocation
        );
        JPAQuery<ScrapListResponseDto> query = queryFactory
            .select(new QBoardResponseDto_ScrapListResponseDto(
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
                        image.storedFileName,
                        board.boardType
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
                .where(board.member.id.eq(memberId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        for (Sort.Order o : pageable.getSort()) {
            PathBuilder pathBuilder = new PathBuilder(board.getType(), board.getMetadata());
            query.orderBy(
                    new OrderSpecifier(o.isAscending() ? Order.ASC : Order.DESC, pathBuilder.get(o.getProperty())));
        }

        List<ScrapListResponseDto> content = query.fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(board.count())
                .from(board)
                .where(board.member.id.eq(memberId));

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}
