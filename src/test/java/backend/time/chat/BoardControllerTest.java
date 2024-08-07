package backend.time.chat;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

//@SpringBootTest
//@AutoConfigureMockMvc
//public class BoardControllerTest {
//
//    @Autowired
//    MemberService memberService;
//
//    @Autowired
//    EntityManager em;
//
//    @Autowired
//    MockMvc mvc;
//
//    @Autowired
//    WebApplicationContext context;
//
//    @BeforeEach
//    public void setting(){
//        mvc = MockMvcBuilders
//                .webAppContextSetup(context)
//                .build();
//    }
//
//    public Long createMember(String email){
//        Member member = new Member();
//        member.setEmail(email);
//        member.setNickname(UUID.randomUUID().toString());
//        return memberService.join(member);
//    }
//
//    public Long createBoard(String email){
//        Member seller = memberService.findOne(createMember(email));
//        Board board = new Board();
//        board.setTitle("상품명");
//        board.setBoardCategory(BoardCategory.WAITING);
//        board.setItemPrice(100);
//        board.setItemTime(60);
//        board.setContent("팝니다");
//        // 연관관계 편의 메서드 실행
//        board.setMember(seller);
//        // 상품 DB 저장
//        em.persist(board);
//        return board.getId();
//    }
//
//    /**
//     * 상품 상세 정보 페이지 이동(테스트용)
//     * 파라메터로 userEmail, boardid받아서 채팅방개설
//     */
//    @Test
//    @Transactional
//    void 테스트버전() throws Exception {
//        // given
//        Member member = em.find(Member.class, createMember("qudgns119@naver.com"));
//        Long boardId = createBoard("test@naver.com");
//
//        em.flush();
//        // when
//        mvc.perform(get("/list/1?userEmail=" + member.getEmail()))
//                .andDo(print())
//                .andExpect(status().is2xxSuccessful());
//    }
//
//
//}
