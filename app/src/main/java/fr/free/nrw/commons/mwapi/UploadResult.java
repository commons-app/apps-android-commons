package fr.free.nrw.commons.mwapi;

import java.util.Date;

public class UploadResult {
    private String errorCode;
    private String resultStatus;
    private Date dateUploaded;
    private String imageUrl;
    private String canonicalFilename;

    UploadResult(String resultStatus, String errorCode) {
        this.resultStatus = resultStatus;
        this.errorCode = errorCode;
    }

    UploadResult(Date dateUploaded, String canonicalFilename, String imageUrl) {
        this.dateUploaded = dateUploaded;
        this.canonicalFilename = canonicalFilename;
        this.imageUrl = imageUrl;
    }

    public Date getDateUploaded() {
        return dateUploaded;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCanonicalFilename() {
        return canonicalFilename;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getResultStatus() {
        return resultStatus;
    }
}
