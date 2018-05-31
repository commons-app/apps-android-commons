package fr.free.nrw.commons.achievements;

import fr.free.nrw.commons.R;

/**
 * calculates the level of the user
 */
public class LevelController {

    /**
     * calculated the level of user and return the level information
     * @param achievements
     * @return
     */
    Level calculateLevelUp( Achievements achievements){
        Level level  = new Level ();
        level.setLevel(1);
        level.setLevelStyle(R.style.LevelOne);
        level.setMaximumUploadCount(20);
        level.setMaximumUniqueImagesUsed(5);
        if(achievements.getImagesUploaded() >= 100 && achievements.getUniqueUsedImages() >= 45){
            level.setLevel(10);
            level.setLevelStyle(R.style.LevelFive);
        } else if (achievements.getImagesUploaded() >= 90 && achievements.getUniqueUsedImages() >= 40){
            level.setLevel(9);
            level.setMaximumUniqueImagesUsed(45);
            level.setMaximumUploadCount(100);
            level.setLevelStyle(R.style.LevelFour);
        } else if (achievements.getImagesUploaded() >= 80 && achievements.getUniqueUsedImages() >= 35){
            level.setLevel(8);
            level.setMaximumUniqueImagesUsed(40);
            level.setMaximumUploadCount(90);
            level.setLevelStyle(R.style.LevelThree);
        } else if (achievements.getImagesUploaded() >= 70 && achievements.getUniqueUsedImages() >= 30){
            level.setLevel(7);
            level.setMaximumUniqueImagesUsed(35);
            level.setMaximumUploadCount(80);
            level.setLevelStyle(R.style.LevelTwo);
        } else if (achievements.getImagesUploaded() >= 60 && achievements.getUniqueUsedImages() >= 25 ){
            level.setLevel(6);
            level.setMaximumUniqueImagesUsed(30);
            level.setMaximumUploadCount(70);
            level.setLevelStyle(R.style.LevelOne);
        } else if (achievements.getImagesUploaded() >= 50 && achievements.getUniqueUsedImages() >= 20 ){
            level.setLevel(5);
            level.setMaximumUniqueImagesUsed(25);
            level.setMaximumUploadCount(60);
            level.setLevelStyle(R.style.LevelFive);
        } else if (achievements.getImagesUploaded() >= 40 && achievements.getUniqueUsedImages() >= 15 ){
            level.setLevel(4);
            level.setMaximumUniqueImagesUsed(20);
            level.setMaximumUploadCount(50);
            level.setLevelStyle(R.style.LevelFour);
        } else if (achievements.getImagesUploaded() >= 30 && achievements.getUniqueUsedImages() >= 10 ){
            level.setLevel(3);
            level.setMaximumUniqueImagesUsed(15);
            level.setMaximumUploadCount(40);
            level.setLevelStyle(R.style.LevelThree);
        } else if (achievements.getImagesUploaded() >= 20 && achievements.getUniqueUsedImages() >= 5 ){
            level.setLevel(2);
            level.setMaximumUniqueImagesUsed(10);
            level.setMaximumUploadCount(30);
            level.setLevelStyle(R.style.LevelTwo);
        }

        return level;
    }
}
