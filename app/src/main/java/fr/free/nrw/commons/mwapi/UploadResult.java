package fr.free.nrw.commons.mwapi;

import java.util.Date;

public class UploadResult {
    private String errorCode;
    private String resultStatus;
    private Date dateUploaded;
    private String imageUrl;
    private String canonicalFilename;

    /**
     * Minimal constructor
     *
     * @param resultStatus Upload result status
     * @param errorCode    Upload error code
     */
    UploadResult(String resultStatus, String errorCode) {
        this.resultStatus = resultStatus;
        this.errorCode = errorCode;
    }

    /**
     * Full-fledged constructor
     * @param resultStatus Upload result status
     * @param dateUploaded Uploaded date
     * @param canonicalFilename Uploaded file name
     * @param imageUrl Uploaded image file name
     */
    UploadResult(String resultStatus, Date dateUploaded, String canonicalFilename, String imageUrl) {
        this.resultStatus = resultStatus;
        this.dateUploaded = dateUploaded;
        this.canonicalFilename = canonicalFilename;
        this.imageUrl = imageUrl;
    }

    /**
     * Gets uploaded date
     * @return Upload date
     */
    public Date getDateUploaded() {
        return dateUploaded;
    }

    /**
     * Gets image url
     * @return Uploaded image url
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Gets canonical file name
     * @return Uploaded file name
     */
    public String getCanonicalFilename() {
        return canonicalFilename;
    }

    /**
     * Gets upload error code
     * @return Error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Gets upload result status
     * @return Upload result status
     */
    public String getResultStatus() {
        return resultStatus;
    }
}
