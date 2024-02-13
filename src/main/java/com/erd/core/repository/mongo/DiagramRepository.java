package com.erd.core.repository.mongo;

import com.erd.core.model.Diagram;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DiagramRepository extends MongoRepository<Diagram, String> { }
