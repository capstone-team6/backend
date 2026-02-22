package backend.time.repository;

import backend.time.model.Member.Member;
import backend.time.model.board.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long>, CustomBoardRepository,
        JpaSpecificationExecutor<Board> {
    List<Board> findByMember(Member member);

    List<Board> findByMemberOrderByCreateDateDesc(Member member);

    List<Board> findByTraderOrderByCreateDateDesc(Member member);

    // N+1 해결: images를 JOIN FETCH로 한 번에 조회
    @Query("SELECT DISTINCT b FROM Board b LEFT JOIN FETCH b.images WHERE b.member = :member ORDER BY b.createDate DESC")
    List<Board> findByMemberWithImages(@Param("member") Member member);

    @Query("SELECT DISTINCT b FROM Board b LEFT JOIN FETCH b.images WHERE b.trader = :member ORDER BY b.createDate DESC")
    List<Board> findByTraderWithImages(@Param("member") Member member);
}