package com.erd.core.repository.mongo;

import com.erd.core.model.mongo.Diagram;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DiagramRepository extends MongoRepository<Diagram, String> {

    Optional<Diagram> findByProjectId(String projectId);
    
    void deleteByProjectId(String projectId);

}
