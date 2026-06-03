package com.campus.menu;

import com.campus.entity.Student;
import com.campus.exception.CampusPaymentException;
import com.campus.service.interfaces.IStudentService;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Console controller for the student module.
 *
 * WHY  : Isolates all user I/O (prompts, printing) from business logic, so the
 *        service stays UI-agnostic and testable (layered architecture).
 * HOW  : Reads input from a shared Scanner, delegates every operation to
 *        IStudentService, and turns checked CampusPaymentExceptions into
 *        friendly one-line messages. This layer does NOT validate or persist.
 * USED BY : MainMenu (delegates "Student Management" here).
 */
public class StudentMenu {

    private final IStudentService studentService;
    private final Scanner scanner;

    public StudentMenu(IStudentService studentService, Scanner scanner) {
        this.studentService = studentService;
        this.scanner = scanner;
    }

    /** Runs the student sub-menu until the user chooses to go back. */
    public void show() {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- Student Management ---");
            System.out.println("1. Register Student");
            System.out.println("2. View Student");
            System.out.println("3. List All Students");
            System.out.println("4. Update Student");
            System.out.println("5. Delete Student");
            System.out.println("6. Back");
            System.out.print("Choice: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> registerStudent();
                case "2" -> viewStudent();
                case "3" -> listStudents();
                case "4" -> updateStudent();
                case "5" -> deleteStudent();
                case "6" -> back = true;
                default -> System.out.println("Invalid choice");
            }
        }
    }

    private void registerStudent() {
        Student student = Student.builder()
                .name(prompt("Name"))
                .email(prompt("Email"))
                .department(prompt("Department"))
                .phone(emptyToNull(prompt("Phone (10 digits, optional)")))
                .build();
        try {
            String id = studentService.registerStudent(student);
            System.out.println("Registered. Student id: " + id);
        } catch (CampusPaymentException e) {
            System.out.println("Could not register: " + e.getMessage());
        }
    }

    private void viewStudent() {
        Optional<Student> student = studentService.getStudent(prompt("Student id"));
        if (student.isPresent()) {
            System.out.println(student.get());
        } else {
            System.out.println("No student found with that id.");
        }
    }

    private void listStudents() {
        List<Student> students = studentService.getAllStudents();
        if (students.isEmpty()) {
            System.out.println("No students yet.");
            return;
        }
        students.forEach(s ->
                System.out.println(s.getStudentId() + " | " + s.getName()
                        + " | " + s.getEmail() + " | " + s.getDepartment()));
    }

    private void updateStudent() {
        String id = prompt("Student id to update");
        Optional<Student> existing = studentService.getStudent(id);
        if (existing.isEmpty()) {
            System.out.println("No student found with that id.");
            return;
        }
        Student student = existing.get();
        // Keep current value when the user leaves a field blank.
        student.setName(orKeep(prompt("Name [" + student.getName() + "]"), student.getName()));
        student.setEmail(orKeep(prompt("Email [" + student.getEmail() + "]"), student.getEmail()));
        student.setDepartment(orKeep(prompt("Department [" + student.getDepartment() + "]"), student.getDepartment()));
        student.setPhone(orKeep(prompt("Phone [" + student.getPhone() + "]"), student.getPhone()));
        try {
            studentService.updateStudent(student);
            System.out.println("Updated.");
        } catch (CampusPaymentException e) {
            System.out.println("Could not update: " + e.getMessage());
        }
    }

    private void deleteStudent() {
        try {
            studentService.deleteStudent(prompt("Student id to delete"));
            System.out.println("Deleted.");
        } catch (CampusPaymentException e) {
            System.out.println("Could not delete: " + e.getMessage());
        }
    }

    // --- small input helpers ---

    private String prompt(String label) {
        System.out.print(label + ": ");
        return scanner.nextLine().trim();
    }

    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private String orKeep(String entered, String current) {
        return entered.isBlank() ? current : entered;
    }
}
