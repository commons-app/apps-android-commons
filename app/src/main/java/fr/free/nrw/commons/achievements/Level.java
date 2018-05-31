package fr.free.nrw.commons.achievements;

/**
 * Contains information about the levels
 */
public class Level {
    private int maximumUploadCount;
    private int maximumUniqueImagesUsed;
    private int revertRate;
    private int level;
    private int levelStyle;

    public int getMaximumUniqueImagesUsed() {
        return maximumUniqueImagesUsed;
    }

    public int getMaximumUploadCount() {
        return maximumUploadCount;
    }

    public void setMaximumUploadCount(int maximumUploadCount) {
        this.maximumUploadCount = maximumUploadCount;
    }

    public void setMaximumUniqueImagesUsed(int maximumUniqueImagesUsed) {
        this.maximumUniqueImagesUsed = maximumUniqueImagesUsed;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setLevelStyle(int levelStyle) {
        this.levelStyle = levelStyle;
    }

    public int getLevelStyle() {
        return levelStyle;
    }

}
