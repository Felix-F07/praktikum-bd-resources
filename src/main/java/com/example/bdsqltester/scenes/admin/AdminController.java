package com.example.bdsqltester.scenes.admin;

import com.example.bdsqltester.datasources.GradingDataSource;
import com.example.bdsqltester.datasources.MainDataSource;
import com.example.bdsqltester.dtos.Assignment;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

public class AdminController {

    @FXML
    private TextArea answerKeyField;

    @FXML
    private ListView<Assignment> assignmentList = new ListView<>();

    @FXML
    private TextField idField;

    @FXML
    private TextArea instructionsField;

    @FXML
    private TextField nameField;

    private final ObservableList<Assignment> assignments = FXCollections.observableArrayList();

    @FXML
    void initialize() {
        idField.setEditable(false);
        idField.setMouseTransparent(true);
        idField.setFocusTraversable(false);

        refreshAssignmentList();

        assignmentList.setCellFactory(param -> new ListCell<Assignment>() {
            @Override
            protected void updateItem(Assignment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.name);
                }
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                if (selected) {
                    onAssignmentSelected(getItem());
                }
            }
        });
    }

    void refreshAssignmentList() {
        assignments.clear();

        try (Connection c = MainDataSource.getConnection()) {
            Statement stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM assignments");

            while (rs.next()) {
                assignments.addAll(new Assignment(rs));
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Database Error");
            alert.setContentText(e.toString());
        }

        assignmentList.setItems(assignments);

        try {
            if (!idField.getText().isEmpty()) {
                long id = Long.parseLong(idField.getText());
                for (Assignment assignment : assignments) {
                    if (assignment.id == id) {
                        assignmentList.getSelectionModel().select(assignment);
                        break;
                    }
                }
            }
        } catch (NumberFormatException e) {
        }
    }

    void onAssignmentSelected(Assignment assignment) {
        idField.setText(String.valueOf(assignment.id));

        nameField.setText(assignment.name);

        instructionsField.setText(assignment.instructions);

        answerKeyField.setText(assignment.answerKey);
    }

    @FXML
    void onNewAssignmentClick(ActionEvent event) {

        idField.clear();

        nameField.clear();
        instructionsField.clear();
        answerKeyField.clear();
    }


    @FXML
    void onSaveClick(ActionEvent event) {
        if (idField.getText().isEmpty()) {
            try (Connection c = MainDataSource.getConnection()) {
                PreparedStatement stmt = c.prepareStatement("INSERT INTO assignments (name, instructions, answer_key) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, nameField.getText());
                stmt.setString(2, instructionsField.getText());
                stmt.setString(3, answerKeyField.getText());
                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    idField.setText(String.valueOf(rs.getLong(1)));
                }
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Database Error");
                alert.setContentText(e.toString());
            }
        } else {
            try (Connection c = MainDataSource.getConnection()) {
                PreparedStatement stmt = c.prepareStatement("UPDATE assignments SET name = ?, instructions = ?, answer_key = ? WHERE id = ?");
                stmt.setString(1, nameField.getText());
                stmt.setString(2, instructionsField.getText());
                stmt.setString(3, answerKeyField.getText());
                stmt.setInt(4, Integer.parseInt(idField.getText()));
                stmt.executeUpdate();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Database Error");
                alert.setContentText(e.toString());
            }
        }


        refreshAssignmentList();
    }

    @FXML
    void onShowGradesClick(ActionEvent event) {
        if (idField.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No Assignment Selected");
            alert.setContentText("Please select an assignment to view grades.");
            alert.showAndWait();
            return;
        }

        long assignmentId = Long.parseLong(idField.getText());

        TableView<ArrayList<String>> tableView = new TableView<>();
        ObservableList<ArrayList<String>> data = FXCollections.observableArrayList();
        ArrayList<String> headers = new ArrayList<>();

        try (Connection conn = MainDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT u.username, g.grade FROM grades g JOIN users u ON g.user_id = u.id WHERE g.assignment_id = ?"
             )) {
            stmt.setLong(1, assignmentId);
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                final int columnIndex = i - 1;
                String headerText = metaData.getColumnLabel(i);
                headers.add(headerText);

                TableColumn<ArrayList<String>, String> column = new TableColumn<>(headerText);
                column.setCellValueFactory(cellData -> {
                    ArrayList<String> rowData = cellData.getValue();
                    if (rowData != null && columnIndex < rowData.size()) {
                        return new SimpleStringProperty(rowData.get(columnIndex));
                    } else {
                        return new SimpleStringProperty("");
                    }
                });
                column.setPrefWidth(150);
                tableView.getColumns().add(column);
            }

            while (rs.next()) {
                ArrayList<String> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String value = rs.getString(i);
                    row.add(value != null ? value : "");
                }
                data.add(row);
            }

            if (data.isEmpty()) {
                Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                infoAlert.setTitle("No Grades");
                infoAlert.setHeaderText(null);
                infoAlert.setContentText("There are no grades submitted for this assignment yet.");
                infoAlert.showAndWait();
                return;
            }

            tableView.setItems(data);
            StackPane root = new StackPane(tableView);
            Scene scene = new Scene(root, 400, 300);
            Stage stage = new Stage();
            stage.setTitle("Grades for Assignment ID: " + assignmentId);
            stage.setScene(scene);
            stage.show();

        } catch (SQLException e) {
            e.printStackTrace();
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Database Error");
            errorAlert.setHeaderText("Could not retrieve grades.");
            errorAlert.setContentText(e.getMessage());
            errorAlert.showAndWait();
        }
    }


    @FXML
    void onTestButtonClick(ActionEvent event) {
        Stage stage = new Stage();
        stage.setTitle("Query Results");


        TableView<ArrayList<String>> tableView = new TableView<>();

        ObservableList<ArrayList<String>> data = FXCollections.observableArrayList();
        ArrayList<String> headers = new ArrayList<>(); // To check if any columns were returned


        try (Connection conn = GradingDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(answerKeyField.getText())) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                final int columnIndex = i - 1;
                String headerText = metaData.getColumnLabel(i);
                headers.add(headerText);

                TableColumn<ArrayList<String>, String> column = new TableColumn<>(headerText);

                column.setCellValueFactory(cellData -> {
                    ArrayList<String> rowData = cellData.getValue();
                    if (rowData != null && columnIndex < rowData.size()) {
                        return new SimpleStringProperty(rowData.get(columnIndex));
                    } else {
                        return new SimpleStringProperty("");
                    }
                });
                column.setPrefWidth(120);
                tableView.getColumns().add(column);
            }

            // 2. Get Data Rows
            while (rs.next()) {
                ArrayList<String> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String value = rs.getString(i);
                    row.add(value != null ? value : "");
                }
                data.add(row);
            }


            if (headers.isEmpty() && data.isEmpty()) {

                Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                infoAlert.setTitle("Query Results");
                infoAlert.setHeaderText(null);
                infoAlert.setContentText("The query executed successfully but returned no data.");
                infoAlert.showAndWait();
                return;
            }


            tableView.setItems(data);


            StackPane root = new StackPane();
            root.getChildren().add(tableView);
            Scene scene = new Scene(root, 800, 600); // Adjust size as needed

            // 6. Set scene and show stage
            stage.setScene(scene);
            stage.show();

        } catch (SQLException e) {
            // Log the error and show an alert to the user
            e.printStackTrace(); // Print stack trace to console/log for debugging
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Database Error");
            errorAlert.setHeaderText("Failed to execute query or retrieve results.");
            errorAlert.setContentText("SQL Error: " + e.getMessage());
            errorAlert.showAndWait();
        } catch (Exception e) {
            // Catch other potential exceptions (e.g., class loading if driver not found)
            e.printStackTrace(); // Print stack trace to console/log for debugging
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setHeaderText("An unexpected error occurred.");
            errorAlert.setContentText(e.getMessage());
            errorAlert.showAndWait();
        }
    } // End of onTestButtonClick method


    @FXML
    void onDeleteAssignmentClick(ActionEvent event) {
        if (idField.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No Assignment Selected");
            alert.setContentText("Please select an assignment to delete.");
            alert.showAndWait();
            return;
        }

        long assignmentId = Long.parseLong(idField.getText());

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Deletion");
        confirmationAlert.setHeaderText("Are you sure you want to delete this assignment?");
        confirmationAlert.setContentText("This action cannot be undone.");
        confirmationAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection conn = MainDataSource.getConnection()) {
                    String deleteQuery = "DELETE FROM assignments WHERE id = ?";
                    PreparedStatement stmt = conn.prepareStatement(deleteQuery);
                    stmt.setLong(1, assignmentId);
                    stmt.executeUpdate();

                    refreshAssignmentList();

                    idField.clear();
                    nameField.clear();
                    instructionsField.clear();
                    answerKeyField.clear();

                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Success");
                    successAlert.setHeaderText("Assignment Deleted");
                    successAlert.setContentText("The assignment has been successfully deleted.");
                    successAlert.showAndWait();

                } catch (SQLException e) {
                    e.printStackTrace();
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Database Error");
                    errorAlert.setHeaderText("Failed to delete assignment");
                    errorAlert.setContentText(e.getMessage());
                    errorAlert.showAndWait();
                }
            }
        });
    }


    @FXML
    private void TombolBalik2(ActionEvent event) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/bdsqltester/login-view.fxml"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setTitle("Login Page");
        stage.setScene(new Scene(root));
        stage.show();
    }
}
