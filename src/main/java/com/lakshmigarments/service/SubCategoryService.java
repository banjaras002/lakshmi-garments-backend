package com.lakshmigarments.service;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.lakshmigarments.dto.CreateSubCategoryDTO;
import com.lakshmigarments.model.SubCategory;
import com.lakshmigarments.repository.SubCategoryRepository;

@Service
public class SubCategoryService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SubCategoryService.class);
	private final SubCategoryRepository subCategoryRepository;
	private final ModelMapper modelMapper;
	
	public SubCategoryService(SubCategoryRepository subCategoryRepository, ModelMapper modelMapper) {
		this.subCategoryRepository = subCategoryRepository;
		this.modelMapper = modelMapper;
	}
	
	public SubCategory createSubCategory(CreateSubCategoryDTO createSubCategoryDTO) {
		SubCategory subCategory = modelMapper.map(createSubCategoryDTO, SubCategory.class);
		SubCategory createdSubCategory = subCategoryRepository.save(subCategory);
		LOGGER.info("Created sub category with name {}", createdSubCategory.getName());
		return createdSubCategory;
	}
	
	public Page<SubCategory> getSubCategories(Integer pageNo, Integer pageSize, String sortBy, String sortDir) {
		
		if (pageSize == null) {
			LOGGER.info("Retrieved all sub categories");
			Pageable wholePage = Pageable.unpaged();
			return subCategoryRepository.findAll(wholePage);
		}
		
		Sort sort  = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
						? Sort.by(sortBy).ascending()
						: Sort.by(sortBy).descending();
		Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
		Page<SubCategory> subCategoryPage = subCategoryRepository.findAll(pageable);
		
		LOGGER.info("Retrieved sub categories as pages");
		return subCategoryPage;
	}
}
