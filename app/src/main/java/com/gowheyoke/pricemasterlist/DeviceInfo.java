package com.gowheyoke.pricemasterlist;

/**
 * Created by Daryll Bangoy on 7/3/2016.
 */
public class DeviceInfo {
    private String device_id;
    private String balance;

    public DeviceInfo() {
      /*Blank default constructor essential for Firebase*/
    }

    public DeviceInfo(String device_id, String balance) {
        this.device_id = device_id;
        this.balance = balance;
    }

    public String getDeviceId() {
        return PriceListDBAdapter.KEY_DEVICE_ID;
    }

    public void setDeviceId(String device_id) {
        this.device_id = device_id;
    }

    public String getBalance() {
        return PriceListDBAdapter.KEY_BALANCE;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }
}
