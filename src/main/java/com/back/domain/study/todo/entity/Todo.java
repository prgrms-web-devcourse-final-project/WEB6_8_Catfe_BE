package com.back.domain.study.todo.entity;

import com.back.domain.study.record.entity.StudyRecord;
import com.back.domain.user.entity.User;
import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Todo extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private boolean isComplete;

    private String description;

    private LocalDate date;

    public Todo(User user, String description, LocalDate date) {
        this.user = user;
        this.description = description;
        this.date = date;
        this.isComplete = false;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void toggleComplete() {
        this.isComplete = !this.isComplete;
    }
}
