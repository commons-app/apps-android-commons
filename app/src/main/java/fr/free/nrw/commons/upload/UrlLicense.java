package fr.free.nrw.commons.upload;

import java.util.HashMap;

/**
 * This is a Util class which provides the necessary token to open the Commons License
 * info in the user language
 */
public class UrlLicense {
    HashMap<String,String> urlLicense = new HashMap<String, String>();
    public void initialize(){
        urlLicense.put("en","https://commons.wikimedia.org/wiki/Commons:Licensing");
        urlLicense.put("ar","https://commons.wikimedia.org/wiki/Commons:Licensing/ar");
        urlLicense.put("ast","https://commons.wikimedia.org/wiki/Commons:Licensing/ast");
        urlLicense.put("az","https://commons.wikimedia.org/wiki/Commons:Licensing/az");
        urlLicense.put("be","https://commons.wikimedia.org/wiki/Commons:Licensing/be");
        urlLicense.put("bg","https://commons.wikimedia.org/wiki/Commons:Licensing/bg");
        urlLicense.put("bn","https://commons.wikimedia.org/wiki/Commons:Licensing/bn");
        urlLicense.put("ca","https://commons.wikimedia.org/wiki/Commons:Licensing/ca");
        urlLicense.put("cs","https://commons.wikimedia.org/wiki/Commons:Licensing/cs");
        urlLicense.put("da","https://commons.wikimedia.org/wiki/Commons:Licensing/da");
        urlLicense.put("de","https://commons.wikimedia.org/wiki/Commons:Licensing/de");
        urlLicense.put("el","https://commons.wikimedia.org/wiki/Commons:Licensing/el");
        urlLicense.put("eo","https://commons.wikimedia.org/wiki/Commons:Licensing/eo");
        urlLicense.put("es","https://commons.wikimedia.org/wiki/Commons:Licensing/es");
        urlLicense.put("eu","https://commons.wikimedia.org/wiki/Commons:Licensing/eu");
        urlLicense.put("fa","https://commons.wikimedia.org/wiki/Commons:Licensing/fa");
        urlLicense.put("fi","https://commons.wikimedia.org/wiki/Commons:Licensing/fi");
        urlLicense.put("fr","https://commons.wikimedia.org/wiki/Commons:Licensing/fr");
        urlLicense.put("gl","https://commons.wikimedia.org/wiki/Commons:Licensing/gl");
        urlLicense.put("gsw","https://commons.wikimedia.org/wiki/Commons:Licensing/gsw");
        urlLicense.put("he","https://commons.wikimedia.org/wiki/Commons:Licensing/he");
        urlLicense.put("hi","https://commons.wikimedia.org/wiki/Commons:Licensing/hi");
        urlLicense.put("hu","https://commons.wikimedia.org/wiki/Commons:Licensing/hu");
        urlLicense.put("id","https://commons.wikimedia.org/wiki/Commons:Licensing/id");
        urlLicense.put("is","https://commons.wikimedia.org/wiki/Commons:Licensing/is");
        urlLicense.put("it","https://commons.wikimedia.org/wiki/Commons:Licensing/it");
        urlLicense.put("ja","https://commons.wikimedia.org/wiki/Commons:Licensing/ja");
        urlLicense.put("ka","https://commons.wikimedia.org/wiki/Commons:Licensing/ka");
        urlLicense.put("km","https://commons.wikimedia.org/wiki/Commons:Licensing/km");
        urlLicense.put("ko","https://commons.wikimedia.org/wiki/Commons:Licensing/ko");
        urlLicense.put("ku","https://commons.wikimedia.org/wiki/Commons:Licensing/ku");
        urlLicense.put("mk","https://commons.wikimedia.org/wiki/Commons:Licensing/mk");
        urlLicense.put("mr","https://commons.wikimedia.org/wiki/Commons:Licensing/mr");
        urlLicense.put("ms","https://commons.wikimedia.org/wiki/Commons:Licensing/ms");
        urlLicense.put("my","https://commons.wikimedia.org/wiki/Commons:Licensing/my");
        urlLicense.put("nl","https://commons.wikimedia.org/wiki/Commons:Licensing/nl");
        urlLicense.put("oc","https://commons.wikimedia.org/wiki/Commons:Licensing/oc");
        urlLicense.put("pl","https://commons.wikimedia.org/wiki/Commons:Licensing/pl");
        urlLicense.put("pt","https://commons.wikimedia.org/wiki/Commons:Licensing/pt");
        urlLicense.put("pt-br","https://commons.wikimedia.org/wiki/Commons:Licensing/pt-br");
        urlLicense.put("ro","https://commons.wikimedia.org/wiki/Commons:Licensing/ro");
        urlLicense.put("ru","https://commons.wikimedia.org/wiki/Commons:Licensing/ru");
        urlLicense.put("scn","https://commons.wikimedia.org/wiki/Commons:Licensing/scn");
        urlLicense.put("sk","https://commons.wikimedia.org/wiki/Commons:Licensing/sk");
        urlLicense.put("sl","https://commons.wikimedia.org/wiki/Commons:Licensing/sl");
        urlLicense.put("sv","https://commons.wikimedia.org/wiki/Commons:Licensing/sv");
        urlLicense.put("tr","https://commons.wikimedia.org/wiki/Commons:Licensing/tr");
        urlLicense.put("uk","https://commons.wikimedia.org/wiki/Commons:Licensing/uk");
        urlLicense.put("ur","https://commons.wikimedia.org/wiki/Commons:Licensing/ur");
        urlLicense.put("vi","https://commons.wikimedia.org/wiki/Commons:Licensing/vi");
        urlLicense.put("zh","https://commons.wikimedia.org/wiki/Commons:Licensing/zh");
    }
    public String getLicenseUrl ( String language){
        if (urlLicense.containsKey(language)) {
            return urlLicense.get(language);
        } else {
            return urlLicense.get("en");
        }
    }
}
