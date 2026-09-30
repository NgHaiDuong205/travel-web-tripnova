package com.duong.travelweb.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Thông tin xuất hoá đơn do khách tự nhập (tên cá nhân / công ty, địa chỉ, mã số thuế). */
public class InvoiceBillingRequestDTO {
    @NotBlank(message = "Vui lòng nhập tên trên hoá đơn")
    @Size(max = 150, message = "Tên trên hoá đơn tối đa 150 ký tự")
    private String billingName;

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String billingAddress;

    // Để trống = không có MST; có thì chỉ gồm chữ, số, dấu gạch ngang (VD MST chi nhánh 0101234567-001)
    @Pattern(regexp = "^$|^[A-Za-z0-9][A-Za-z0-9-]{4,29}$", message = "Mã số thuế không hợp lệ")
    private String billingTaxCode;

    public String getBillingName() {
        return billingName;
    }

    public void setBillingName(String billingName) {
        this.billingName = billingName;
    }

    public String getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(String billingAddress) {
        this.billingAddress = billingAddress;
    }

    public String getBillingTaxCode() {
        return billingTaxCode;
    }

    public void setBillingTaxCode(String billingTaxCode) {
        this.billingTaxCode = billingTaxCode;
    }
}
