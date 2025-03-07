package com.know.aikg.service;

public enum RoleKeywords {
    LEARN_AI("大语言模型专家", "大语言模型前沿进展", "程序员", "xinlongzhan@webank.com"),
    LEARN_BOBY("资深母婴专家", "母婴知识", "孕妇", "xinlongzhan@webank.com"),
    ;
    
    private final String role;

    private final String area;

    private final String reader;    
    
    private final String readerEmail;

    RoleKeywords(String role, String area, String reader, String readerEmail) {
        this.role = role;
        this.area = area;
        this.reader = reader;
        this.readerEmail = readerEmail;
    }
    
    public String getRole() {
            return role;
    }

    public String getArea() {
        return area;
    }

    public String getReader() {
        return reader;
    }

    public String getReaderEmail() {
        return readerEmail;
    }
}
