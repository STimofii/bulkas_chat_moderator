package com.bulka.net.tg.bots.ldrs.chats;

import java.io.Serializable;

public class Trigger implements Serializable {
    private String text = "";
    boolean strict = false;
    boolean lethal = false;
    boolean advancedCheck = true;

    public Trigger(String text) {
        this.text = text;
    }

    public Trigger(String text, boolean strict, boolean lethal, boolean advancedCheck) {
        this.text = text;
        this.strict = strict;
        this.lethal = lethal;
        this.advancedCheck = advancedCheck;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text.toLowerCase();
    }

    public boolean isLethal() {
        return lethal;
    }

    public void setLethal(boolean lethal) {
        this.lethal = lethal;
    }

    public boolean isAdvancedCheck() {
        return advancedCheck;
    }

    public void setAdvancedCheck(boolean advancedCheck) {
        this.advancedCheck = advancedCheck;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }
}
