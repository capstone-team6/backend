package backend.time.service;

import backend.time.dto.request.*;
import backend.time.dto.request.BoardRequestDto.BoardUpdateDto;
import backend.time.dto.request.BoardRequestDto.PointDto;
import backend.time.dto.request.BoardRequestDto.WriteBoardDto;
import backend.time.dto.response.BoardResponseDto.AccountResponseDto;
import backend.time.dto.response.BoardResponseDto.WhoResponseDto;
import backend.time.model.ChatRoom;
import backend.time.model.Member.Member;
import backend.time.model.pay.Account;
import backend.time.model.pay.PayMethod;
import backend.time.model.pay.PayStorage;
import backend.time.repository.*;
import backend.time.model.board.Board;
import backend.time.model.board.BoardCategory;
import backend.time.model.board.BoardType;
import backend.time.model.board.Image;
import backend.time.repository.BoardRepository;
import backend.time.repository.ImageRepository;
import backend.time.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static backend.time.dto.request.PayRequestDto.*;
import static backend.time.model.board.BoardState.*;
import static backend.time.model.pay.PayMethod.*;
import static backend.time.model.board.BoardType.SELL;

@Transactional(readOnly = true)
@RequiredArgsConstructor
@Service
public class BoardService {

    final private BoardRepository boardRepository;
    final private ImageManager imageManager;
    final private MemberRepository memberRepository;
    final private ImageRepository imageRepository;
    final private ChatRoomRepository chatRoomRepository;
    final private PayStorageRepository payStorageRepository;
    final private AccountRepository accountRepository;
    final private GeometryFactory geometryFactory = new GeometryFactory();

    // 금지 단어 목록
    List<String> forbiddenWords = List.of("과제", "소주", "맥주", "담배", "성매매", "마약", "주류", "씨발", "시발", "지랄", "존나", "개새끼");

    @Transactional
    public void addPoint(PointDto pointDto) {
        Member findMember = memberRepository.findByKakaoId(pointDto.getKakaoId())
                .orElseThrow(() -> new IllegalArgumentException("해당 멤버가 존재하지 않습니다."));
        findMember.setLongitude(pointDto.getLongitude());
        findMember.setLatitude(pointDto.getLatitude());
        findMember.setAddress(pointDto.getAddress());
        Point location = geometryFactory.createPoint(new Coordinate(pointDto.getLongitude(), pointDto.getLatitude()));
        location.setSRID(4326);
        findMember.setLocation(location);
    }

    public Board findOne(Long id) {
        return boardRepository.findById(id).get();
    }

    @Transactional
    public Long write(WriteBoardDto boardDto, Member member) throws IOException {
        for (String word : forbiddenWords) {
            if (boardDto.getTitle().contains(word) || boardDto.getContent().contains(word)) {
                throw new IllegalArgumentException("제목이나 내용에 금지된 단어가 포함되어 있습니다: " + word);
            }
        }

        double longitude = 0;
        double latitude = 90;
        String address = null;

        if (boardDto.getLongitude() != null && boardDto.getLatitude() != null) {
            longitude = boardDto.getLongitude();
            latitude = boardDto.getLatitude();
            address = boardDto.getAddress();
        }

        Point location = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        location.setSRID(4326);

        Board savedBoard = createAndSaveBoard(boardDto, member, address, location, longitude, latitude);

        Long boardId = savedBoard.getId();
        addNewImages(boardDto, savedBoard);

        return boardId;
    }

    private Board createAndSaveBoard(WriteBoardDto boardDto, Member member, String address, Point location,
            double longitude,
            double latitude) {
        Board board = Board.builder()
                .category(BoardCategory.valueOf(boardDto.getCategory()))
                .title(boardDto.getTitle())
                .itemTime(boardDto.getTime())
                .itemPrice(boardDto.getPrice())
                .content(boardDto.getContent())
                .boardType(BoardType.valueOf(boardDto.getBoardType()))
                .address(address)
                .longitude(longitude)
                .latitude(latitude)
                .member(member)
                .location(location)
                .build();

        return boardRepository.save(board);
    }

    private void addNewImages(WriteBoardDto boardDto, Board board) throws IOException {
        if (boardDto.getImages() != null) {
            if (boardDto.getImages().size() > 5) {
                throw new IllegalArgumentException("최대 5개의 이미지만 업로드할 수 있습니다.");
            }

            List<Image> images = imageManager.saveImages(boardDto.getImages(), board);

            for (Image image : images) {
                board.addImage(image);
            }
        }
    }

    @Transactional
    public void update(Long id, BoardUpdateDto boardUpdateDto) throws IOException {
        for (String word : forbiddenWords) {
            if (boardUpdateDto.getTitle().contains(word) || boardUpdateDto.getContent().contains(word)) {
                throw new IllegalArgumentException("제목이나 내용에 금지된 단어가 포함되어 있습니다: " + word);
            }
        }
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 존재하지 않습니다."));
        if (board.getBoardState() == RESERVED || board.getBoardState() == SOLD) {
            throw new IllegalArgumentException("거래 중이거나 판매 완료된 글은 수정할 수 없습니다.");
        }

        double longitude = 0;
        double latitude = 90;
        if (boardUpdateDto.getLatitude() != null && boardUpdateDto.getLongitude() != null) {
            longitude = boardUpdateDto.getLongitude();
            latitude = boardUpdateDto.getLatitude();
        }
        Point location = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        location.setSRID(4326);
        setUpdatedInformation(boardUpdateDto, board, location);
        List<MultipartFile> images = boardUpdateDto.getImages();
        updateImage(images, board);
    }

    private static void setUpdatedInformation(BoardUpdateDto boardUpdateDto, Board board, Point location) {
        board.setLocation(location);
        board.setTitle(boardUpdateDto.getTitle());
        board.setContent(boardUpdateDto.getContent());
        board.setItemPrice(boardUpdateDto.getPrice());
        board.setItemTime(boardUpdateDto.getTime());
        board.setAddress(boardUpdateDto.getAddress());
        board.setLongitude(boardUpdateDto.getLongitude());
        board.setLatitude(boardUpdateDto.getLatitude());
        board.setCategory(BoardCategory.valueOf(boardUpdateDto.getCategory()));
        board.setBoardType(BoardType.valueOf(boardUpdateDto.getBoardType()));
    }

    private void updateImage(List<MultipartFile> updateImages, Board board) throws IOException {
        if (updateImages != null && !updateImages.isEmpty()) {
            List<Image> findImages = imageRepository.findByBoard(board);
            Set<String> updateImageNames = updateImages.stream()
                    .map(MultipartFile::getOriginalFilename)
                    .collect(Collectors.toSet());

            removeImagesIfNotContain(board, findImages, updateImageNames);
            List<MultipartFile> newImages = new ArrayList<>();
            addNewImages(updateImages, board, findImages, newImages);
        }
    }

    private void addNewImages(List<MultipartFile> updateImages, Board board, List<Image> findImages,
            List<MultipartFile> newImages) throws IOException {
        for (MultipartFile image : updateImages) {
            if (findImages.stream().noneMatch(
                    findImage -> Objects.equals(findImage.getStoredFileName(), image.getOriginalFilename()))) {
                newImages.add(image);
            }
        }
        List<Image> imageList = imageManager.saveImages(newImages, board);

        for (Image image : imageList) {
            board.addImage(image);
        }
    }

    private void removeImagesIfNotContain(Board board, List<Image> findImages, Set<String> updateImageNames) {
        findImages.removeIf(findImage -> {
            boolean toDelete = !updateImageNames.contains(findImage.getStoredFileName());
            if (toDelete) {
                board.removeImage(findImage);
                imageRepository.delete(findImage);
            }
            return toDelete;
        });
    }

    @Transactional
    public void delete(Long id) {
        boardRepository.deleteById(id);
    }

    @Transactional
    public void choosePayMeth(PayMethDto paymethdto, Long boardId, Long chatId, Member member) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 채팅방이 존재하지 않습니다."));
        Board board = getAndValidBoard(boardId);
        PayMethod payMethod = PayMethod.valueOf(paymethdto.getPayMeth());

        board.setPayMethod(payMethod);
        if (payMethod.equals(PAY)) {
            processPayMethodPay(board, chatRoom);
        } else if (payMethod.equals(ACCOUNT)) {
            processPayMethodAccount(paymethdto, member, board, chatRoom);
        }
        board.setBoardState(RESERVED);
    }

    private void processPayMethodAccount(PayMethDto paymethdto, Member member, Board board, ChatRoom chatRoom) {
        Account account = Account.builder()
                .accountNumber(paymethdto.getAccountNumber())
                .bank(paymethdto.getBank())
                .member(member)
                .board(board)
                .chatRoom(chatRoom)
                .holder(paymethdto.getHolder()).build();
        accountRepository.save(account);
    }

    private void processPayMethodPay(Board board, ChatRoom chatRoom) {
        Member payer = getPayer(board, chatRoom);
        // 동시 충전/사용 요청에 의한 Lost Update 방지: SELECT FOR UPDATE
        payer = memberRepository.findByIdWithLock(payer.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당하는 멤버가 존재하지 않습니다."));

        Long timePay = payer.getTimePay();
        validTImePay(board, timePay);
        payer.setTimePay(timePay - board.getItemPrice());

        PayStorage storage = PayStorage.builder()
                .member(payer)
                .amount(board.getItemPrice())
                .board(board)
                .build();
        payStorageRepository.save(storage);
    }

    /**
     * 판매글일때 채팅하기 누른사람(buyer)이 돈을 지불한 사람
     * 구매글일때 채팅하기 누른사람(buyer)말고 글쓴사람 writer가 돈을 지불한 사람
     */
    private static Member getPayer(Board board, ChatRoom chatRoom) {
        Member payer;

        if (board.getBoardType().equals(SELL)) {
            payer = chatRoom.getBuyer();
        } else {
            payer = board.getMember();
        }
        return payer;
    }

    private static void validTImePay(Board board, Long timePay) {
        if (board.getItemPrice() > timePay) {
            throw new IllegalArgumentException("틈새페이를 충전해주세요");
        }
    }

    @Transactional
    public void cancelTrade(Long boardId, Long chatId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 채팅방이 존재하지 않습니다."));
        Board board = getAndValidBoard(boardId);

        if (board.getPayMethod().equals(PAY)) {
            refundForPay(board, chatRoom);
        }
        board.setBoardState(SALE);
    }

    private void refundForPay(Board board, ChatRoom chatRoom) {
        PayStorage storage = payStorageRepository.findByBoard(board)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 저장소가 존재하지 않습니다."));
        Member payer = getPayer(board, chatRoom);
        // 환불 시 동시 요청에 의한 Lost Update 방지: SELECT FOR UPDATE
        payer = memberRepository.findByIdWithLock(payer.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당하는 멤버가 존재하지 않습니다."));

        payer.setTimePay(payer.getTimePay() + storage.getAmount());
        payStorageRepository.delete(storage);
    }

    @Transactional
    public void completeTrade(Long boardId, Long chatId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 채팅방이 존재하지 않습니다."));
        Board board = getAndValidBoard(boardId);

        if (board.getPayMethod().equals(PAY)) {
            PayStorage storage = payStorageRepository.findByBoard(board)
                    .orElseThrow(() -> new IllegalArgumentException("해당하는 저장소가 존재하지 않습니다."));

            // 정산 시 동시 요청에 의한 Lost Update 방지: SELECT FOR UPDATE
            if (board.getBoardType().equals(SELL)) {
                Member seller = memberRepository.findByIdWithLock(board.getMember().getId())
                        .orElseThrow(() -> new IllegalArgumentException("해당하는 멤버가 존재하지 않습니다."));
                seller.setTimePay(seller.getTimePay() + storage.getAmount());
            } else {
                Member buyer = memberRepository.findByIdWithLock(chatRoom.getBuyer().getId())
                        .orElseThrow(() -> new IllegalArgumentException("해당하는 멤버가 존재하지 않습니다."));
                buyer.setTimePay(buyer.getTimePay() + storage.getAmount());
            }
            payStorageRepository.delete(storage);
        }
        board.setTrader(chatRoom.getBuyer());
        board.setBoardState(SOLD);
    }

    private Board getAndValidBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 글이 존재하지 않습니다."));
        if (board.getBoardState() == SALE || board.getBoardState() == SOLD) {
            throw new IllegalArgumentException("잘못된 접근입니다.");
        }
        return board;
    }

    public AccountResponseDto getAccount(Long chatId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 채팅방이 존재하지 않습니다."));
        Account account = accountRepository.findByChatRoom(chatRoom)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 계좌 정보가 존재하지 않습니다."));
        return AccountResponseDto.builder()
                .accountNumber(account.getAccountNumber())
                .bank(account.getBank())
                .holder(account.getHolder())
                .build();
    }

    public WhoResponseDto getRole(Long boardId, Long chatId, Member member) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 글이 존재하지 않습니다."));
        ChatRoom chatRoom = chatRoomRepository.findById(chatId)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 채팅방이 존재하지 않습니다."));
        WhoResponseDto whoResponseDto = new WhoResponseDto();
        Member payer = getPayer(board, chatRoom);
        if (Objects.equals(member.getId(), payer.getId())) {
            whoResponseDto.setRole("buyer");
            return whoResponseDto;
        }
        whoResponseDto.setRole("seller");
        return whoResponseDto;
    }

    public List<Board> writeList(Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 멤버가 존재하지 않습니다."));
        return boardRepository.findByMemberOrderByCreateDateDesc(member);
    }

    public List<Board> tradeList(Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 멤버가 존재하지 않습니다."));
        return boardRepository.findByTraderOrderByCreateDateDesc(member);
    }
}
