package kdt.web_ide.post.service;

import kdt.web_ide.boards.entity.Board;
import kdt.web_ide.boards.entity.BoardRepository;
import kdt.web_ide.common.exception.CustomException;
import kdt.web_ide.common.exception.ErrorCode;
import kdt.web_ide.file.service.S3Service;
import kdt.web_ide.post.dto.PostRequestDto;
import kdt.web_ide.post.dto.PostResponseDto;
import kdt.web_ide.post.entity.Post;
import kdt.web_ide.post.entity.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final BoardRepository boardRepository; // BoardRepository 추가
    private final S3Service s3Service;

    public PostResponseDto createPost(PostRequestDto requestDto) {
        // 게시판 조회
        Board board = boardRepository.findById(requestDto.getBoardId())
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND, "Board not found for ID: " + requestDto.getBoardId()));

        // 파일 제목 생성 및 빈 파일 업로드
        String fileName = generateFileName(requestDto.getName(), String.valueOf(requestDto.getLanguage()));
        String filePath = uploadEmptyFileToS3(fileName, board.getId());

        // 게시글 생성
        Post post = Post.builder()
                .board(board)
                .name(requestDto.getName())
                .language(requestDto.getLanguage())
                .filePath(filePath)
                .build();

        Post savedPost = postRepository.save(post);
        return mapToResponseDto(savedPost);
    }

    public PostResponseDto getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND, "Post not found for ID: " + id));
        return mapToResponseDto(post);
    }

    public List<PostResponseDto> getAllPosts() {
        return postRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private String uploadEmptyFileToS3(String fileName, Long boardId) {
        try {
            String s3Path = "boards/" + boardId + "/" + fileName;
            ByteArrayInputStream emptyContent = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
            s3Service.uploadFile(emptyContent, s3Path, "text/plain");
            return s3Path;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.S3_UPLOAD_ERROR, "Failed to upload empty file to S3 for file: " + fileName);
        }
    }

    private String generateFileName(String postName, String language) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String extension = getFileExtension(language);
        return postName.replaceAll("\\s+", "_") + "_" + timestamp + "_" + UUID.randomUUID() + extension;
    }

    private String getFileExtension(String language) {
        return switch (language.toUpperCase()) {
            case "PYTHON" -> ".py";
            case "JAVA" -> ".java";
            case "JAVASCRIPT" -> ".js";
            case "C++" -> ".cpp";
            default -> ".txt";
        };
    }

    private PostResponseDto mapToResponseDto(Post post) {
        return PostResponseDto.builder()
                .id(post.getId())
                .boardId(post.getBoard().getId().intValue()) // Board와 매핑
                .name(post.getName())
                .language(post.getLanguage())
                .filePath(post.getFilePath())
                .createdAt(post.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();
    }

    public String downloadFile(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND, "Post not found for ID: " + postId));
        return s3Service.getFileUrl(post.getFilePath());
    }

    public String executeFile(Long postId, String input) throws IOException, InterruptedException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND, "Post not found for ID: " + postId));
        return s3Service.executeFile(post.getFilePath(), input);

    }

    public void modifyFileContent(Long postId, String newContent) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND, "Post not found for ID: " + postId));
        s3Service.modifyFileContent(post.getFilePath(), newContent);
    }
}
