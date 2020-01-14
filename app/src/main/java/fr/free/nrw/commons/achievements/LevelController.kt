package fr.free.nrw.commons.achievements

import fr.free.nrw.commons.R

/**
 * calculates the level of the user
 */
class LevelController {
    var level: LevelInfo? = null

    enum class LevelInfo(val levelNumber: Int,
                         val levelStyle: Int,
                         val maxUniqueImages: Int,
                         val maxUploadCount: Int,
                         val minNonRevertPercentage: Int) {
        LEVEL_1(1, R.style.LevelOne, 5, 20, 85), LEVEL_2(2, R.style.LevelTwo, 10, 30, 86), LEVEL_3(3, R.style.LevelThree, 15, 40, 87), LEVEL_4(4, R.style.LevelFour, 20, 50, 88), LEVEL_5(5, R.style.LevelFive, 25, 60, 89), LEVEL_6(6, R.style.LevelOne, 30, 70, 90), LEVEL_7(7, R.style.LevelTwo, 40, 80, 90), LEVEL_8(8, R.style.LevelThree, 45, 90, 90), LEVEL_9(9, R.style.LevelFour, 50, 100, 90), LEVEL_10(10, R.style.LevelFive, 55, 110, 90), LEVEL_11(11, R.style.LevelOne, 60, 120, 90), LEVEL_12(12, R.style.LevelTwo, 65, 130, 90), LEVEL_13(13, R.style.LevelThree, 70, 140, 90), LEVEL_14(14, R.style.LevelFour, 75, 150, 90), LEVEL_15(15, R.style.LevelFive, 80, 160, 90), LEVEL_16(16, R.style.LevelOne, 160, 320, 91), LEVEL_17(17, R.style.LevelTwo, 320, 640, 92), LEVEL_18(18, R.style.LevelThree, 640, 1280, 93), LEVEL_19(19, R.style.LevelFour, 1280, 2560, 94), LEVEL_20(20, R.style.LevelFive, 2560, 5120, 95), LEVEL_21(21, R.style.LevelOne, 5120, 10240, 96), LEVEL_22(22, R.style.LevelTwo, 10240, 20480, 97), LEVEL_23(23, R.style.LevelThree, 20480, 40960, 98), LEVEL_24(24, R.style.LevelFour, 40960, 81920, 98), LEVEL_25(25, R.style.LevelFive, 81920, 163840, 98), LEVEL_26(26, R.style.LevelOne, 163840, 327680, 98), LEVEL_27(27, R.style.LevelTwo, 327680, 655360, 98);

        companion object {
            @JvmStatic
            fun from(imagesUploaded: Int,
                     uniqueImagesUsed: Int,
                     nonRevertRate: Int): LevelInfo {
                var level = LEVEL_15
                for (levelInfo in values()) {
                    if (imagesUploaded < levelInfo.maxUploadCount || uniqueImagesUsed < levelInfo.maxUniqueImages || nonRevertRate < levelInfo.minNonRevertPercentage) {
                        level = levelInfo
                        return level
                    }
                }
                return level
            }
        }

    }
}