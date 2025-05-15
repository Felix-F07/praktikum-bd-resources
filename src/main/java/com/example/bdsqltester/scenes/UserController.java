package com.example.bdsqltester.scenes;

import com.example.bdsqltester.datasources.MainDataSource;
import com.example.bdsqltester.dtos.Assignment;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class UserController {
    @FXML
    private ListView<Assignment> assignmentList;
    @FXML private TextField idField;
    @FXML private TextField nameField;
    @FXML private TextArea instructionsField;
    @FXML private TextArea answerKeyField;
    @FXML private TextArea userQueryArea;
    @FXML private Label gradeLabel;

    Connection connection = MainDataSource.getConnection();
    private int userId;

    public UserController() throws SQLException {
    }


    public void setUserId(int id) {
        this.userId = id;
    }

    @FXML
    public void initialize() {
        if (connection == null) {
            showAlert("Error", "Database connection is not established.");
            return;
        }
        loadAssignments();
        assignmentList.setOnMouseClicked(this::onAssignmentSelected);

    }

    private void loadAssignments() {
        ObservableList<Assignment> assignments = FXCollections.observableArrayList();
        try {
            if (connection == null) {
                showAlert("Error", "Database connection is not available.");
                return;
            }

            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM assignments ORDER BY id");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                assignments.add(new Assignment(rs));
            }

            assignmentList.setItems(assignments);

        } catch (SQLException e) {
            showAlert("Error", "Failed to load assignments: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private void onAssignmentSelected(MouseEvent event) {
        Assignment selected = assignmentList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        idField.setText(String.valueOf(selected.id));
        nameField.setText(selected.name);
        instructionsField.setText(selected.instructions);
        answerKeyField.setText(selected.answerKey);
        loadUserGrade((int) selected.id);
    }


    private void loadUserGrade(int assignmentId) {
        try {
            if (connection == null) {
                showAlert("Error", "Database connection is not available.");
                return;
            }
            PreparedStatement stmt = connection.prepareStatement("SELECT grade FROM grades WHERE assignment_id = ? AND user_id = ?");
            stmt.setInt(1, assignmentId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                gradeLabel.setText("Score: " + rs.getInt("grade"));
            } else {
                gradeLabel.setText("Score: -");
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load grade: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    void onTestButtonClick() {
        String query = userQueryArea.getText();
        try {
            if (connection == null) {
                showAlert("Error", "Database connection is not available.");
                return;
            }
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            String resultString = resultSetToString(rs);
            showAlert("Query Output", resultString);
        } catch (SQLException e) {
            showAlert("Query Error", e.getMessage());
        }
    }

    @FXML
    void onSubmitClick() {
        if (idField.getText().isEmpty()) {
            showAlert("Error", "Please select an assignment first.");
            return;
        }

        int assignmentId = Integer.parseInt(idField.getText());
        String userQuery = userQueryArea.getText();
        String answerQuery = answerKeyField.getText();

        try {
            if (connection == null) {
                showAlert("Error", "Database connection is not available.");
                return;
            }
            String userResult = resultSetToString(connection.createStatement().executeQuery(userQuery));
            String answerResult = resultSetToString(connection.createStatement().executeQuery(answerQuery));

            int grade;
            if (userResult.equals(answerResult)) {
                grade = 100;
            } else if (sortLines(userResult).equals(sortLines(answerResult))) {
                grade = 50;
            } else {
                grade = 0;
            }

            PreparedStatement checkStmt = connection.prepareStatement(
                    "SELECT grade FROM grades WHERE assignment_id = ? AND user_id = ?"
            );
            checkStmt.setInt(1, assignmentId);
            checkStmt.setInt(2, userId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                int prevGrade = rs.getInt("grade");
                if (grade > prevGrade) {
                    PreparedStatement updateStmt = connection.prepareStatement(
                            "UPDATE grades SET grade = ? WHERE assignment_id = ? AND user_id = ?"
                    );
                    updateStmt.setInt(1, grade);
                    updateStmt.setInt(2, assignmentId);
                    updateStmt.setInt(3, userId);
                    updateStmt.executeUpdate();
                }
            } else {
                PreparedStatement insertStmt = connection.prepareStatement(
                        "INSERT INTO grades (assignment_id, user_id, grade) VALUES (?, ?, ?)"
                );
                insertStmt.setInt(1, assignmentId);
                insertStmt.setInt(2, userId);
                insertStmt.setInt(3, grade);
                insertStmt.executeUpdate();
            }

            gradeLabel.setText("Score: " + grade);
            showAlert("Submission Result", "You received a score of: " + grade);

        } catch (SQLException e) {
            showAlert("Error", e.getMessage());
        }
    }

    private String resultSetToString(ResultSet rs) throws SQLException {
        StringBuilder sb = new StringBuilder();
        ResultSetMetaData md = rs.getMetaData();
        int colCount = md.getColumnCount();
        while (rs.next()) {
            for (int i = 1; i <= colCount; i++) {
                sb.append(rs.getString(i)).append("\t");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String sortLines(String input) {
        List<String> lines = new ArrayList<>(List.of(input.split("\n")));
        lines.sort(String::compareTo);
        return String.join("\n", lines);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
