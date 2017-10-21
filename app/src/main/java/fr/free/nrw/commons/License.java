package fr.free.nrw.commons;

import android.support.annotation.Nullable;

public class License {
    private String key;
    private String template;
    private String url;
    private String name;

    public License(String key, String template, String url, String name) {
        if (key == null) {
            throw new RuntimeException("License.key must not be null");
        }
        if (template == null) {
            throw new RuntimeException("License.template must not be null");
        }
        this.key = key;
        this.template = template;
        this.url = url;
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public String getTemplate() {
        return template;
    }

    public String getName() {
        if (name == null) {
            // hack
            return getKey();
        } else {
            return name;
        }
    }

    public @Nullable String getUrl(String language) {
        if (url == null) {
            return null;
        } else {
            return url.replace("$lang", language);
        }
    }
}
