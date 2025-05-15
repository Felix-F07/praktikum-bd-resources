package com.example.bdsqltester.dtos;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Assignment {
    public long id;
    public String name;
    public String instructions;
    public String answerKey;

    public Assignment(long id, String name, String instructions, String answerKey) {
        this.id = id;
        this.name = name;
        this.instructions = instructions;
        this.answerKey = answerKey;
    }

    public Assignment(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.name = rs.getString("name");
        this.instructions = rs.getString("instructions");
        this.answerKey = rs.getString("answer_key");
    }

    @Override
    public String toString() {
        return name;
    }
}
