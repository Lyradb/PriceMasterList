package com.gowheyoke.pricemasterlist;

/**
 * Created by auberon on 6/26/2016.
 */
public class Product {
    private String barcode;
    private String make;
    private String itemdesc;
    private String unit;
    private String qty;
    private String wsp;
    private String rsp;

    public Product() {
      /*Blank default constructor essential for Firebase*/
    }
    //Getters and setters

    public String getBarcode() {
        return PriceListDBAdapter.KEY_BARCODE;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getMake() {
        return PriceListDBAdapter.KEY_MAKE;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getItemDesc() {
        return PriceListDBAdapter.KEY_ITEMDESC;
    }

    public void setItemDesc(String itemdesc) {
        this.itemdesc = itemdesc;
    }

    public String getUnit() {
        return PriceListDBAdapter.KEY_UNIT;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getQty() {
        return PriceListDBAdapter.KEY_QTY;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }

    public String getWsp() {
        return PriceListDBAdapter.KEY_WSP;
    }

    public void setWsp(String wsp) {
        this.wsp = wsp;
    }

    public String getRsp() {
        return PriceListDBAdapter.KEY_RSP;
    }

    public void setRsp(String rsp) {
        this.rsp = rsp;
    }
}
