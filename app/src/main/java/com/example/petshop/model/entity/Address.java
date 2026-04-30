package com.example.petshop.model.entity;

import com.google.gson.annotations.SerializedName;

public class Address {

    @SerializedName("id")
    private String id;

    @SerializedName("userId")
    private String userId;

    @SerializedName("label")
    private String label;                // Nhà, Công ty, ...

    @SerializedName("receiverName")
    private String receiverName;

    @SerializedName("receiverPhone")
    private String receiverPhone;

    @SerializedName("addressLine")
    private String addressLine;          // số nhà, tên đường

    @SerializedName("ward")
    private String ward;                 // phường/xã

    @SerializedName("wardCode")
    private String wardCode;

    @SerializedName("district")
    private String district;             // quận/huyện

    @SerializedName("districtCode")
    private String districtCode;

    @SerializedName("city")
    private String city;                 // tỉnh/thành phố

    @SerializedName("cityCode")
    private String cityCode;

    @SerializedName("isDefault")
    private boolean isDefault;

    @SerializedName("createdAt")
    private String createdAt;

    public Address() {}

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (addressLine != null && !addressLine.isEmpty()) sb.append(addressLine).append(", ");
        if (ward != null && !ward.isEmpty())               sb.append(ward).append(", ");
        if (district != null && !district.isEmpty())       sb.append(district).append(", ");
        if (city != null && !city.isEmpty())               sb.append(city);
        return sb.toString();
    }

    // region Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }

    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getWardCode() { return wardCode; }
    public void setWardCode(String wardCode) { this.wardCode = wardCode; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getDistrictCode() { return districtCode; }
    public void setDistrictCode(String districtCode) { this.districtCode = districtCode; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCityCode() { return cityCode; }
    public void setCityCode(String cityCode) { this.cityCode = cityCode; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    // endregion
}
