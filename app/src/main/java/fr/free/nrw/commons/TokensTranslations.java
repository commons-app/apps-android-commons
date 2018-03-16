package fr.free.nrw.commons;

import java.util.HashMap;

/**
 * Created by Dell on 3/16/2018.
 */

public class TokensTranslations {
    private static HashMap<String,String> translationToken;

    TokensTranslations() {
        translationToken.put("Kazakh", "ab");
        translationToken.put("Afrikaans", "af");
        translationToken.put("Arabic", "ar");
        translationToken.put("Bengali", "as");
        translationToken.put("Asturianu", "ast");
        translationToken.put("azərbaycanca", "az");
        translationToken.put("Bikol Central", "bcl");
        translationToken.put("Bulgarain","bg");
        translationToken.put("বাংলা", "bn");
        translationToken.put("Brezhoneg", "br");
        translationToken.put("Bosanski", "bs");
        translationToken.put("català", "ca");
        translationToken.put("کوردی","ckb");
        translationToken.put("čeština", "cs");
        translationToken.put("kaszëbsczi", "csb");
        translationToken.put("Cymraeg", "cy");
        translationToken.put("dansk", "da");
        translationToken.put("Deutsch", "de");
        translationToken.put("Zazaki", "diq");
        translationToken.put("डोटेली","diq");
        translationToken.put("Ελληνικά","el");
        translationToken.put("euskara","eu");
        translationToken.put("español", "es");
        translationToken.put("فارسی","fa");
        translationToken.put("suomi", "fi");
        translationToken.put("føroyskt", "fo");
        translationToken.put("français", "fr");
        translationToken.put("Nordfriisk", "frr");
        translationToken.put("galego", "gr");
        translationToken.put("Hawaiʻi", "haw");
        translationToken.put("עברית","he");
        translationToken.put("हिन्दी","hi");
        translationToken.put("Hunsrik", "hrx");
        translationToken.put("hornjoserbsce", "hsb");
        translationToken.put("magyar","hu");
        translationToken.put("interlingua","ia");
        translationToken.put("Bahasa Indonesia", "id");
        translationToken.put("íslenska","is");
        translationToken.put("Italian","it");
        translationToken.put("japanese","ja");
        translationToken.put("Basa Jawa","jv");
        translationToken.put("ქართული", "ka");
        translationToken.put("Taqbaylit","kab");
        translationToken.put(" ភាសាខ្មែរ","km");
        translationToken.put("ಕನ್ನಡ", "kn");
        translationToken.put("한국어", "ko");
        translationToken.put("къарачай-малкъар","krc");
        translationToken.put("Кыргызча","ky");
        translationToken.put("latina","la");
        translationToken.put("Lëtzebuergesch","lb");
        translationToken.put("lietuvių", "lt");
        translationToken.put("latviešu","lv");
        translationToken.put("Malagasy","mg");
        translationToken.put("македонски", "mk");
        translationToken.put("മലയാളം","ml");
        translationToken.put("монгол","mn");
        translationToken.put("मराठी","mr");
        translationToken.put("Bahasa Melayu","ms");
        translationToken.put("Malti","mt");
        translationToken.put("norsk bokmål", "nb");
        translationToken.put("नेपाली","ne");
        translationToken.put("Nederlands","nl");
        translationToken.put("occitan","oc");
        translationToken.put("ଓଡ଼ିଆ","or");
        translationToken.put("ਪੰਜਾਬੀ","pa");
        translationToken.put("polsk", "pl");
        translationToken.put("Piemontèis","pms");
        translationToken.put("پښتو","ps");
        translationToken.put("português","pt");
        translationToken.put("română","ro");
        translationToken.put("русский","ru");
        translationToken.put(" سنڌي","sd");
        translationToken.put(" සිංහල","si");
        translationToken.put("slovenčina","sk");
        translationToken.put(" سرائیکی","skr");
        translationToken.put("Basa Sunda","su");
        translationToken.put("svenska","sv");
        translationToken.put("தமிழ்", "ta");
        translationToken.put("ತುಳು", "tcy");
        translationToken.put(" తెలుగు","te");
        translationToken.put(" ไทย","th");
        translationToken.put("Türkçe","tr");
        translationToken.put("українська","uk");
        translationToken.put("اردو","ur");
        translationToken.put("Tiếng Việt","vi");
        translationToken.put(" მარგალური", "xmf");
        translationToken.put("ייִדיש","yi");
    }

    public String getTranslationToken ( String language){
        return translationToken.get(language);
    }
}
