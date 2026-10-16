package com.ag.deprem.model.dto;

public class FeltReportRequestDto {
    private String feltLevel;       // NOT_FELT, WEAK, MODERATE, STRONG, VIOLENT
    private String motionType;      // SLOW_SWAYING, RAPID_SHAKING, JOLTING, VIBRATION
    private String reaction;        // NO_REACTION, SURPRISED, FRIGHTENED, RAN_OUTSIDE
    private Boolean soundHeard;     // true / false
    private String damageObserved;  // NONE, HAIRLINE_CRACKS, OBJECTS_TOPPLED, STRUCTURAL
    private Integer buildingFloor;  // 0, 1, 2...
    private Double latitude;
    private Double longitude;
    private String province;
    private String district;

    public FeltReportRequestDto() {
    }

    public String getFeltLevel() { return feltLevel; }
    public void setFeltLevel(String feltLevel) { this.feltLevel = feltLevel; }

    public String getMotionType() { return motionType; }
    public void setMotionType(String motionType) { this.motionType = motionType; }

    public String getReaction() { return reaction; }
    public void setReaction(String reaction) { this.reaction = reaction; }

    public Boolean getSoundHeard() { return soundHeard; }
    public void setSoundHeard(Boolean soundHeard) { this.soundHeard = soundHeard; }

    public String getDamageObserved() { return damageObserved; }
    public void setDamageObserved(String damageObserved) { this.damageObserved = damageObserved; }

    public Integer getBuildingFloor() { return buildingFloor; }
    public void setBuildingFloor(Integer buildingFloor) { this.buildingFloor = buildingFloor; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
}
