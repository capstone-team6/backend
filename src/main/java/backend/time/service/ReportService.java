package backend.time.service;

import backend.time.dto.request.ReportRequestDto;
import backend.time.model.Member.Member;
import backend.time.model.Report;
import backend.time.model.board.Board;
import backend.time.repository.BoardRepository;
import backend.time.repository.MemberRepository;
import backend.time.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {
    private final MemberRepository memberRepository;
    private final BoardRepository boardRepository;
    private final ReportRepository reportRepository;
    @Transactional
    public boolean postReport(Long memberId, Long boardId, ReportRequestDto reportDto){
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 회원입니다."));
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        Report report = reportRepository.findByReporterIdAndReportedBoardId(memberId,boardId);
        if(report == null){
            report = Report.builder()
                    .reportCategory(reportDto.getReportCategory())
                    .reporterId(member.getId())
                    .reportedId(board.getMember().getId())
                    .reportedBoardId(board.getId())
                    .build();
            reportRepository.save(report);
        }
        else { // 기존에 신고한 전적이 있으면 신고 사유만 변경 시키기
            report.setReportCategory(reportDto.getReportCategory());
        }
        return true;
    }
}
