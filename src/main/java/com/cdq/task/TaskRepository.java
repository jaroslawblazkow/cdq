package com.cdq.task;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

interface TaskRepository extends ReactiveMongoRepository<Task, String> {
}