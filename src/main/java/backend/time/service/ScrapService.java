package backend.time.service;

import backend.time.model.Member.Member;
import backend.time.model.Scrap;
import backend.time.model.board.Board;
import backend.time.repository.BoardRepository;
import backend.time.repository.ScrapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ScrapService {
    private final ScrapRepository scrapRepository;
    private final BoardRepository boardRepository;
    private final NotificationService notificationService;

    @Transactional
    public boolean doScrap(Member member, Long boardId){
        Optional<Scrap> scrap = scrapRepository.findByMemberIdAndBoardId(member.getId(),boardId);
        Board board = boardRepository.findById(boardId)
                .orElseThrow(()->new IllegalArgumentException("없는 게시글 입니다."));
        if(scrap.isEmpty()){
            Scrap newScrap = Scrap.builder()
                    .board(board)
                    .member(member)
                    .build();
            scrapRepository.save(newScrap);
            notificationService.notifyScrap(member, boardId);
            board.setScrapCount(board.getScrapCount()+1);
            return true;
        }
        else{
            scrapRepository.delete(scrap.get());
            board.setScrapCount(board.getScrapCount()-1);
            return false;
        }
    }
}
