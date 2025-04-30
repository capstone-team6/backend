package backend.time.service;

import backend.time.dto.request.ObjectionRequestDto;
import backend.time.dto.response.ObjectionResponseDto;
import backend.time.model.Member.Member;
import backend.time.model.Objection.Objection;
import backend.time.model.Objection.ObjectionImage;
import backend.time.model.Objection.ObjectionStatus;
import backend.time.repository.MemberRepository;
import backend.time.repository.ObjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ObjectionService {
    private final MemberRepository memberRepository;
    private final ObjectionRepository objectionRepository;
    private final ImageManager imageManager;
    //이미지 아직 안함
    @Transactional
    public void createObjection(Member member, ObjectionRequestDto objectionDto)throws IOException {
        Member objector = memberRepository.findById(member.getId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다.ㅣ"));
        Member objected = memberRepository.findById(objectionDto.getObjectedId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        if (objectionDto.getImages() != null && objectionDto.getImages().size() < 6) {
            Objection objection = Objection.builder()
                    .title(objectionDto.getTitle())
                    .content(objectionDto.getContent())
                    .objector(objector)
                    .objected(objected)
                    .status(ObjectionStatus.Reported)
                    .build();
            objectionRepository.save(objection);
            imageManager.saveObjectionImages(objectionDto.getImages(), objection);
        } else if (objectionDto.getImages() == null) {
            Objection objection = Objection.builder()
                    .title(objectionDto.getTitle())
                    .content(objectionDto.getContent())
                    .objector(objector)
                    .objected(objected)
                    .status(ObjectionStatus.Reported)
                    .build();
            objectionRepository.save(objection);
        } else {
            throw new IllegalArgumentException("최대 5개의 이미지만 업로드 할 수 있습니다.");
        }
    }

    public List<ObjectionResponseDto> getObjections(Long memberId){
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 회원입니다."));
        // 그 사람이 쓴 이의신청 내역, 시간 올림차순으로
        List<Objection> objectionList = new ArrayList<>();

        objectionList = objectionRepository.findAll()
                .stream()
                .filter(objection->objection.getObjector().getId().equals(memberId))
                .collect(Collectors.toList());

        List<ObjectionResponseDto> objectionResponseDtos = new ArrayList<>();

        for(int i=0; i<objectionList.size(); i++){
            Objection tmpObjection = objectionList.get(i);
            List<ObjectionImage> images = objectionList.get(i).getImages();
            ObjectionResponseDto objectionResponseDto = ObjectionResponseDto.builder()
                    .title(tmpObjection.getTitle())
                    .content(tmpObjection.getContent())
                    .nickname(tmpObjection.getObjected().getNickname())
                    .objectionStatus(tmpObjection.getStatus())
//                    .images()
                    .build();
            List<String> collect = images.stream().map(ObjectionImage::getStoredFileName)
                    .toList();
            objectionResponseDto.setImages(collect);
            objectionResponseDtos.add(objectionResponseDto);
        }
        return objectionResponseDtos;


    }




}
