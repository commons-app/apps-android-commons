package fr.free.nrw.commons.achievements;

import fr.free.nrw.commons.R;

/**
 * calculates the level of the user
 */
public class LevelController {

    public LevelInfo level;
    public enum LevelInfo{
        LEVEL_15(15,R.style.LevelFive, 80, 160),
        LEVEL_14(14,R.style.LevelFour, 75 , 150),
        LEVEL_13(13,R.style.LevelThree, 70, 140),
        LEVEL_12(12,R.style.LevelTwo,65 , 130),
        LEVEL_11(11,R.style.LevelOne, 60, 120),
        LEVEL_10(10, R.style.LevelFive, 55, 110),
        LEVEL_9(9, R.style.LevelFour, 50, 100),
        LEVEL_8(8, R.style.LevelThree, 45, 90),
        LEVEL_7(7, R.style.LevelTwo, 40, 80),
        LEVEL_6(6,R.style.LevelOne,30,70),
        LEVEL_5(5, R.style.LevelFive, 25, 60),
        LEVEL_4(4, R.style.LevelFour,20,50),
        LEVEL_3(3, R.style.LevelThree, 15,40),
        LEVEL_2(2, R.style.LevelTwo, 10, 30),
        LEVEL_1(1, R.style.LevelOne, 5, 20 );

        private int levelNumber;
        private int levelStyle;
        private int maxUniqueImages;
        private int maxUploadCount;

        LevelInfo(int levelNumber, int levelStyle, int maxUniqueImages, int maxUploadCount) {
            this.levelNumber = levelNumber;
            this.levelStyle = levelStyle;
            this.maxUniqueImages = maxUniqueImages;
            this.maxUploadCount = maxUploadCount;
        }

        public static LevelInfo from(int imagesUploaded, int uniqueImagesUsed) {
            LevelInfo level = LEVEL_1;

            for (LevelInfo levelInfo : LevelInfo.values()) {
                if (imagesUploaded > levelInfo.maxUploadCount && uniqueImagesUsed > levelInfo.maxUniqueImages) {
                    level = levelInfo;
                    return level;
                }
            }
            return level;
        }

        public int getLevelStyle() {
            return levelStyle;
        }

        public int getLevelNumber() {
            return levelNumber;
        }

        public void setLevelStyle(int levelStyle) {
            this.levelStyle = levelStyle;
        }

        public int getMaxUniqueImages() {
            return maxUniqueImages;
        }

        public int getMaxUploadCount() {
            return maxUploadCount;
        }
    }

}
