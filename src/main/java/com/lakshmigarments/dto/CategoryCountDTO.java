package com.lakshmigarments.dto;

import java.sql.Date;
import java.util.List;

import com.lakshmigarments.model.Category;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CategoryCountDTO {
    private Category category;
    private List<SubCategoryCountDTO> subCategoryCountDTOs;
}
