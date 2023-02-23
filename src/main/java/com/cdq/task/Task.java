package com.cdq.task;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document
@ToString
class Task {
    @Id
    private String id;
    private String input;
    private String pattern;
    private Integer position;
    private Integer typos;
    private TaskStatus status;
    private double progress;
}