package devlava.jwt.service;

import devlava.jwt.dto.BoardDto;
import devlava.jwt.dto.FileDownloadDto;
import devlava.jwt.entity.Board;
import devlava.jwt.entity.BoardRef;
import devlava.jwt.repository.BoardRepository;
import devlava.jwt.repository.BoardRefRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardRefRepository boardRefRepository;

    @Value("${file.upload.path}")
    private String uploadPath;

    /* 게시판 전체 조회 */
    @Transactional(readOnly = true)
    public Page<BoardDto> getBoards(String searchKeyword, Pageable pageable) {
        Page<Board> boardPage;
        if (StringUtils.hasText(searchKeyword)) {
            boardPage = boardRepository.findByTitleContainingOrContentContaining(searchKeyword, searchKeyword,
                    pageable);
        } else {
            boardPage = boardRepository.findAll(pageable);
        }
        return boardPage.map(this::convertToDto);
    }

    /* 게시글 저장 */
    public BoardDto saveBoard(List<MultipartFile> files, BoardDto boardDto) {
        // 1. Board 엔티티 생성 및 저장
        Board board = Board.builder()
                .title(boardDto.getTitle())
                .content(boardDto.getContent())
                .attachments(new ArrayList<>())
                .build();

        board = boardRepository.save(board); // 먼저 저장하여 ID 생성

        // 2. 게시글 ID 기준으로 폴더 생성
        String boardUploadPath = createBoardUploadPath(board.getId());

        // 3. 파일 처리 및 BoardRef 엔티티 생성
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String fileType = determineFileType(file.getOriginalFilename());
                    BoardRef fileRef = createBoardRef(file, fileType, board, boardUploadPath);
                    board.getAttachments().add(fileRef);
                }
            }
        }

        // 4. 비디오 링크 처리
        if (boardDto.getLinks() != null && !boardDto.getLinks().isEmpty()) {
            for (String link : boardDto.getLinks()) {
                BoardRef videoRef = BoardRef.builder()
                        .board(board)
                        .fileType("VIDEO_LINK")
                        .fileUrl(link)
                        .originalFileName(link)
                        .storedFileName(link)
                        .build();
                board.getAttachments().add(videoRef);
            }
        }

        // 5. 첨부파일 정보 저장
        board = boardRepository.save(board);

        return convertToDto(board);
    }

    /* 게시글 수정 */
    @Transactional
    public BoardDto updateBoard(List<MultipartFile> files, BoardDto boardDto) {
        try {
            // 1. 기존 게시글 조회
            Board board = boardRepository.findById(boardDto.getBoardId())
                    .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

            // 2. 기본 정보 업데이트 (빈 값이 아닐 경우에만 업데이트)
            if (StringUtils.hasText(boardDto.getTitle())) {
                board.setTitle(boardDto.getTitle());
            }
            if (StringUtils.hasText(boardDto.getContent())) {
                board.setContent(boardDto.getContent());
            }

            // 3. 파일 업데이트가 있는 경우에만 기존 파일 삭제
            if ((files != null && !files.isEmpty()) ||
                    (boardDto.getLinks() != null && !boardDto.getLinks().isEmpty())) {
                deleteExistingFiles(board);
            }

            // 4. 게시글 ID 기준으로 폴더 생성
            String boardUploadPath = createBoardUploadPath(board.getId());

            // 5. 새로운 파일 처리
            if (files != null && !files.isEmpty()) {
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        String fileType = determineFileType(file.getOriginalFilename());
                        BoardRef fileRef = createBoardRef(file, fileType, board, boardUploadPath);
                        board.getAttachments().add(fileRef);
                    }
                }
            }

            // 6. 비디오 링크 처리
            if (boardDto.getLinks() != null && !boardDto.getLinks().isEmpty()) {
                for (String link : boardDto.getLinks()) {
                    BoardRef videoRef = BoardRef.builder()
                            .board(board)
                            .fileType("VIDEO_LINK")
                            .fileUrl(link)
                            .originalFileName(link)
                            .storedFileName(link)
                            .build();
                    board.getAttachments().add(videoRef);
                }
            }

            // 7. 저장
            board = boardRepository.save(board);

            return convertToDto(board);
        } catch (Exception e) {
            // 8. 오류 발생 시 새로 생성된 파일들 정리
            String boardUploadPath = Paths.get(uploadPath, String.valueOf(boardDto.getBoardId())).toString();
            File boardDir = new File(boardUploadPath);
            if (boardDir.exists()) {
                deleteDirectory(boardDir);
            }
            throw new RuntimeException("게시글 수정 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /* 게시글 상세 조회 */
    @Transactional(readOnly = true)
    public BoardDto getBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
        return convertToDto(board);
    }

    /* 조회 - DTO 변환 */
    private BoardDto convertToDto(Board board) {
        BoardDto dto = BoardDto.builder()
                .boardId(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .createdAt(board.getCreatedAt())
                .links(new ArrayList<>())
                .attachments(new ArrayList<>())
                .build();

        // 첨부파일 정보 변환
        for (BoardRef ref : board.getAttachments()) {
            if ("VIDEO_LINK".equals(ref.getFileType())) {
                dto.getLinks().add(ref.getFileUrl());
            } else {
                BoardDto.BoardRefDto refDto = BoardDto.BoardRefDto.builder()
                        .id(ref.getId())
                        .originalFileName(ref.getOriginalFileName())
                        .storedFileName(ref.getStoredFileName())
                        .fileType(ref.getFileType())
                        .fileUrl(ref.getFileUrl())
                        .fileSize(ref.getFileSize())
                        .createdAt(ref.getCreatedAt())
                        .build();
                dto.getAttachments().add(refDto);
            }
        }

        return dto;
    }

    /* 파일 다운로드 */
    @Transactional(readOnly = true)
    public FileDownloadDto downloadFile(Long fileId) {
        BoardRef boardRef = boardRefRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

        if ("VIDEO_LINK".equals(boardRef.getFileType())) {
            throw new RuntimeException("비디오 링크는 다운로드할 수 없습니다.");
        }

        try {
            Path filePath = Paths.get(boardRef.getFileUrl());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return FileDownloadDto.builder()
                        .resource(resource)
                        .originalFileName(boardRef.getOriginalFileName())
                        .build();
            } else {
                throw new RuntimeException("파일을 읽을 수 없습니다.");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("파일을 찾을 수 없습니다.", e);
        }
    }

    /* 첨부 파일 업로드 로직 */
    private BoardRef createBoardRef(MultipartFile file, String fileType, Board board, String boardUploadPath) {
        try {
            String originalFileName = file.getOriginalFilename();
            String storedFileName = UUID.randomUUID().toString() + "_" + originalFileName;

            // 파일 저장
            Path filePath = Paths.get(boardUploadPath, storedFileName);
            Files.write(filePath, file.getBytes());

            return BoardRef.builder()
                    .board(board)
                    .originalFileName(originalFileName)
                    .storedFileName(storedFileName)
                    .fileType(fileType)
                    .fileUrl(filePath.toString())
                    .fileSize(file.getSize())
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    /* 첨부 파일 업로드 - 폴더 분류 */
    private String createBoardUploadPath(Long boardId) {
        String boardUploadPath = Paths.get(uploadPath, String.valueOf(boardId)).toString();
        File uploadDir = new File(boardUploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        return boardUploadPath;
    }

    /* 첨부 파일 업로드 - 파일 타입 분류 */
    private String determineFileType(String fileName) {
        if (fileName == null)
            return "OTHER";
        fileName = fileName.toLowerCase();
        if (fileName.endsWith(".pdf"))
            return "PDF";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")
                || fileName.endsWith(".gif"))
            return "IMAGE";
        if (fileName.endsWith(".mp4") || fileName.endsWith(".avi") || fileName.endsWith(".mov"))
            return "VIDEO";
        return "OTHER";
    }

    /* 첨부 파일 수정 - 파일 삭제 */
    private void deleteExistingFiles(Board board) {
        try {
            // 1. DB에서 첨부파일 정보 삭제 준비
            List<BoardRef> attachmentsToDelete = new ArrayList<>(board.getAttachments());

            // 2. 실제 파일 삭제
            for (BoardRef ref : attachmentsToDelete) {
                if (!"VIDEO_LINK".equals(ref.getFileType())) {
                    File file = new File(ref.getFileUrl());
                    if (file.exists() && !file.delete()) {
                        throw new IOException("파일 삭제 실패: " + ref.getFileUrl());
                    }
                }
            }

            // 3. 게시글 폴더 삭제
            File boardDir = new File(Paths.get(uploadPath, String.valueOf(board.getId())).toString());
            if (boardDir.exists()) {
                File[] files = boardDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.delete()) {
                            throw new IOException("파일 삭제 실패: " + file.getPath());
                        }
                    }
                }
                if (!boardDir.delete()) {
                    throw new IOException("폴더 삭제 실패: " + boardDir.getPath());
                }
            }

            // 4. DB에서 첨부파일 정보 삭제
            boardRefRepository.deleteAll(attachmentsToDelete);
            board.getAttachments().clear();
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /* 첨부 파일 수정 - 경로 삭제 */
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}
