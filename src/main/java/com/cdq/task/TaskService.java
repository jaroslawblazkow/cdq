package com.cdq.task;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.stream.IntStream;

@Service
@AllArgsConstructor
@Slf4j
class TaskService {
    private final TaskRepository taskRepository;
    private final CacheManager cacheManager;

    Mono<Task> createTask(String pattern, String input) {
        Task task = new Task();
        task.setPattern(pattern);
        task.setInput(input);
        task.setStatus(TaskStatus.CREATED);
        return taskRepository.save(task).doOnNext(savedTask -> {
            log.info("Saved task: {}", savedTask);
            invalidateCache();
        });
    }

    Mono<Task> processTask(Task task) {
        return Mono.fromCallable(() -> calculateTask(task))
                .doOnSuccess(s -> {
                    task.setStatus(TaskStatus.COMPLETED);
                    task.setProgress(100d);
                    taskRepository.save(task).subscribe();
                })
                .onErrorResume(e -> {
                    task.setProgress(0d);
                    task.setStatus(TaskStatus.FAILED);
                    return taskRepository.save(task).then(Mono.error(e));
                })
                .doFinally(it -> invalidateCache());
    }

    Flux<Task> listTasks() {
        return taskRepository.findAll();
    }

    Mono<Task> getTaskById(String id) {
        return taskRepository.findById(id);
    }

    private Task calculateTask(Task task) throws InterruptedException {
        int inputLength = task.getInput().length();
        int patternLength = task.getPattern().length();
        int totalPositions = inputLength - patternLength + 1;
        for (int i = 0; i < totalPositions; i++) {
            // for a sake of processing postponing
            Thread.sleep(3000);
            int position = i;
            int typos = (int) IntStream.range(0, patternLength)
                    .filter(j -> task.getPattern().charAt(j) != task.getInput().charAt(position + j))
                    .count();
            task.setPosition(position);
            task.setTypos(typos);
            if (typos == 0) {
                return task;
            } else {
                task.setProgress((double) (position + 1) / inputLength * 100);
                task.setStatus(TaskStatus.IN_PROGRESS);
                taskRepository.save(task).subscribe();
            }
        }
        return task;
    }
    private void invalidateCache() {
        Optional.ofNullable(cacheManager.getCache("tasks")).map(Cache::invalidate);
    }
}