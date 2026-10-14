package com.ag.deprem.model.dto;

public class KoeriRawEventDto {
    private String date;
    private String time;
    private String latitude;
    private String longitude;
    private String depth;
    private String md;
    private String ml;
    private String mw;
    private String location;
    private String solutionQuality;
    private String rawLine;

    public KoeriRawEventDto() {
    }

    public KoeriRawEventDto(String date, String time, String latitude, String longitude, String depth,
                            String md, String ml, String mw, String location, String solutionQuality, String rawLine) {
        this.date = date;
        this.time = time;
        this.latitude = latitude;
        this.longitude = longitude;
        this.depth = depth;
        this.md = md;
        this.ml = ml;
        this.mw = mw;
        this.location = location;
        this.solutionQuality = solutionQuality;
        this.rawLine = rawLine;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getLatitude() { return latitude; }
    public void setLatitude(String latitude) { this.latitude = latitude; }

    public String getLongitude() { return longitude; }
    public void setLongitude(String longitude) { this.longitude = longitude; }

    public String getDepth() { return depth; }
    public void setDepth(String depth) { this.depth = depth; }

    public String getMd() { return md; }
    public void setMd(String md) { this.md = md; }

    public String getMl() { return ml; }
    public void setMl(String ml) { this.ml = ml; }

    public String getMw() { return mw; }
    public void setMw(String mw) { this.mw = mw; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getSolutionQuality() { return solutionQuality; }
    public void setSolutionQuality(String solutionQuality) { this.solutionQuality = solutionQuality; }

    public String getRawLine() { return rawLine; }
    public void setRawLine(String rawLine) { this.rawLine = rawLine; }
}
