package com.forjix.cuentoskilla.service.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.firebase.cloud.StorageClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
public class FirebaseStorageService {

    public String upload(MultipartFile file, String path) {
        try {
            Blob blob = StorageClient.getInstance().bucket().create(path, file.getBytes(), file.getContentType());
            return blob.getBlobId().getName();
        } catch (IOException e) {
            throw new StorageException("Failed to upload file to Firebase", e);
        }
    }

    public String upload(Path localFilePath, String path, String contentType) {
        try {
            byte[] bytes = Files.readAllBytes(localFilePath);
            Blob blob = StorageClient.getInstance().bucket().create(path, bytes, contentType);
            return blob.getBlobId().getName();
        } catch (IOException e) {
            throw new StorageException("Failed to upload file to Firebase", e);
        }
    }

    public String generateSignedUrl(String path) {
        BlobInfo blobInfo = BlobInfo.newBuilder(StorageClient.getInstance().bucket().getName(), path).build();
        URL url = StorageClient.getInstance().bucket().getStorage()
                .signUrl(blobInfo, 10, TimeUnit.MINUTES);
        return url.toString();
    }

    public boolean exists(String path) {
        return StorageClient.getInstance().bucket().get(path) != null;
    }

    public byte[] download(String path) {
        Blob blob = StorageClient.getInstance().bucket().get(path);
        if (blob == null) {
            throw new StorageException("FILE_NOT_FOUND");
        }
        return blob.getContent();
    }

    public void delete(String path) {
        Blob blob = StorageClient.getInstance().bucket().get(path);
        if (blob != null && !blob.delete()) {
            throw new StorageException("Failed to delete file from Firebase");
        }
    }
}
