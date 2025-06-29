package devlava.jwt.controller;

import devlava.jwt.dto.BoardDto;
import devlava.jwt.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping("/list")
    public ResponseEntity<Page<BoardDto>> getBoards(
            @RequestParam(required = false) String searchKeyword,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(boardService.getBoards(searchKeyword, pageable));
    }

    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveBoard(@RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestPart("boardData") BoardDto boardDto) {
        boardService.saveBoard(files, boardDto);
        return ResponseEntity.ok().body(boardDto);
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<BoardDto> getBoard(@PathVariable Long boardId) {
        BoardDto boardDto = boardService.getBoard(boardId);
        return ResponseEntity.ok(boardDto);
    }

    @PutMapping(value = "/{boardId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateBoard(@PathVariable Long boardId,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestPart("boardData") BoardDto boardDto) {
        boardDto.setBoardId(boardId);
        BoardDto updatedBoard = boardService.updateBoard(files, boardDto);
        return ResponseEntity.ok().body(updatedBoard);
    }

    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) throws IOException {
        BoardService.FileDownloadDto downloadDto = boardService.downloadFile(fileId);

        String encodedFileName = URLEncoder.encode(downloadDto.getOriginalFileName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(downloadDto.getResource().contentLength()))
                .body(downloadDto.getResource());
    }
}
