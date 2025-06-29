package devlava.jwt.service;

import devlava.jwt.dto.BoardDto;
import devlava.jwt.entity.Board;
import devlava.jwt.entity.BoardRef;
import devlava.jwt.repository.BoardRepository;
import devlava.jwt.repository.BoardRefRepository;
import lombok.Builder;
import lombok.Getter;
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

    @Getter
    @Builder
    public static class FileDownloadDto {
        private final Resource resource;
        private final String originalFileName;
    }

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

            // 3. 파일 처리
            List<Long> remainingFileIds = boardDto.getRemainingFileIds() != null ? boardDto.getRemainingFileIds()
                    : new ArrayList<>();
            deleteExistingFiles(board, remainingFileIds);

            // 4. 새로운 파일 추가
            if (files != null && !files.isEmpty()) {
                String boardUploadPath = createBoardUploadPath(board.getId());
                for (MultipartFile file : files) {
                    if (!file.isEmpty()) {
                        String fileType = determineFileType(file.getOriginalFilename());
                        BoardRef fileRef = createBoardRef(file, fileType, board, boardUploadPath);
                        board.getAttachments().add(fileRef);
                    }
                }
            }

            // 5. 비디오 링크 처리
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

            // 6. 저장
            board = boardRepository.save(board);

            return convertToDto(board);
        } catch (Exception e) {
            // 7. 오류 발생 시 새로 생성된 파일들 정리
            String boardUploadPath = Paths.get(uploadPath, String.valueOf(boardDto.getBoardId())).toString();
            File boardDir = new File(boardUploadPath);
            if (boardDir.exists()) {
                deleteDirectory(boardDir);
            }
            throw new RuntimeException("게시글 수정 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public BoardDto getBoard(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));
        return convertToDto(board);
    }

    private String createBoardUploadPath(Long boardId) {
        String boardUploadPath = Paths.get(uploadPath, String.valueOf(boardId)).toString();
        File uploadDir = new File(boardUploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        return boardUploadPath;
    }

    private void deleteExistingFiles(Board board, List<Long> remainingFileIds) {
        try {
            // 1. 삭제할 첨부파일 목록 생성
            List<BoardRef> attachmentsToDelete = board.getAttachments().stream()
                    .filter(ref -> !remainingFileIds.contains(ref.getId()))
                    .toList();

            // 2. 실제 파일 삭제
            for (BoardRef ref : attachmentsToDelete) {
                if (!"VIDEO_LINK".equals(ref.getFileType())) {
                    File file = new File(ref.getFileUrl());
                    if (file.exists() && !file.delete()) {
                        throw new IOException("파일 삭제 실패: " + ref.getFileUrl());
                    }
                }
            }

            // 3. DB에서 첨부파일 정보 삭제
            boardRefRepository.deleteAll(attachmentsToDelete);
            board.getAttachments().removeAll(attachmentsToDelete);

            // 4. 빈 폴더 정리
            cleanupEmptyDirectories(board.getId());
        } catch (IOException e) {
            throw new RuntimeException("파일 삭제 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private void cleanupEmptyDirectories(Long boardId) throws IOException {
        File boardDir = new File(Paths.get(uploadPath, String.valueOf(boardId)).toString());
        if (boardDir.exists() && (boardDir.list() == null || boardDir.list().length == 0)) {
            if (!boardDir.delete()) {
                throw new IOException("빈 폴더 삭제 실패: " + boardDir.getPath());
            }
        }
    }

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
}
