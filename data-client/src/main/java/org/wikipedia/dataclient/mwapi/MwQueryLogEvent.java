package org.wikipedia.dataclient.mwapi;

import org.wikipedia.model.BaseModel;
import org.wikipedia.util.DateUtil;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

@SuppressWarnings("unused")
public class MwQueryLogEvent extends BaseModel {
    private int logid;
    private int ns;
    private int index;
    private String title;
    private int pageid;
    private Params params;
    private String type;
    private String action;
    private String user;
    private int userid;
    private String timestamp;
    private String comment;
    private String parsedcomment;
    private List<String> tags;

    public int logid() {
        return logid;
    }

    public int ns() {
        return ns;
    }

    public int index() {
        return index;
    }

    public String title() {
        return title;
    }

    public int pageid() {
        return pageid;
    }

    public String type() {
        return type;
    }

    public String action() {
        return action;
    }

    public String user() {
        return user;
    }

    public int userid() {
        return userid;
    }

    public String timestamp() {
        return timestamp;
    }

    public Date date(){
        try {
            return DateUtil.iso8601DateParse(timestamp);
        } catch (ParseException e) {
            return null;
        }
    }

    public String comment() {
        return comment;
    }

    public String parsedcomment() {
        return parsedcomment;
    }

    public List<String> tags() {
        return tags;
    }

    public boolean isDeleted() {
        return pageid==0;
    }

    public Params params() {
        return params;
    }

    public static class Params{
        private String img_sha1;
        private String img_timestamp;

        public String img_sha1() {
            return img_sha1;
        }

        public String img_timestamp() {
            return img_timestamp;
        }
    }
}
