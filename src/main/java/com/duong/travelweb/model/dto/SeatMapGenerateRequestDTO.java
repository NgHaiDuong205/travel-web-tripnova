package com.duong.travelweb.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Sinh sơ đồ ghế theo các khối (thay toàn bộ ghế chưa có người đặt). */
public class SeatMapGenerateRequestDTO {
    @NotEmpty(message = "Cần ít nhất một khối ghế")
    @Size(max = 10, message = "Tối đa 10 khối ghế")
    private List<@Valid SeatBlockRequestDTO> blocks;

    public List<@Valid SeatBlockRequestDTO> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<@Valid SeatBlockRequestDTO> blocks) {
        this.blocks = blocks;
    }
}

