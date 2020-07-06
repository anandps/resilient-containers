package com.github.anandps.resilient.app;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("example")
public class ExampleEntity {
    @Id
    private int id;
    private String name;
}

