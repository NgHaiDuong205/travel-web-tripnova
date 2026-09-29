package com.duong.travelweb.repository.custom;

import com.duong.travelweb.model.entity.InvoiceEntity;

import java.time.LocalDate;
import java.util.List;

public interface InvoiceRepositoryCustom {
    /**
     * Danh sách cho admin, mới nhất trước, kèm sẵn order / user / payment.
     * @param keyword số hoá đơn, mã order, email hoặc tên khách (null = bỏ qua)
     * @param from    ngày phát hành từ (tính cả ngày này, null = bỏ qua)
     * @param to      ngày phát hành đến (tính cả ngày này, null = bỏ qua)
     */
    List<InvoiceEntity> findForAdmin(String keyword, LocalDate from, LocalDate to, int page, int limit);

    long countForAdmin(String keyword, LocalDate from, LocalDate to);
}
