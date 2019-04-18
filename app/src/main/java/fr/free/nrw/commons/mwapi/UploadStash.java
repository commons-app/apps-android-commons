package fr.free.nrw.commons.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UploadStash {
    @NonNull
    private String errorCode;
    @NonNull
    private String resultStatus;
    @NonNull
    private String filename;
    @NonNull
    private String filekey;

    @NonNull
    public final String getErrorCode() {
        return this.errorCode;
    }

    @NonNull
    public final String getResultStatus() {
        return this.resultStatus;
    }

    @NonNull
    public final String getFilename() {
        return this.filename;
    }

    @NonNull
    public final String getFilekey() {
        return this.filekey;
    }

    public UploadStash(@NonNull String errorCode, @NonNull String resultStatus, @NonNull String filename, @NonNull String filekey) {
        this.errorCode = errorCode;
        this.resultStatus = resultStatus;
        this.filename = filename;
        this.filekey = filekey;
    }

    public String toString() {
        return "UploadStash(errorCode=" + this.errorCode + ", resultStatus=" + this.resultStatus + ", filename=" + this.filename + ", filekey=" + this.filekey + ")";
    }

    public int hashCode() {
        return ((this.errorCode.hashCode() * 31 + this.resultStatus.hashCode()
        ) * 31 + this.filename.hashCode()
        ) * 31 + this.filekey.hashCode();
    }

    public boolean equals(@Nullable Object obj) {
        if (this != obj) {
            if (obj instanceof UploadStash) {
                UploadStash that = (UploadStash)obj;
                if (this.errorCode.equals(that.errorCode)
                    && this.resultStatus.equals(that.resultStatus)
                    && this.filename.equals(that.filename)
                        && this.filekey.equals(that.filekey)) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }
}
