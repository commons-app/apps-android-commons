package fr.free.nrw.commons.upload;

import fr.free.nrw.commons.upload.models.ImageCoordinates;

public interface SimilarImageInterface {
    void showSimilarImageFragment(String originalFilePath, String possibleFilePath,
        ImageCoordinates similarImageCoordinates);
}
