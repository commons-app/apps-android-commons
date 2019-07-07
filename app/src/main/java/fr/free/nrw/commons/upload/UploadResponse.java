package fr.free.nrw.commons.upload;

public class UploadResponse {
    private final UploadResult upload;

    public UploadResponse(UploadResult upload) {
        this.upload = upload;
    }

    public UploadResult getUpload() {
        return upload;
    }
}
