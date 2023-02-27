package com.cdq.task;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
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
        return taskRepository.save(task)
                .doOnNext(savedTask -> {
                    log.info("Saved task: {}", savedTask);
                    invalidateCache();
                });
    }

    void processTask(Task task) {
        calculateTask(task)
                .flatMap(s -> {
                    task.setStatus(TaskStatus.COMPLETED);
                    task.setProgress(100d);
                    return taskRepository.save(task);
                })
                .onErrorResume(e -> {
                    task.setProgress(0d);
                    task.setStatus(TaskStatus.FAILED);
                    return taskRepository.save(task)
                            .then(Mono.error(e));
                })
                .doFinally(it -> invalidateCache())
                .subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    Flux<Task> listTasks() {
        return taskRepository.findAll();
    }

    Mono<Task> getTaskById(String id) {
        return taskRepository.findById(id);
    }

    private Mono<Task> calculateTask(Task task) {
        int inputLength = task.getInput().length();
        int patternLength = task.getPattern().length();
        int totalPositions = inputLength - patternLength + 1;

        return Flux.range(0, totalPositions)
                .delayElements(Duration.ofSeconds(3))
                .flatMap(position -> {
                    int typos = (int) IntStream.range(0, patternLength)
                            .filter(j -> task.getPattern().charAt(j) != task.getInput().charAt(position + j))
                            .count();

                    task.setPosition(position);
                    task.setTypos(typos);

                    if (typos == 0) {
                        return Mono.just(task);
                    } else {
                        task.setProgress((double) (position + 1) / inputLength * 100);
                        task.setStatus(TaskStatus.IN_PROGRESS);
                        return taskRepository.save(task);
                    }
                })
                .doOnComplete(() -> log.info("End of task processing"))
                .last();
    }


    private void invalidateCache() {
        Optional.ofNullable(cacheManager.getCache("tasks"))
                .ifPresent(Cache::invalidate);
    }
}