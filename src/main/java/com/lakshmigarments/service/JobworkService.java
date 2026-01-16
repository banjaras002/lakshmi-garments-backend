package com.lakshmigarments.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.lakshmigarments.dto.JobworkRequestDTO;
import com.lakshmigarments.dto.JobworkResponseDTO;
import com.lakshmigarments.dto.request.CreateJobworkRequest;
import com.lakshmigarments.dto.response.JobworkDetailDTO;
import com.lakshmigarments.dto.response.JobworkResponse;
import com.lakshmigarments.model.Jobwork;

@Service
public interface JobworkService<T extends CreateJobworkRequest> {

    List<String> getJobworkNumbers(String search);

    JobworkDetailDTO getJobworkDetail(String jobworkNumber);

    String getNextJobworkNumber();

//    Jobwork createJobwork(JobworkRequestDTO jobworkRequestDTO);

    Page<JobworkResponseDTO> getAllJobworks(Pageable pageable, String search);
    
    Jobwork reAssignJobwork(String jobworkNumber, String employeeName);
    
    JobworkResponse createJobwork(T request);
    
    JobworkResponse closeJobwork(String jobworkNumber);
    
    JobworkResponse reopenJobwork(String jobworkNumber);
    
//    List<String> getUnfinishedJobworks(String employeeName, String jobworkNumber);

}
