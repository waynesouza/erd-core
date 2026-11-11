package com.erd.core.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * What an import produced: how many tables and relationships the diagram now has, and a warning
 * for each part of the script that could not be represented.
 */
public class ImportDdlResponseDTO implements Serializable {

    @JsonProperty
    private int tables;

    @JsonProperty
    private int relationships;

    @JsonProperty
    private List<String> warnings;

    public ImportDdlResponseDTO() { }

    public ImportDdlResponseDTO(int tables, int relationships, List<String> warnings) {
        this.tables = tables;
        this.relationships = relationships;
        this.warnings = warnings;
    }

    public int getTables() {
        return tables;
    }

    public void setTables(int tables) {
        this.tables = tables;
    }

    public int getRelationships() {
        return relationships;
    }

    public void setRelationships(int relationships) {
        this.relationships = relationships;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
