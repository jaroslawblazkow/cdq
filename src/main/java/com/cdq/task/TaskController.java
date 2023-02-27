package com.cdq.task;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;

@RestController
@RequestMapping("/api/tasks")
@AllArgsConstructor
@Slf4j
class TaskController {
    private final TaskService taskService;

    @PostMapping
    public Mono<ResponseEntity<Task>> createTask(@RequestParam String pattern, @RequestParam String input) {
        return taskService.createTask(pattern, input)
                .doOnSuccess(taskService::processTask)
                .map(it -> ResponseEntity.created(URI.create("/api/tasks/" + it.getId())).body(it));
    }

    @GetMapping
    @Cacheable("tasks")
    public Flux<Task> listTasks() {
        return taskService.listTasks();
    }

    @GetMapping("/{id}")
    @Cacheable(value = "tasks", key = "#id")
    public Mono<Task> getTaskById(@PathVariable String id) {
        return taskService.getTaskById(id);
    }
}