package com.lakshmigarments.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lakshmigarments.dto.EmployeeRequestDTO;
import com.lakshmigarments.dto.JobworkRequestDTO;
import com.lakshmigarments.dto.JobworkResponseDTO;
import com.lakshmigarments.dto.request.CreateJobworkRequest;
import com.lakshmigarments.dto.request.ReassignJobworkRequest;
import com.lakshmigarments.dto.response.JobworkDetailDTO;
import com.lakshmigarments.dto.response.JobworkResponse;
import com.lakshmigarments.model.Employee;
import com.lakshmigarments.model.Jobwork;
import com.lakshmigarments.service.JobworkService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/jobworks")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class JobworkController {

    private final Logger LOGGER = LoggerFactory.getLogger(JobworkController.class);
    private final JobworkService jobworkService;

    @GetMapping
    public ResponseEntity<Page<JobworkResponseDTO>> getAllJobworks(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "7") int size, @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String order, @RequestParam(required = false) String search) {
        return new ResponseEntity<>(
                jobworkService.getAllJobworks(PageRequest.of(page, size, Sort.by(sortBy).descending()), search),
                HttpStatus.OK);
    }

    // create a new jobwork
    @PostMapping
    public ResponseEntity<JobworkResponse> createJobwork(@RequestBody CreateJobworkRequest jobworkRequest) {
        LOGGER.info("Received jobwork request for batch: {} and employee: {}", jobworkRequest.getBatchSerialCode(),
        		jobworkRequest.getAssignedTo());
        JobworkResponse createdJobwork = jobworkService.createJobwork(jobworkRequest);
        LOGGER.info("Jobwork created successfully for batch: {} and employee: {}", jobworkRequest.getBatchSerialCode(),
        		jobworkRequest.getAssignedTo());
        return new ResponseEntity<>(createdJobwork, HttpStatus.CREATED);
    }

    @GetMapping("/jobwork-numbers")
    public ResponseEntity<List<String>> getJobworkNumbers(@RequestParam(required = false) String search) {
        return new ResponseEntity<>(jobworkService.getJobworkNumbers(search), HttpStatus.OK);
    }

    // GETTNG a quick detail of the jobwork for viewing during in pass acceptance
    @GetMapping("/jobwork-numbers/{jobworkNumber}")
    public ResponseEntity<JobworkDetailDTO> getJobworkDetail(@PathVariable String jobworkNumber) {
    	LOGGER.info("Received request to get jobwork details of {}", jobworkNumber);
        return new ResponseEntity<>(jobworkService.getJobworkDetail(jobworkNumber), HttpStatus.OK);
    }

    @GetMapping("/jobwork-numbers/next-number")
    public ResponseEntity<String> getNextJobworkNumber() {
        return new ResponseEntity<>(jobworkService.getNextJobworkNumber(), HttpStatus.OK);
    }
    
    @PostMapping("/{jobworkNumber}/reassign")
    public ResponseEntity<Jobwork> reAssignJobwork(@PathVariable String jobworkNumber, 
    		@RequestBody ReassignJobworkRequest reassignRequest) {
    	LOGGER.info("Received request to reassign jobwork {} to employee {}", jobworkNumber, reassignRequest.getEmployeeName());
        return new ResponseEntity<>(jobworkService.reAssignJobwork(jobworkNumber, 
        		reassignRequest.getEmployeeName()), HttpStatus.OK);
    }
    
    @PostMapping("/{jobworkNumber}/close")
    public ResponseEntity<JobworkResponse> closeJobwork(@PathVariable String jobworkNumber) {
    	LOGGER.info("Received request to close jobwork {}", jobworkNumber);
    	JobworkResponse jobworkResponse = jobworkService.closeJobwork(jobworkNumber);
    	LOGGER.info("Closed jobwork {}", jobworkNumber);
    	return new ResponseEntity<>(jobworkResponse, HttpStatus.OK);
    }
    
    @PostMapping("/{jobworkNumber}/reopen")
    public ResponseEntity<JobworkResponse> reopenJobwork(@PathVariable String jobworkNumber) {
    	LOGGER.info("Received request to reopen jobwork {}", jobworkNumber);
    	JobworkResponse jobworkResponse = jobworkService.reopenJobwork(jobworkNumber);
    	LOGGER.info("Reopened jobwork {}", jobworkNumber);
    	return new ResponseEntity<>(jobworkResponse, HttpStatus.OK);
    }
    
//    @GetMapping("/unfinished")
//    public ResponseEntity<List<String>> getUnfinishedJobworks(@RequestParam String employeeName, 
//    		@RequestParam String jobworkNumber) {
//        return new ResponseEntity<>(jobworkService.getUnfinishedJobworks(jobworkNumber,
//        		 employeeName), HttpStatus.OK);
//    }

}
