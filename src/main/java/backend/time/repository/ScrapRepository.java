package backend.time.repository;

import backend.time.model.Scrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ScrapRepository extends JpaRepository<Scrap,Long>, JpaSpecificationExecutor<Scrap>, CustomScrapRepository {
    Optional<Scrap> findByMemberIdAndBoardId(Long memberId, Long boardId);
}
