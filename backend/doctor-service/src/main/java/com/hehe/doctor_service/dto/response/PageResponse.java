package com.hehe.doctor_service.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Bao ket qua phan trang: content = du lieu trang hien tai + metadata de FE render. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse<T> {
    List<T> content;
    int page;
    int size;
    int totalPages;
    long totalElements;

    /** Cat 1 trang tu danh sach day du (da cache/loc san). */
    public static <T> PageResponse<T> of(List<T> all, int page, int size) {
        int total = all.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        int from = Math.max(0, Math.min(page * size, total));
        int to = Math.min(from + size, total);
        return new PageResponse<>(all.subList(from, to), page, size, totalPages, total);
    }
}
