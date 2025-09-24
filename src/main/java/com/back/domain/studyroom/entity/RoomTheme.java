package com.back.domain.studyroom.entity;

import com.back.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class RoomTheme extends BaseEntity {
    @Enumerated(EnumType.STRING)
    private RoomType type;
    private String name;
    private String imageUrl;

    @OneToMany(mappedBy = "theme", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Room> rooms = new ArrayList<>();
}
