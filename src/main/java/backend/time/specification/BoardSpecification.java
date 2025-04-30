package backend.time.specification;

import backend.time.model.board.Board;
import backend.time.model.board.BoardCategory;
import backend.time.model.board.BoardType;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class BoardSpecification {

    // 위치 기반 검색 결과로 필터링
    public static Specification<Board> withIds(List<Long> ids) {
        return (root, query, cb) -> {
            if (ids == null || ids.isEmpty()) {
                return cb.disjunction();
            }
            return root.get("id").in(ids);
        };
    }

    // 제목이나 내용으로 검색
    public static Specification<Board> withTitleOrContent(String keyword) {
        System.out.println(keyword);
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) {
                return null;
            }
            return cb.or(
                    cb.like(root.get("title"), "%" + keyword + "%"),
                    cb.like(root.get("content"), "%" + keyword + "%")
            );
        };
    }

    // 카테고리로 검색
    public static Specification<Board> withCategory(String category) {
        return (root, query, cb) -> {
            if (category == null || category.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("category"), BoardCategory.valueOf(category));
        };
    }

    // 판매글 구매글 검색
    public static Specification<Board> withType(String boardType) {
        return (root, query, cb) -> {
            if (boardType == null || boardType.isEmpty()) {
                return null;
            }
            return cb.equal(root.get("boardType"), BoardType.valueOf(boardType));
        };
    }
}
