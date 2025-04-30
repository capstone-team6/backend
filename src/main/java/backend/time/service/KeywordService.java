package backend.time.service;

import backend.time.dto.request.KeywordRequestDto;
import backend.time.exception.MemberNotFoundException;
import backend.time.model.Keyword;
import backend.time.model.Member.Member;
import backend.time.repository.KeywordRepository;
import backend.time.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeywordService {

    private final KeywordRepository keywordRepository;
    private final MemberRepository memberRepository;

    public List<Keyword> findAll(){
        return keywordRepository.findAll();
    }

    @Transactional
    public String save(KeywordRequestDto keywordDto, String kakaoId) {
        String keyword = keywordDto.getKeyword();
        Member member = memberRepository.findByKakaoId(kakaoId).orElseThrow(() -> {throw new MemberNotFoundException();});

        Keyword keywordEntity = new Keyword();
        keywordEntity.setKeyword(keyword);
        keywordEntity.setMember(member);

        return keywordRepository.save(keywordEntity).getKeyword();
    }

    public List<Keyword> findByMemberId(Long memberId) {
        return keywordRepository.findKeywordByMember_Id(memberId);
    }

    public void deleteKeword(Long id) {
        keywordRepository.deleteById(id);
    }
}
