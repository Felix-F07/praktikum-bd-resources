package com.example.bdsqltester.dtos;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Grade {
    public long userId;
    public long assignmentId;
    public double grade;

    public Grade(long userId, long assignmentId, double grade) {
        this.userId = userId;
        this.assignmentId = assignmentId;
        this.grade = grade;
    }

    public Grade(ResultSet rs) throws SQLException {
        this.userId = rs.getLong("user_id");
        this.assignmentId = rs.getLong("assignment_id");
        this.grade = rs.getDouble("grade");
    }
}
