package fr.free.nrw.commons.achievements;

/**
 * calculates the level of the user
 */
public class LevelController {

    int calculateLevelUp( Achievements achievements){
        int level = 1;
        if(achievements.getImagesUploaded() >= 100 && achievements.getUniqueUsedImages() >= 45){
            level = 10;
        } else if (achievements.getImagesUploaded() >= 90 && achievements.getUniqueUsedImages() >= 40){
            level = 9;
        } else if (achievements.getImagesUploaded() >= 80 && achievements.getUniqueUsedImages() >= 35){
            level = 8;
        } else if (achievements.getImagesUploaded() >= 70 && achievements.getUniqueUsedImages() >= 30){
            level = 7;
        } else if (achievements.getImagesUploaded() >= 60 && achievements.getUniqueUsedImages() >= 25 ){
            level = 6;
        } else if (achievements.getImagesUploaded() >= 50 && achievements.getUniqueUsedImages() >= 20 ){
            level = 5;
        } else if (achievements.getImagesUploaded() >= 40 && achievements.getUniqueUsedImages() >= 15 ){
            level = 4;
        } else if (achievements.getImagesUploaded() >= 30 && achievements.getUniqueUsedImages() >= 10 ){
            level = 3;
        } else if (achievements.getImagesUploaded() >= 20 && achievements.getUniqueUsedImages() >= 5 ){
            level = 2;
        }

        return level;
    }
}
