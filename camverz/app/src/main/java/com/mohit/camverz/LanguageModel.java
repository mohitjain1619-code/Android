package com.mohit.camverz;

public class LanguageModel {
    private final String code;
    private final String nameNative;
    private final String nameEnglish;
    private final String flagEmoji;

    public LanguageModel(String code, String nameNative, String nameEnglish, String flagEmoji) {
        this.code = code;
        this.nameNative = nameNative;
        this.nameEnglish = nameEnglish;
        this.flagEmoji = flagEmoji;
    }

    public String getCode() {
        return code;
    }

    public String getNameNative() {
        return nameNative;
    }

    public String getNameEnglish() {
        return nameEnglish;
    }

    public String getFlagEmoji() {
        return flagEmoji;
    }
}
