package com.anju.service;

import com.anju.common.BusinessException;
import com.anju.dto.FileUploadInitRequest;
import com.anju.dto.FileUploadInitResponse;
import com.anju.entity.FileMetadata;
import com.anju.entity.FileUploadChunk;
import com.anju.entity.FileUploadSession;
import com.anju.entity.FileVersion;
import com.anju.repository.FileMetadataRepository;
import com.anju.repository.FileUploadChunkRepository;
import com.anju.repository.FileUploadSessionRepository;
import com.anju.repository.FileVersionRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileUploadSessionRepository fileUploadSessionRepository;
    private final FileUploadChunkRepository fileUploadChunkRepository;
    private final FileVersionRepository fileVersionRepository;
    private final Map<String, ConcurrentLinkedDeque<LocalDateTime>> uploadRateWindow = new ConcurrentHashMap<>();

    public FileService(FileMetadataRepository fileMetadataRepository,
                       FileUploadSessionRepository fileUploadSessionRepository,
                       FileUploadChunkRepository fileUploadChunkRepository,
                       FileVersionRepository fileVersionRepository) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.fileUploadSessionRepository = fileUploadSessionRepository;
        this.fileUploadChunkRepository = fileUploadChunkRepository;
        this.fileVersionRepository = fileVersionRepository;
    }

    @Transactional
    public FileUploadInitResponse initUpload(FileUploadInitRequest request) {
        validateInit(request);
        var existing = fileMetadataRepository.findByContentHashAndDeletedFalse(request.getContentHash());
        if (existing.isPresent()) {
            return new FileUploadInitResponse(null, true, existing.get().getId(), List.of());
        }
        String uploadId = "UP-" + UUID.randomUUID();
        FileUploadSession session = new FileUploadSession();
        session.setUploadId(uploadId);
        session.setFileName(request.getFileName());
        session.setContentHash(request.getContentHash());
        session.setTotalChunks(request.getTotalChunks());
        session.setMimeType(request.getMimeType());
        session.setFileSize(request.getFileSize());
        session.setChunkSize(request.getChunkSize());
        session.setStatus("UPLOADING");
        fileUploadSessionRepository.save(session);
        return new FileUploadInitResponse(uploadId, false, null, List.of());
    }

    @Transactional
    public Map<String, Object> uploadChunk(String uploadId, Integer chunkIndex, Long chunkBytes, MultipartFile file) throws IOException {
        enforceUploadThrottle(uploadId, chunkBytes == null || chunkBytes <= 0 ? 1024 * 512L : chunkBytes);
        FileUploadSession session = getSession(uploadId);
        if (chunkIndex == null || chunkIndex < 0 || chunkIndex >= session.getTotalChunks()) {
            throw new BusinessException("Invalid chunk index");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("chunk file is required");
        }

        Path chunkPath = chunkPath(uploadId, chunkIndex);
        Files.createDirectories(chunkPath.getParent());
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, chunkPath, StandardCopyOption.REPLACE_EXISTING);
        }
        if (!fileUploadChunkRepository.existsByUploadIdAndChunkIndex(uploadId, chunkIndex)) {
            FileUploadChunk chunk = new FileUploadChunk();
            chunk.setUploadId(uploadId);
            chunk.setChunkIndex(chunkIndex);
            fileUploadChunkRepository.save(chunk);
        }
        return Map.of("uploadId", uploadId, "chunkIndex", chunkIndex, "accepted", true, "stored", true);
    }

    @Transactional
    public Map<String, Object> completeUpload(String uploadId) {
        FileUploadSession session = getSession(uploadId);
        long uploadedCount = fileUploadChunkRepository.countByUploadId(uploadId);
        if (uploadedCount != session.getTotalChunks()) {
            throw new BusinessException("Missing chunks, upload not complete");
        }
        var existing = fileMetadataRepository.findByContentHashAndDeletedFalse(session.getContentHash());
        if (existing.isPresent()) {
            fileUploadSessionRepository.deleteByUploadId(uploadId);
            return Map.of("fileId", existing.get().getId(), "dedupHit", true);
        }
        FileMetadata metadata = new FileMetadata();
        metadata.setFileNo("FILE-" + UUID.randomUUID());
        metadata.setFileName(session.getFileName());
        metadata.setContentHash(session.getContentHash());
        metadata.setMimeType(session.getMimeType() == null || session.getMimeType().isBlank()
                ? "application/octet-stream"
                : session.getMimeType());
        metadata.setFileSize(session.getFileSize() == null ? 0L : session.getFileSize());
        metadata.setChunkSize(session.getChunkSize() == null ? 0 : session.getChunkSize());
        metadata.setTotalChunks(session.getTotalChunks());
        metadata.setUploadedChunks((int) uploadedCount);
        metadata.setCurrentVersion(1);
        metadata.setStatus("ACTIVE");
        metadata.setDeleted(false);
        metadata.setDeleteExpireAt(null);
        FileMetadata saved = fileMetadataRepository.save(metadata);

        // Assemble chunks and verify hash before finalizing storage.
        Path assembled = assembleChunks(uploadId, session.getTotalChunks(), saved.getId(), 1);
        String sha256 = sha256Hex(assembled);
        if (!sha256.equalsIgnoreCase(session.getContentHash())) {
            throw new BusinessException("contentHash verification failed");
        }

        FileVersion v1 = new FileVersion();
        v1.setFileId(saved.getId());
        v1.setVersionNo(1);
        v1.setChangeNote("Initial version");
        fileVersionRepository.save(v1);
        fileUploadSessionRepository.deleteByUploadId(uploadId);
        return Map.of("fileId", saved.getId(), "dedupHit", false, "version", 1);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFile(Long fileId) {
        return toView(getFileOrThrow(fileId));
    }

    @Transactional
    public Map<String, Object> newVersionFromUpload(Long fileId, String uploadId, String note) {
        FileMetadata file = getFileOrThrow(fileId);
        if (uploadId == null || uploadId.isBlank()) {
            throw new BusinessException("uploadId is required");
        }
        FileUploadSession session = getSession(uploadId);
        long uploadedCount = fileUploadChunkRepository.countByUploadId(uploadId);
        if (uploadedCount != session.getTotalChunks()) {
            throw new BusinessException("Missing chunks, upload not complete");
        }
        int next = fileVersionRepository.findByFileIdOrderByVersionNoAsc(fileId).stream()
                .mapToInt(FileVersion::getVersionNo).max().orElse(0) + 1;

        Path assembled = assembleChunks(uploadId, session.getTotalChunks(), fileId, next);
        String sha256 = sha256Hex(assembled);
        if (!sha256.equalsIgnoreCase(session.getContentHash())) {
            throw new BusinessException("contentHash verification failed");
        }

        FileVersion version = new FileVersion();
        version.setFileId(fileId);
        version.setVersionNo(next);
        version.setChangeNote(note == null ? "" : note);
        fileVersionRepository.save(version);

        file.setCurrentVersion(next);
        file.setContentHash(session.getContentHash());
        if (session.getMimeType() != null && !session.getMimeType().isBlank()) {
            file.setMimeType(session.getMimeType());
        }
        if (session.getFileSize() != null) {
            file.setFileSize(session.getFileSize());
        }
        if (session.getChunkSize() != null) {
            file.setChunkSize(session.getChunkSize());
        }
        fileMetadataRepository.save(file);
        fileUploadSessionRepository.deleteByUploadId(uploadId);
        return Map.of("fileId", fileId, "version", next);
    }

    @Transactional
    public Map<String, Object> rollback(Long fileId, Integer targetVersion) {
        FileMetadata file = getFileOrThrow(fileId);
        if (targetVersion == null || fileVersionRepository.findByFileIdAndVersionNo(fileId, targetVersion).isEmpty()) {
            throw new BusinessException("Invalid target version");
        }
        file.setCurrentVersion(targetVersion);
        fileMetadataRepository.save(file);
        return Map.of("fileId", fileId, "currentVersion", targetVersion);
    }

    @Transactional
    public Map<String, Object> recycle(Long fileId) {
        FileMetadata file = getFileOrThrow(fileId);
        file.setDeleted(true);
        file.setStatus("DELETED");
        file.setDeleteExpireAt(LocalDateTime.now().plusDays(30));
        return toView(fileMetadataRepository.save(file));
    }

    @Transactional
    public Map<String, Object> restore(Long fileId) {
        FileMetadata file = getFileOrThrow(fileId);
        if (!file.isDeleted()) {
            return toView(file);
        }
        if (file.getDeleteExpireAt() != null && file.getDeleteExpireAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Recycle bin retention expired");
        }
        file.setDeleted(false);
        file.setStatus("ACTIVE");
        file.setDeleteExpireAt(null);
        return toView(fileMetadataRepository.save(file));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRecycleBin() {
        return fileMetadataRepository.findByDeletedTrue().stream().map(this::toView).toList();
    }

    @Transactional
    public Map<String, Object> purge(Long fileId) {
        FileMetadata file = getFileOrThrow(fileId);
        if (!file.isDeleted()) {
            throw new BusinessException("File must be in recycle bin before purge");
        }
        fileVersionRepository.deleteByFileId(fileId);
        fileMetadataRepository.delete(file);
        deleteStoredFiles(fileId);
        return Map.of("fileId", fileId, "purged", true);
    }

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void cleanupExpiredRecycleBin() {
        List<FileMetadata> expired = fileMetadataRepository.findByDeletedTrueAndDeleteExpireAtBefore(LocalDateTime.now());
        for (FileMetadata file : expired) {
            Long fileId = file.getId();
            fileVersionRepository.deleteByFileId(fileId);
            fileMetadataRepository.delete(file);
            deleteStoredFiles(fileId);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> preview(Long fileId) {
        FileMetadata file = getFileOrThrow(fileId);
        String previewType = previewType(file.getMimeType());
        return Map.of(
                "fileId", file.getId(),
                "fileName", file.getFileName(),
                "mimeType", file.getMimeType(),
                "previewType", previewType,
                "previewUrl", "/api/v1/files/" + file.getId() + "/preview-content"
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> previewContent(Long fileId) {
        FileMetadata file = getFileOrThrow(fileId);
        String type = previewType(file.getMimeType());
        return Map.of(
                "fileId", fileId,
                "previewType", type,
                "supported", !"binary".equals(type) && !"unknown".equals(type),
                "renderMode", switch (type) {
                    case "image", "audio", "video" -> "STREAM";
                    case "document" -> "DOCUMENT_VIEW";
                    default -> "DOWNLOAD_ONLY";
                },
                "contentUrl", "/api/v1/files/" + fileId + "/content"
        );
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadCurrentVersion(Long fileId) {
        FileMetadata file = getFileOrThrow(fileId);
        Path path = storedFilePath(fileId, file.getCurrentVersion());
        if (!Files.exists(path)) {
            throw new BusinessException("stored file content not found");
        }
        Resource resource = new FileSystemResource(path.toFile());
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(file.getMimeType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header("Content-Disposition", "inline; filename=\"" + file.getFileName() + "\"")
                .body(resource);
    }

    private void validateInit(FileUploadInitRequest request) {
        if (request.getFileName() == null || request.getFileName().isBlank()) {
            throw new BusinessException("fileName is required");
        }
        if (request.getContentHash() == null || request.getContentHash().isBlank()) {
            throw new BusinessException("contentHash is required");
        }
        if (request.getTotalChunks() == null || request.getTotalChunks() <= 0) {
            throw new BusinessException("totalChunks must be positive");
        }
        if (request.getChunkSize() == null || request.getChunkSize() <= 0) {
            throw new BusinessException("chunkSize must be positive");
        }
        if (request.getFileSize() == null || request.getFileSize() < 0) {
            throw new BusinessException("fileSize must be >= 0");
        }
    }

    private FileUploadSession getSession(String uploadId) {
        return fileUploadSessionRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new BusinessException("Upload session not found"));
    }

    private FileMetadata getFileOrThrow(Long fileId) {
        return fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("File not found"));
    }

    private Map<String, Object> toView(FileMetadata file) {
        List<Integer> versions = fileVersionRepository.findByFileIdOrderByVersionNoAsc(file.getId()).stream()
                .map(FileVersion::getVersionNo)
                .toList();
        return Map.of(
                "fileId", file.getId(),
                "fileName", file.getFileName(),
                "contentHash", file.getContentHash(),
                "status", file.getStatus(),
                "isDeleted", file.isDeleted(),
                "expireAt", file.getDeleteExpireAt(),
                "currentVersion", file.getCurrentVersion(),
                "versions", new ArrayList<>(versions)
        );
    }

    private void enforceUploadThrottle(String uploadId, long bytes) {
        LocalDateTime now = LocalDateTime.now();
        ConcurrentLinkedDeque<LocalDateTime> window = uploadRateWindow.computeIfAbsent(uploadId, k -> new ConcurrentLinkedDeque<>());
        window.addLast(now);
        while (!window.isEmpty() && window.peekFirst().isBefore(now.minusSeconds(1))) {
            window.removeFirst();
        }
        if (window.size() > 10) {
            throw new BusinessException("Upload throttled: too many chunk requests per second");
        }
        // Rough bandwidth control: max ~5MB/s equivalent by request budget.
        long estimatedBytesPerSecond = window.size() * bytes;
        if (estimatedBytesPerSecond > 5L * 1024L * 1024L) {
            throw new BusinessException("Upload bandwidth throttled");
        }
    }

    private String previewType(String mimeType) {
        if (mimeType == null) {
            return "unknown";
        }
        if (mimeType.startsWith("image/")) {
            return "image";
        }
        if (mimeType.startsWith("audio/")) {
            return "audio";
        }
        if (mimeType.startsWith("video/")) {
            return "video";
        }
        if (mimeType.contains("pdf") || mimeType.contains("word") || mimeType.contains("excel") || mimeType.contains("text")) {
            return "document";
        }
        return "binary";
    }

    private Path storageRoot() {
        return Path.of("storage");
    }

    private Path chunkPath(String uploadId, int chunkIndex) {
        return storageRoot().resolve("chunks").resolve(uploadId).resolve(chunkIndex + ".part");
    }

    private Path storedFilePath(Long fileId, int version) {
        return storageRoot().resolve("files").resolve(String.valueOf(fileId)).resolve("v" + version + ".bin");
    }

    private Path assembleChunks(String uploadId, int totalChunks, Long fileId, int version) {
        try {
            Path out = storedFilePath(fileId, version);
            Files.createDirectories(out.getParent());
            // Write sequentially; streaming to file.
            try (var os = Files.newOutputStream(out)) {
                for (int i = 0; i < totalChunks; i++) {
                    Path part = chunkPath(uploadId, i);
                    if (!Files.exists(part)) {
                        throw new BusinessException("missing chunk file: " + i);
                    }
                    Files.copy(part, os);
                }
            }
            return out;
        } catch (IOException ex) {
            throw new BusinessException("failed to assemble chunks");
        }
    }

    private String sha256Hex(Path file) {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[8192];
            int read;
            while ((read = in.read(buf)) > 0) {
                digest.update(buf, 0, read);
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new BusinessException("failed to compute sha256");
        }
    }

    private void deleteStoredFiles(Long fileId) {
        Path fileDir = storageRoot().resolve("files").resolve(String.valueOf(fileId));
        if (!Files.exists(fileDir)) {
            return;
        }
        try (var walk = Files.walk(fileDir)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Best-effort cleanup only; metadata is already purged.
                        }
                    });
        } catch (IOException ignored) {
            // Best-effort cleanup only.
        }
    }
}
