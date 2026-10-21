package com.ag.deprem.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class FeltReportRequestDto {

    @Pattern(regexp = "^(?i)(NOT_FELT|WEAK|MODERATE|STRONG|VIOLENT)$", message = "Geçersiz hissedilme seviyesi")
    private String feltLevel;       // NOT_FELT, WEAK, MODERATE, STRONG, VIOLENT

    @Size(max = 30, message = "Hareket türü en fazla 30 karakter olabilir")
    private String motionType;      // SLOW_SWAYING, RAPID_SHAKING, JOLTING, VIBRATION

    @Size(max = 30, message = "Tepki en fazla 30 karakter olabilir")
    private String reaction;        // NO_REACTION, SURPRISED, FRIGHTENED, RAN_OUTSIDE

    private Boolean soundHeard;     // true / false

    @Size(max = 30, message = "Gözlenen hasar en fazla 30 karakter olabilir")
    private String damageObserved;  // NONE, HAIRLINE_CRACKS, OBJECTS_TOPPLED, STRUCTURAL

    @Min(value = 0, message = "Bina katı 0 veya daha büyük olmalıdır")
    @Max(value = 150, message = "Bina katı 150'den büyük olamaz")
    private Integer buildingFloor;  // 0, 1, 2...

    @DecimalMin(value = "-90.0", message = "Enlem -90 ile 90 arasında olmalıdır")
    @DecimalMax(value = "90.0", message = "Enlem -90 ile 90 arasında olmalıdır")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Boylam -180 ile 180 arasında olmalıdır")
    @DecimalMax(value = "180.0", message = "Boylam -180 ile 180 arasında olmalıdır")
    private Double longitude;

    @Size(max = 64, message = "İl adı en fazla 64 karakter olabilir")
    private String province;

    @Size(max = 64, message = "İlçe adı en fazla 64 karakter olabilir")
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
