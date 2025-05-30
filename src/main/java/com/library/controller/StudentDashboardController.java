package com.library.controller;

import com.library.model.Book;
import com.library.model.BookRequest;
import com.library.model.Student;
import com.library.model.StudentProfile;
import com.library.model.RequestStatus;
import com.library.service.BookService;
import com.library.service.StudentService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.library.model.Borrowing;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.layout.VBox;
import com.library.model.User;
import java.util.prefs.Preferences;
import java.time.format.DateTimeFormatter;
import com.library.service.BorrowingService;
import java.io.IOException;

public class StudentDashboardController {
    @FXML
    private Label welcomeLabel;
    @FXML
    private TextField bookSearchField;
    @FXML
    private ComboBox<String> searchFilter;
    @FXML
    private TableView<Book> booksTable;
    @FXML
    private TableColumn<Book, String> titleColumn;
    @FXML
    private TableColumn<Book, String> authorColumn;
    @FXML
    private TableColumn<Book, String> statusColumn;
    @FXML
    private Button requestBookButton;

    @FXML
    private TableView<BookRequest> requestsTable;
    @FXML
    private TableColumn<BookRequest, String> requestBookTitleColumn;
    @FXML
    private TableColumn<BookRequest, LocalDateTime> requestDateColumn;
    @FXML
    private TableColumn<BookRequest, LocalDateTime> requestDueDateColumn;
    @FXML
    private TableColumn<BookRequest, String> requestStatusColumn;

    @FXML
    private TableView<Borrowing> borrowingsTable;
    @FXML
    private TableColumn<Borrowing, String> borrowingBookTitleColumn;
    @FXML
    private TableColumn<Borrowing, LocalDate> borrowingDateColumn;
    @FXML
    private TableColumn<Borrowing, LocalDate> borrowingDueDateColumn;
    @FXML
    private TableColumn<Borrowing, String> borrowingStatusColumn;

    private final BookService bookService;
    private final StudentService studentService;
    private final BorrowingService borrowingService;
    private Student currentStudent;
    private User currentUser;
    private ObservableList<Book> books = FXCollections.observableArrayList();
    private ObservableList<BookRequest> requests = FXCollections.observableArrayList();

    public StudentDashboardController() {
        this.bookService = new BookService();
        this.studentService = new StudentService();
        this.borrowingService = new BorrowingService();
    }

    @FXML
    public void initialize() {
        try {
            // Setup tables
            setupBooksTable();
            setupRequestsTable();
            setupBorrowingsTable();

            // Add listeners to search fields
            bookSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.isEmpty()) {
                    loadBooks();
                }
            });

            // Load only books initially
            loadBooks();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to initialize student dashboard: " + e.getMessage());
        }
    }

    public void setUser(User user) {
        this.currentUser = user;
        // Get the student associated with this user
        this.currentStudent = studentService.getStudentByEmail(user.getEmail());
        if (currentStudent != null) {
            welcomeLabel.setText("Welcome, " + currentStudent.getName());
            loadInitialData();
        } else {
            showAlert("Error", "Student information not found.");
        }
    }

    public void setCurrentStudent(Student student) {
        this.currentStudent = student;
        welcomeLabel.setText("Welcome, " + student.getName());
        loadInitialData();
    }

    @FXML
    private void handleSearchBooks() {
        String searchText = bookSearchField.getText().trim();
        try {
            List<Book> books;
            if (searchText.isEmpty()) {
                books = bookService.getAllBooks();
            } else {
                books = bookService.searchBooks(searchText);
            }
            booksTable.setItems(FXCollections.observableArrayList(books));
        } catch (Exception e) {
            showAlert("Error", "Failed to search books: " + e.getMessage());
        }
    }

    @FXML
    private void handleBookRequest(Book book) {
        if (!currentStudent.canBorrowMore()) {
            showAlert("Error", "You have reached the maximum number of books you can borrow!");
            return;
        }

        try {
            LocalDateTime dueDate = LocalDateTime.now().plusDays(14); // 2 weeks borrowing period
            boolean success = studentService.requestBook(currentStudent.getId(), book.getId(),
                    dueDate);

            if (success) {
                showAlert("Success", "Book request submitted successfully!");
                loadRequests();
            } else {
                showAlert("Error", "Failed to submit book request. Please try again.");
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to submit book request: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/student_profile.fxml"));
            Parent root = loader.load();
            StudentProfileController controller = loader.getController();

            // Convert Student to StudentProfile
            StudentProfile profile = new StudentProfile(
                    currentStudent.getId().intValue(),
                    currentStudent.getName(),
                    currentStudent.getStudentId(),
                    currentStudent.getContactNumber(),
                    currentStudent.getEmail(),
                    currentStudent.getBooksBorrowed(),
                    currentStudent.getMaxBooks());
            controller.setCurrentStudent(profile);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Student Profile");
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Failed to load profile: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Library Management System - Login");
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Failed to load login screen: " + e.getMessage());
        }
    }

    @FXML
    private void handleMyAccount() {
        try {
            // Get current student
            if (currentStudent == null) {
                showAlert("Error", "Student information not found.");
                return;
            }

            // Load account dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/account_dialog.fxml"));
            Parent root = loader.load();
            AccountDialogController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("My Account");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);

            controller.setStudent(currentStudent);
            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();

            // Refresh student data after any changes
            Student updatedStudent = studentService.getStudentById(currentStudent.getId());
            if (updatedStudent != null) {
                setCurrentStudent(updatedStudent);
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to open account dialog: " + e.getMessage());
        }
    }

    private void loadBooks() {
        try {
            List<Book> books = bookService.getAllBooks();
            booksTable.setItems(FXCollections.observableArrayList(books));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load books: " + e.getMessage());
        }
    }

    private void loadRequests() {
        List<BookRequest> studentRequests = studentService.getStudentRequests(currentStudent.getId());
        requests.setAll(studentRequests);
        requestsTable.setItems(requests);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showBookDetails(Book book) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/book_details_dialog.fxml"));
            Parent root = loader.load();
            BookDetailsDialogController controller = loader.getController();
            controller.setBook(book, false); // false for student view

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Book Details: " + book.getTitle());
            dialogStage.setScene(new Scene(root, 800, 600));
            dialogStage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open book details: " + e.getMessage());
        }
    }

    @FXML
    private void handleRequestBook() {
        Book selectedBook = booksTable.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert("No Selection", "Please select a book to request.");
            return;
        }

        try {
            // Check if student can borrow more books
            if (currentStudent.getBooksBorrowed() >= currentStudent.getMaxBooks()) {
                showAlert("Cannot Request", "You have reached the maximum number of books you can borrow.");
                return;
            }

            // Submit the request
            studentService.requestBook(currentStudent.getId(), selectedBook.getId(), LocalDateTime.now().plusDays(14));
            showAlert("Success", "Book request submitted successfully!");
            loadRequests(); // Refresh requests table
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to submit book request: " + e.getMessage());
        }
    }

    private void setupBooksTable() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        statusColumn.setCellValueFactory(cellData -> {
            Book book = cellData.getValue();
            long availableCopies = book.getCopies().stream()
                    .filter(copy -> "Available".equals(copy.getStatus()))
                    .count();
            return new SimpleStringProperty(availableCopies > 0 ? "Available" : "Not Available");
        });

        // Add double-click handler for book details
        booksTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Book selectedBook = booksTable.getSelectionModel().getSelectedItem();
                if (selectedBook != null) {
                    showBookDetails(selectedBook);
                }
            }
        });

        // Enable/disable request button based on selection
        requestBookButton.setDisable(true);
        booksTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    boolean isEnabled = newSelection != null &&
                            newSelection.getCopies().stream()
                                    .anyMatch(copy -> "Available".equals(copy.getStatus()));
                    requestBookButton.setDisable(!isEnabled);
                });
    }

    private void setupRequestsTable() {
        requestBookTitleColumn
                .setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getBook().getTitle()));
        requestDateColumn.setCellValueFactory(new PropertyValueFactory<>("requestDate"));
        requestDueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        requestStatusColumn
                .setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().toString()));
    }

    private void setupBorrowingsTable() {
        borrowingBookTitleColumn
                .setCellValueFactory(
                        cellData -> new SimpleStringProperty(cellData.getValue().getBookCopy().getBook().getTitle()));
        borrowingDateColumn.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        borrowingDueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        borrowingStatusColumn.setCellValueFactory(cellData -> {
            Borrowing borrowing = cellData.getValue();
            String status = borrowing.getReturnDate() != null ? "Returned" : "Borrowed";
            if (borrowing.getReturnDate() == null && borrowing.getDueDate().isBefore(LocalDate.now())) {
                status = "Overdue";
            }
            return new SimpleStringProperty(status);
        });
    }

    private void loadInitialData() {
        loadBooks();
        loadRequests();
        loadBorrowings();
    }

    private void loadBorrowings() {
        try {
            List<Borrowing> borrowings = borrowingService.getStudentBorrowings(currentStudent.getId());
            borrowingsTable.setItems(FXCollections.observableArrayList(borrowings));
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load borrowings: " + e.getMessage());
        }
    }
}