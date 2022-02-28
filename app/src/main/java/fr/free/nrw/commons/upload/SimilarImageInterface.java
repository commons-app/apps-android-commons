package fr.free.nrw.commons.upload;

import fr.free.nrw.commons.data.models.upload.ImageCoordinates;

public interface SimilarImageInterface {
    void showSimilarImageFragment(String originalFilePath, String possibleFilePath,
        ImageCoordinates similarImageCoordinates);
}
