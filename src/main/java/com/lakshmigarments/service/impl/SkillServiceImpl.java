package com.lakshmigarments.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.lakshmigarments.dto.SkillRequestDTO;
import com.lakshmigarments.dto.SkillResponseDTO;
import com.lakshmigarments.exception.DuplicateSkillException;
import com.lakshmigarments.model.Skill;
import com.lakshmigarments.repository.SkillRepository;
import com.lakshmigarments.repository.specification.SkillSpecification;
import com.lakshmigarments.service.SkillService;

@Service
public class SkillServiceImpl implements SkillService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SkillServiceImpl.class);

	private final SkillRepository skillRepository;
	
	private final ModelMapper modelMapper;
	
	public SkillServiceImpl(SkillRepository skillRepository, ModelMapper modelMapper) {
		this.skillRepository = skillRepository;
		this.modelMapper = modelMapper;
	}

	@Override
	public SkillResponseDTO createSkill(SkillRequestDTO skillRequestDTO) {
		
		String skillName = skillRequestDTO.getName().trim();
		
		if (skillRepository.existsByName(skillName)) {
			LOGGER.error("Skill already exists with name {}", skillName);
			throw new DuplicateSkillException("Skill already exists with name " + skillName);
		}
		
		Skill skill = new Skill();
		skill.setName(skillName);
		
		Skill savedSkill = skillRepository.save(skill);
		LOGGER.debug("Skill created with name {}", savedSkill.getName());
		
		return modelMapper.map(savedSkill, SkillResponseDTO.class);
	}

	@Override
	public List<SkillResponseDTO> getAllSkills(String search) {
		
		Specification<Skill> spec = SkillSpecification.filterByName(search);
		
		List<Skill> skills = skillRepository.findAll(spec);
		
		LOGGER.debug("Found {} skill(s) matching filter", skills.size());
		
		return skills.stream()
                .map(skill -> modelMapper.map(skill, SkillResponseDTO.class))
                .collect(Collectors.toList());
		
	}

}
