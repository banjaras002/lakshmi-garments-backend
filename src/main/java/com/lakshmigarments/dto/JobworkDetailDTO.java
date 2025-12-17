package com.lakshmigarments.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.lakshmigarments.model.JobworkType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobworkDetailDTO {

    private String jobworkNumber;

    private LocalDateTime startedAt;

    private JobworkType jobworkType;

    private String batchSerialCode;

    private String assignedTo;

    private List<String> items;

    private List<Long> quantity;

}
