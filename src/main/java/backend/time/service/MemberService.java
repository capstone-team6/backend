package backend.time.service;

import backend.time.dto.request.MemberRequestDto.EvaluationDto;
import backend.time.dto.response.EvaluationResponseDto;
import backend.time.dto.response.EvaluationResponseDto.MannerEvaluationDto;
import backend.time.dto.response.EvaluationResponseDto.ServiceEvaluationDto;
import backend.time.dto.response.EvaluationResponseDto.ServiceEvaluationResponseDto;
import backend.time.dto.response.EvaluationResponseDto.ServiceEvaluationStarDto;
import backend.time.dto.response.MemberResponseDto;
import backend.time.exception.MemberNotFoundException;
import backend.time.model.Member.*;
import backend.time.model.Objection.Objection;
import backend.time.model.board.Board;
import backend.time.model.board.BoardCategory;
import backend.time.repository.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final EntityManager entityManager;
    private final BoardRepository boardRepository;

    private final ServiceStarRepository serviceStarRepository ;


    private final MannerEvaluationRepository mannerEvaluationRepository;
    private final ServiceEvaluationRepository serviceEvaluationRepository;
    private final ObjectionRepository objectionRepository;
    private final ObjectionImageRepository objectionImageRepository;

    public Member findMember(String kakaoId) {
        return memberRepository.findByKakaoId(kakaoId).orElseThrow(()->{throw new MemberNotFoundException();});
    }

    @Transactional
    public Long join(Member member) {
        return memberRepository.save(member).getId();
    }

    public Member findOne(Long memberId){
        return memberRepository.findById(memberId).orElseThrow(() -> {throw new MemberNotFoundException();});
    }


    //액세스 토큰과 리프레시 토큰을 얻기 위함
    public String getReturnAccessToken(String code) {
        System.out.println(code);
        String access_token = "";
        String refresh_token = "";
        String reqURL = "https://kauth.kakao.com/oauth/token"; //토큰 받기
        try {
            System.out.println("1");
            URL url = new URL(reqURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            System.out.println("2");

            //HttpURLConnection 설정 값 셋팅(필수 헤더 세팅)
            con.setRequestMethod("POST"); //인증 토큰 전송
            con.setRequestProperty("Content-type","application/x-www-form-urlencoded"); //인증 토큰 전송
            con.setDoOutput(true); //OutputStream으로 POST 데이터를 넘겨주겠다는 옵션
            System.out.println("3");

            //buffer 스트림 객체 값 셋팅 후 요청
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(con.getOutputStream()));
            StringBuilder sb = new StringBuilder();
            sb.append("grant_type=authorization_code");
            sb.append("&client_id=").append("e9bae5955920774f4b427d206bb20954"); // 앱 KEY VALUE
            sb.append("&redirect_uri=").append("http://localhost:8080/oauth/kakao");
            sb.append("&code=" + code);
            bw.write(sb.toString());
            System.out.println("sb="+sb);
            bw.flush();
            con.connect();

            int responseCode = con.getResponseCode();
            String r = con.getResponseMessage();
            System.out.println("responseCode지롱 : "+r);
            System.out.println("responseCode"+responseCode);
            //RETURN 값 result 변수에 저장
            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(),"UTF-8"));

            String br_line = "";
            String result = "";

            while ((br_line = br.readLine()) != null) {
                result += br_line;
                System.out.println(result);
            }
            System.out.println("result"+result);

            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(result);

            //토큰 값 저장 및 리턴
            access_token = element.getAsJsonObject().get("access_token").getAsString();
            refresh_token = element.getAsJsonObject().get("refresh_token").getAsString();

            br.close();
            bw.close();

        } catch (Exception e) {
            System.out.println("여기...?");
            e.printStackTrace();
        }
        return access_token;
    }

    //kakao에게 회원 id 요청
    public Member getUserInfo(String access_token){
        Map<String, Object> resultMap = new HashMap<>();
        String reqURL = "https://kapi.kakao.com/v2/user/me"; // 사용자 정보 가져오기
        try{
            URL url = new URL(reqURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            //Header 내용
            con.setRequestProperty("Authorization","Bearer "+access_token);

            int responseStatus = con.getResponseCode();
            System.out.println("responseCode : "+ responseStatus);

            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String br_line = "";
            String result = "";

            while((br_line = br.readLine())!=null){
                result += br_line;
            }
            System.out.println("result = "+result);

            JsonParser parser = new JsonParser();
            //JsonElement : JSON 데이터의 다양한 요소들에 접근할 수 있게 함
            JsonElement element = parser.parse(result);
//            System.out.println("element = "+element);

            String kakaoId = element.getAsJsonObject().get("id").getAsString();

            Optional<Member> isOurMember = memberRepository.findByKakaoId(kakaoId);
            br.close();

            //존재하면 resultMap 값 넣어줌
            if(isOurMember.isEmpty()){
                return Member.builder()
                        .kakaoId(kakaoId)
                        .role(Member_Role.GUEST)
                        .build();
            }
            else{
                return isOurMember.get();
            }

        }
        catch (Exception e){
            System.out.println("kakao로부터 사용자 정보 불러오기 실패");
            return null;
        }
    }

    //회원가입(위치 미포함)
    @Transactional
    public boolean saveMember(String kakaoId, String nickname){
        Optional<Member> member = memberRepository.findByKakaoId(kakaoId);
        if(member.isEmpty()){
            return false;
        }
        else{
            member.get().setRole(Member_Role.USER);
            member.get().setNickname(nickname);
            return true;
        }
    }

    @Transactional
    public void saveUnfinishMember(String kakaoId){
            Member member = Member.builder()
                    .kakaoId(kakaoId)
                    .role(Member_Role.GUEST)
                    .build();
            memberRepository.save(member);

    }
    // 닉네임 중복 검사
    public boolean isNicknameDuplicated(String nickname){
        // 중복 됨
        if(memberRepository.findByNickname(nickname).isPresent()){ return true;}
        // 중복 아닐 때
        else{return false;}
    }

    // 닉네임 변경
    @Transactional
    public boolean changeNickname(Member member, String nickname){
        Member isMember = memberRepository.findById(member.getId())
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 회원입니다."));

        if(memberRepository.findByNickname(nickname).isEmpty()){ //닉네임이 중복되지 않으면
            isMember.setNickname(nickname);
            entityManager.flush();
            return true;
        }
        else{
            return false;
        }
    }

    //회원 탈퇴 (우리 DB에서만 없애는거)
    @Transactional
    public void deleteMember(Member member){
        Member isMember = memberRepository.findById(member.getId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

//        objectionImageRepository.deleteAll();
        if(!boardRepository.findByMember(member).isEmpty()){
/*            System.out.println("yes");
            System.out.println("delete"+boardRepository.findByMember(member).size());*/
            boardRepository.deleteAll(boardRepository.findByMember(member));
        }
        if(!objectionRepository.findByObjector(member).isEmpty()){
            List<Objection> objectionList = objectionRepository.findByObjector(member);
            for(int i=0; i<objectionList.size(); i++){
                objectionImageRepository.deleteAll(objectionImageRepository.findByObjection(objectionList.get(i)));
                System.out.println("findbyObjector"+objectionRepository.findByObjector(member).get(i));

            }
            objectionRepository.deleteAll(objectionRepository.findByObjector(member));
        }

        memberRepository.delete(isMember);
    }

    @Transactional
    public void sendEvaluation(Long memberId, Long boardId, EvaluationDto evaluationDto){
        Board board = boardRepository.findById(boardId) //내가 평가하기 전에 그 사람이 게시물을 삭제하면...? -> 1. 평가를 못받음 2. 매너 평가만 반영
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 게시물입니다."));
        Member receiver = memberRepository.findById(memberId)
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 회원입니다."));

        // manner 평가 추가
        sendMannerEvaluation(receiver, evaluationDto.mannerEvaluationDtoList);

        //service 평가
        System.out.println("service input");
        sendServiceEvaluation(receiver, board, evaluationDto.serviceEvaluationDtoList);
        System.out.println("service output");

    }

    public void sendMannerEvaluation(Member receiver, List<MannerEvaluationCategory> mannerEvaluationCategoryList){
        List<MannerEvaluation> tmpMannerEvaluationList = receiver.getMannerEvaluationList();

        for(int i=0; i<mannerEvaluationCategoryList.size(); i++){
            MannerEvaluationCategory mannerEvaluationCategory = mannerEvaluationCategoryList.get(i);
            Integer isHere = -1;
            for(int j=0; j<tmpMannerEvaluationList.size(); j++){
                if(tmpMannerEvaluationList.get(j).getMannerEvaluationCategory().equals(mannerEvaluationCategory)){
                    isHere = j;
                    break;
                }
            }
            if(isHere != -1){ // 존재하면 => count + 1
                Integer tmpCount = tmpMannerEvaluationList.get(isHere).getMannerEvaluationCount()+1;
//                System.out.println("여기 존재 " + tmpCount);
                tmpMannerEvaluationList.get(isHere).setMannerEvaluationCount(tmpCount);
            }
            else{ //존재하지 않는 평가면 평가 추가 & count = 1
                MannerEvaluation mannerEvaluation = MannerEvaluation.builder()
                        .member(receiver)
                        .mannerEvaluationCategory(mannerEvaluationCategory)
                        .mannerEvaluationCount(1)
                        .build();
                mannerEvaluationRepository.save(mannerEvaluation);
//                tmpMannerEvaluationList.add(mannerEvaluation);
            }
            calculateMannerTime(receiver, mannerEvaluationCategory);
        }


//        receiver.setMannerEvaluationList(tmpMannerEvaluationList);
    }



/*    public void calculateServiceAVG(Member member){
        List<ServiceEvaluation> serviceEvaluationList = serviceEvaluationRepository.findByMember(member);
//        Optional<ServiceEvaluation> serviceEvaluation = serviceEvaluationRepository.findById(serviceId);
        for(int i=0; i<serviceEvaluationList.size();i++){
            ServiceEvaluation serviceEvaluation = serviceEvaluationList.get(i);
            Integer serviceEvaluationScore = serviceEvaluation.getServiceEvaluationScore();
            serviceEvaluation.setServiceEvaluationAVG(serviceEvaluationScore/serviceEvaluation.getServiceEvaluationCount());
        }

        ServiceEvaluationCategory serviceEvaluationCategory = serviceEvaluation.get().getServiceEvaluationCategory();



        serviceEvaluation.get().setEvaluationAVG();

    }*/
    // 서비스 평가 보내기
    public void sendServiceEvaluation(Member receiver, Board board, List<ServiceEvaluationCategory> serviceEvaluationCategoryList) {
        // board가 없을 경우에는?
        //해당 사람이 해당 카테고리에 쓴 평가들 모임
        List<ServiceEvaluation> tmpServiceEvaluationList = serviceEvaluationRepository.findByMemberAndBoardCategory(receiver,board.getCategory());
        ServiceEvaluation serviceEvaluation; // 이미 있는 평가면 그 아이를 가리키고 없으면 새로운 애를 만듦
//        Integer currentScore; // 카테고리별 점수를 구하기 위함
        ServiceStar serviceStar;
        for (int i = 0; i < serviceEvaluationCategoryList.size(); i++) {
            ServiceEvaluationCategory serviceEvaluationCategory = serviceEvaluationCategoryList.get(i);
            Integer isHere = -1;
            for (int j = 0; j < tmpServiceEvaluationList.size(); j++) {
                if (tmpServiceEvaluationList.get(j).getServiceEvaluationCategory().equals(serviceEvaluationCategory)) {
                    isHere = j;
                    break;
                }
            }
            if (isHere != -1) { // 존재하면 => count + 1
                serviceEvaluation = tmpServiceEvaluationList.get(isHere);
                Integer tmpCount = serviceEvaluation.getServiceEvaluationCount() + 1;
//                System.out.println("isHere tmp "+tmpServiceEvaluationList.get(isHere).getBoardCategory());
                serviceEvaluation.setServiceEvaluationCount(tmpCount);


            } else { //존재하지 않는 평가면 평가 추가 & count = 1
                serviceEvaluation = ServiceEvaluation.builder()
                        .member(receiver)
                        .serviceEvaluationCategory(serviceEvaluationCategory)
                        .serviceEvaluationCount(1)
                        .boardCategory(board.getCategory())
                        .build();
                serviceEvaluationRepository.save(serviceEvaluation);

//                tmpServiceEvaluationList.add(serviceEvaluation);
            }
            if(serviceStarRepository.findByMemberAndBoardCategory(receiver, board.getCategory()).isPresent()){
                System.out.println("notfirst");

                serviceStar = serviceStarRepository.findByMemberAndBoardCategory(receiver, board.getCategory()).get();
                serviceStar.setTotalCount(serviceStar.getTotalCount()+1);
                serviceStar.setTotalScore(serviceStar.getTotalScore()+calculateServiceScore(receiver,serviceEvaluationCategory));
            }
            else{
                System.out.println("firstEvaluation");

                serviceStar = ServiceStar.builder()
                        .member(receiver)
                        .boardCategory(board.getCategory())
                        .totalCount(1)
                        .totalScore(calculateServiceScore(receiver, serviceEvaluationCategory))
                        .build();
                serviceStarRepository.save(serviceStar);
            }


/*
            if(serviceEvaluation.getEvaluationTotalCount() == null){
                serviceEvaluation.setEvaluationTotalCount(1);
                serviceEvaluation.setServiceEvaluationScore(calculateServiceScore(receiver, serviceEvaluationCategory));

            }
            else{
                serviceEvaluation.setEvaluationTotalCount(serviceEvaluation.getEvaluationTotalCount()+1);
                serviceEvaluation.setServiceEvaluationScore(serviceEvaluation.getServiceEvaluationScore()+calculateServiceScore(receiver, serviceEvaluationCategory));
            }
            serviceEvaluation.setServiceEvaluationAVG(avgToStar(serviceEvaluation.getServiceEvaluationScore()/serviceEvaluation.getEvaluationTotalCount()));
*/


//            serviceEvaluation.setServiceEvaluationScore(calculateServiceScore(receiver,serviceEvaluationCategory));

/*            calcAvg(tmpServiceEvaluationList);
            Integer avg = serviceEvaluationCount.getServiceEvaluationScore()/tmpServiceEvaluationList.size();
            serviceEvaluationCount.setServiceEvaluationAVG(avgToStar(avg));*/
        }
//        receiver.setServiceEvaluationList(tmpServiceEvaluationList);
    }
/*    public Integer calcAvg(List<ServiceEvaluation> tmpServiceEvaluationList){
        int sum=0;
        for(int i=0; i<tmpServiceEvaluationList.size(); i++){
            sum+=tmpServiceEvaluationList.get(i).getServiceEvaluationScore();
        }
        int avg = sum/tmpServiceEvaluationList.size() * 100;
        return avgToStar(avg);

    }*/

    // 각 카테고리 별 service 평가를 별점으로 바꿔주는 함수

//-~0 (1~10 11~20) (21~30 31~40) (41~50 51~60) (61~70 71~80) (81~90 91~100)

    public Integer avgToStar(float avg){
        avg *= 100;
        Integer star;
        if(avg<=0) star = 0;
        else if(avg<=20) star = 1;
        else if(avg<=40) star = 2;
        else if(avg<=60) star = 3;
        else if(avg<=80) star = 4;
        else star =5;
        return star;
    }

    @Transactional
    public void calculateMannerTime(Member member, MannerEvaluationCategory mannerEvaluationCategory){
        Integer tmpCount;
        Integer result;
        if(member.getMannerEvaluationScore() != null) {
            tmpCount = member.getMannerEvaluationScore();
        }
        else {
            tmpCount = 0;
            System.out.println("here");
        }
        if(mannerEvaluationCategory.equals(MannerEvaluationCategory.NICETIME)||mannerEvaluationCategory.equals(MannerEvaluationCategory.PRETTYLANGUAGE)
                ||mannerEvaluationCategory.equals(MannerEvaluationCategory.KIND)||mannerEvaluationCategory.equals(MannerEvaluationCategory.CHATFAST)){
            result = tmpCount+1;
            member.setMannerEvaluationScore(result);
            if(result >= 10){
                member.setMannerTime(member.getMannerTime()+5);
                member.setMannerEvaluationScore(member.getMannerEvaluationScore()-10);
            }
        }
        else{
             result = tmpCount-1;
            member.setMannerEvaluationScore(result);
            if(result <= -10){
                member.setMannerTime(member.getMannerTime()-5);
                member.setMannerEvaluationScore(member.getMannerEvaluationScore()+10);
            }
        }

    }
    //통합 매너 평가 점수도 수정하기
    @Transactional
    public Integer calculateServiceScore(Member member, ServiceEvaluationCategory serviceEvaluationCategory){
        if(serviceEvaluationCategory.equals(ServiceEvaluationCategory.EXACT)||serviceEvaluationCategory.equals(ServiceEvaluationCategory.POSITIVE)
                ||serviceEvaluationCategory.equals(ServiceEvaluationCategory.FLEXIBILITY)){
            //통합 매너 평가 점수
            calculateMannerTime(member, MannerEvaluationCategory.NICETIME);
            System.out.println("+1");

            //각 서비스 점수
            return 1;

        }
        else{
            //통합 매너 평가 점수
            calculateMannerTime(member,MannerEvaluationCategory.LATETIME);
            System.out.println("-1");

            //각 서비스 점수
            return -1;
        }

    }


    //평가 보기
    public EvaluationResponseDto.getEvaluationResponseDto getEvaluation(Long memberId){
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 회원입니다."));
        List<MannerEvaluationDto> mannerEvaluationDtoList = new ArrayList<>();
//        System.out.println("getMannerEvaluationList size : "+member.getMannerEvaluationList().size() );
        for(int i=0; i<member.getMannerEvaluationList().size(); i++){
            MannerEvaluation mannerEvaluation = member.getMannerEvaluationList().get(i);
            MannerEvaluationDto mannerEvaluationDto = MannerEvaluationDto.builder()
                    .mannerEvaluationCategory(mannerEvaluation.getMannerEvaluationCategory())
                    .mannerEvaluationCount(mannerEvaluation.getMannerEvaluationCount())
                    .build();
            mannerEvaluationDtoList.add(mannerEvaluationDto);
        }
        //각 카테고리별 별 점수

        List<ServiceEvaluation> serviceEvaluationList = member.getServiceEvaluationList();
        List<ServiceEvaluationStarDto> starDtoList= new ArrayList<>();
        List<BoardCategory> completeCategory = new ArrayList<>();
        ServiceStar serviceStar;
        for(int i=0; i<serviceEvaluationList.size(); i++){
            ServiceEvaluation serviceEvaluation = serviceEvaluationList.get(i);
            BoardCategory boardCategory = serviceEvaluationList.get(i).getBoardCategory();
            if(completeCategory.contains(boardCategory)){
                continue;
            }
            System.out.println("boardCategory "+boardCategory);
            if(serviceStarRepository.findByMemberAndBoardCategory(member,boardCategory).isPresent()){
                serviceStar = serviceStarRepository.findByMemberAndBoardCategory(member,boardCategory).get();
            }
            else{
                System.out.println("serviceStar empty");
                break;
            }
            completeCategory.add(boardCategory);
            float avg = serviceStar.getTotalScore().floatValue()/serviceStar.getTotalCount().floatValue();
            ServiceEvaluationStarDto starDto = ServiceEvaluationStarDto.builder()
                    .boardCategory(boardCategory)
                    .starCount(avgToStar(avg)) //아직 안 함
                    .build();
            starDtoList.add(starDto);
        }


//        System.out.println("getServiceEvaluationList size : "+member.getServiceEvaluationList().size() );
        return EvaluationResponseDto.getEvaluationResponseDto.builder()
                .mannerEvaluationList(mannerEvaluationDtoList)
                .serviceEvaluationStarDtoList(starDtoList)
                .build();
    }
    //각 서비스의 평균 점수 계산


    public ServiceEvaluationResponseDto getCategoryEvaluation(Long memberId, BoardCategory category){
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        List<ServiceEvaluation> serviceEvaluationList = serviceEvaluationRepository.findByMemberAndBoardCategory(member, category);
        List<ServiceEvaluationDto> serviceEvaluationDtoList = new ArrayList<>();

        for(int i=0; i<serviceEvaluationList.size(); i++) {
            ServiceEvaluation serviceEvaluation = serviceEvaluationList.get(i);
            ServiceEvaluationDto serviceEvaluationDto = ServiceEvaluationDto.builder()
                    .serviceEvaluationCategory(serviceEvaluation.getServiceEvaluationCategory())
                    .boardCategory(serviceEvaluation.getBoardCategory())//지워도 되는지 확인
                    .serviceEvaluationCount(serviceEvaluation.getServiceEvaluationCount())
                    .build();
            serviceEvaluationDtoList.add(serviceEvaluationDto);
        }

        return  ServiceEvaluationResponseDto.builder()
                .serviceEvaluationList(serviceEvaluationDtoList)
                .build();
    }

    public MemberResponseDto getProfile(Long id){
        Member member = memberRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("없는 회원입니다."));
        return MemberResponseDto.builder()
                .mannerTime(member.getMannerTime())
                .nickname(member.getNickname())
                .id(member.getId())
                .timePay(member.getTimePay())
                .build();
    }
    public MemberResponseDto getOtherProfile(Long id){
        Member member = memberRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("없는 회원입니다."));
        return MemberResponseDto.builder()
                .mannerTime(member.getMannerTime())
                .nickname(member.getNickname())
                .build();
    }



}

