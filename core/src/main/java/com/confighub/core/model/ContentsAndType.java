package com.confighub.core.model;

public class ContentsAndType
{
    private final String contents;
    private final String type;

    public ContentsAndType(String contents, String type)
    {
        this.contents = contents;
        this.type = type;
    }

    public String getContents()
    {
        return contents;
    }

    public String getType()
    {
        return type;
    }
}
