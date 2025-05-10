
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.stream.Collectors;
// === Base Classes ===
abstract class User {
    String name;
    String password;
 
    public User(String name, String password) {
        this.name = name;
        this.password = password;
    }
 
    public String getName() {
        return name;
    }
 
    public String getPassword() {
        return password;
    }
 
    public abstract void showDashboard();
}
 
class Admin extends User {
    static List<Teacher> teachers = new ArrayList<>();
    static List<Course> courses = new ArrayList<>();
    static List<Classroom> classrooms = new ArrayList<>();
    static List<ScheduleEntry> masterSchedule = new ArrayList<>();
    static List<Student> students = new ArrayList<>();
 
    public Admin(String name, String password) {
        super(name, password);
        if (classrooms.isEmpty()) {
            classrooms.add(new Classroom("A101", 30));
            classrooms.add(new Classroom("B102", 35));
            classrooms.add(new Classroom("C103", 40));
            classrooms.add(new Classroom("D104", 25));
            classrooms.add(new Classroom("E105", 45));
        }
    }
 
    @Override
    public void showDashboard() {
        new AdminDashboard(this);
    }
 
    private void log(String message) {
        System.out.println(message);
    }
 
    public void addTeacher(String teacherName, String password) {
        teachers.add(new Teacher(teacherName, password));
    }
 
    public void addStudent(String studentName, String password) {
        students.add(new Student(studentName, password));
    }
 
    public void addCourse(String name, String code, int maxLabSections, int maxLectureSections) {
        if (teachers.isEmpty()) {
            throw new IllegalStateException("Cannot add course: No teachers available");
        }
        Course newCourse = new Course(name, code, maxLabSections, maxLectureSections);
        courses.add(newCourse);
    }
 
    public void addClassroom(String id, int capacity) {
        classrooms.add(new Classroom(id, capacity));
    }
 
    public void assignSchedule(ScheduleEntry entry) {
        boolean roomConflict = masterSchedule.stream()
                .anyMatch(existing -> existing.slot.equals(entry.slot)
                        && existing.room.equals(entry.room));
 
        if (roomConflict) {
            throw new IllegalArgumentException("Room " + entry.room + " is already booked at " + entry.slot);
        }
 
        boolean teacherConflict = masterSchedule.stream()
                .anyMatch(existing -> existing.slot.equals(entry.slot)
                        && existing.teacher.equals(entry.teacher));
 
        if (teacherConflict) {
            throw new IllegalArgumentException("Teacher " + entry.teacher.getName() +
                    " is already teaching at " + entry.slot);
        }
 
        entry.teacher.addScheduleEntry(entry);
        masterSchedule.add(entry);
    }
 
    public void assignCourseToTeacher(Course course, Teacher teacher) {
        if (!teacher.courses.contains(course)) {
            teacher.addCourse(course);
            log("Assigned " + course.code + " to " + teacher.getName());
            System.out.println("Courses for " + teacher.getName() + ": " + teacher.courses);
        }
    }
 
    public void removeCourse(Course course) {
        masterSchedule.removeIf(entry -> entry.course.equals(course));
        teachers.forEach(t -> t.courses.remove(course));
        courses.remove(course);
    }
 
    public void removeTeacher(Teacher teacher) {
        masterSchedule.removeIf(entry -> entry.teacher.equals(teacher));
        teachers.remove(teacher);
    }
 
    public void removeStudent(Student student) {
        students.remove(student);
    }
 
    public List<ScheduleEntry> getAvailableSections() {
        return masterSchedule;
    }
 
    public void resetAllData() {
        teachers.clear();
        courses.clear();
        masterSchedule.clear();
        classrooms.clear();
        students.clear();
        classrooms.add(new Classroom("A101", 30));
        classrooms.add(new Classroom("B102", 35));
        classrooms.add(new Classroom("C103", 40));
        classrooms.add(new Classroom("D104", 25));
        classrooms.add(new Classroom("E105", 45));
    }
}
class Teacher extends User {
    List<Course> courses = new ArrayList<>();
    List<ScheduleEntry> schedule = new ArrayList<>();
 
    public Teacher(String name, String password) {
        super(name, password);
    }
 
    public void addCourse(Course course) {
    if (!courses.contains(course)) {
        courses.add(course);
        System.out.println("Added course " + course.code + " to " + getName() + "'s schedule");
    }
}
 
    public void addScheduleEntry(ScheduleEntry entry) {
        for (ScheduleEntry existing : schedule) {
            if (existing.slot.equals(entry.slot)) {
                throw new IllegalArgumentException("Time slot conflict for teacher " + getName());
            }
        }
        schedule.add(entry);
    }
 
    @Override
    public void showDashboard() {
        new TeacherDashboard(this);
    }
 
    @Override
    public String toString() {
        return getName();
    }
}
 
class Student extends User {
    List<ScheduleEntry> selectedCourses;
 
    public Student(String name, String password) {
        super(name, password);
        this.selectedCourses = new ArrayList<>();
    }
 
    @Override
    public void showDashboard() {
        new StudentDashboard(this);
    }
 
    public boolean hasCourseConflict(ScheduleEntry newEntry) {
        for (ScheduleEntry existing : selectedCourses) {
            if (existing.slot.equals(newEntry.slot)) {
                return true;
            }
        }
        return false;
    }
}
 
 
class SaveTimetablePanel extends JPanel {
    public SaveTimetablePanel(TimetableGrid timetableGrid) {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        JButton savePNGButton = new JButton("Save as PNG");
        savePNGButton.addActionListener(e -> TimetableExporter.exportAsImage(timetableGrid));
        add(savePNGButton);
    }
}
 
class AdminClassroomPanel extends JPanel {
    private JPanel classroomButtonsPanel;
 
    public AdminClassroomPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
 
        JPanel addPanel = createAddClassroomPanel();
        add(addPanel, BorderLayout.NORTH);
 
        classroomButtonsPanel = new JPanel(new GridLayout(0, 3, 15, 15));
        updateClassroomButtons();
 
        JScrollPane scrollPane = new JScrollPane(classroomButtonsPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Available Classrooms"));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }
 
    private JPanel createAddClassroomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Add New Classroom"));
 
        JTextField classroomIdField = new JTextField(15);
        JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(30, 1, 200, 1));
        JButton addButton = new JButton("Add Classroom");
 
        addButton.addActionListener(e -> {
            String newId = classroomIdField.getText().trim();
            int capacity = (Integer) capacitySpinner.getValue();
 
            if (!newId.isEmpty()) {
                // Validate the ID format: 1 letter followed by 3 digits
                if (!newId.matches("^[A-Za-z]{1}\\d{3}$")) {
                    JOptionPane.showMessageDialog(this,
                            "Classroom ID must be one letter followed by three digits (e.g., A101, X999).",
                            "Invalid Classroom ID",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
 
                boolean exists = Admin.classrooms.stream()
                        .anyMatch(c -> c.id.equalsIgnoreCase(newId));
 
                if (!exists) {
                    Admin.classrooms.add(new Classroom(newId, capacity));
                    classroomIdField.setText("");
                    capacitySpinner.setValue(30);
                    updateClassroomButtons();
                    JOptionPane.showMessageDialog(this,
                            "Classroom " + newId + " added successfully!");
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Classroom ID already exists!",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Please enter a valid Classroom ID.",
                        "Missing Input",
                        JOptionPane.WARNING_MESSAGE);
            }
        });
 
        panel.add(new JLabel("Classroom ID:"));
        panel.add(classroomIdField);
        panel.add(new JLabel("Capacity:"));
        panel.add(capacitySpinner);
        panel.add(addButton);
 
        return panel;
    }
 
    private void updateClassroomButtons() {
        classroomButtonsPanel.removeAll();
 
        for (Classroom classroom : Admin.classrooms) {
            JPanel buttonPanel = new JPanel(new BorderLayout(5, 5));
 
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(150, 100));
            button.setLayout(new GridBagLayout());
 
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
 
            JLabel idLabel = new JLabel("<html><center>" + classroom.id + "</center></html>");
            JLabel capacityLabel = new JLabel("<html><center>Max Capacity: " + classroom.capacity + "</center></html>");
 
            idLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            capacityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
 
            infoPanel.add(Box.createVerticalStrut(5));
            infoPanel.add(idLabel);
            infoPanel.add(Box.createVerticalStrut(5));
            infoPanel.add(capacityLabel);
            infoPanel.add(Box.createVerticalStrut(5));
 
            infoPanel.setOpaque(false);
 
            button.add(infoPanel);
 
            button.addActionListener(e -> new ClassroomTimetableWindow(classroom));
 
            buttonPanel.add(button, BorderLayout.CENTER);
 
            JButton deleteButton = new JButton("×");
            deleteButton.setForeground(Color.RED);
            deleteButton.setFont(new Font(deleteButton.getFont().getName(), Font.BOLD, 16));
            deleteButton.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to delete classroom " + classroom.id + "?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION);
 
                if (confirm == JOptionPane.YES_OPTION) {
                    Admin.classrooms.remove(classroom);
                    updateClassroomButtons();
                }
            });
 
            deleteButton.setPreferredSize(new Dimension(30, 30));
            deleteButton.setMargin(new Insets(0, 0, 0, 0));
 
            buttonPanel.add(deleteButton, BorderLayout.EAST);
            classroomButtonsPanel.add(buttonPanel);
        }
 
        classroomButtonsPanel.revalidate();
        classroomButtonsPanel.repaint();
    }
}
 
class TimetableGrid extends JPanel {
    protected static final int HOURS = 10;
    protected static final String[] DAYS = { "M", "T", "W", "Th", "F" };
    protected JButton[][] buttons = new JButton[HOURS][DAYS.length];
 
    public TimetableGrid() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
 
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.2;
        gbc.weighty = 0.1;
        add(new JLabel("Time/Day"), gbc);
 
        for (int i = 0; i < DAYS.length; i++) {
            gbc.gridx = i + 1;
            gbc.gridy = 0;
            add(new JLabel(DAYS[i], SwingConstants.CENTER), gbc);
        }
 
        for (int hour = 0; hour < HOURS; hour++) {
            gbc.gridx = 0;
            gbc.gridy = hour + 1;
            add(new JLabel((hour + 8) + ":00", SwingConstants.RIGHT), gbc);
 
            for (int day = 0; day < DAYS.length; day++) {
                gbc.gridx = day + 1;
                buttons[hour][day] = new JButton("Free");
                buttons[hour][day].setPreferredSize(new Dimension(150, 60));
                buttons[hour][day].setMinimumSize(new Dimension(120, 40));
                add(buttons[hour][day], gbc);
            }
        }
 
        updateTimetable();
    }
 
    protected int getDayIndex(String day) {
        for (int i = 0; i < DAYS.length; i++) {
            if (DAYS[i].equals(day)) {
                return i;
            }
        }
        return -1;
    }
 
    public void updateTimetable() {
        for (int hour = 0; hour < HOURS; hour++) {
            for (int day = 0; day < DAYS.length; day++) {
                buttons[hour][day].setText("Free");
                buttons[hour][day].setBackground(null);
                buttons[hour][day].setOpaque(false);
            }
        }
 
        for (ScheduleEntry entry : Admin.masterSchedule) {
            int dayIndex = getDayIndex(entry.slot.day);
            int hourIndex = entry.slot.hour - 8;
 
            if (dayIndex >= 0 && hourIndex >= 0 && hourIndex < HOURS) {
                JButton button = buttons[hourIndex][dayIndex];
                button.setText("<html>" + entry.course.code + "<br>Sec: " + entry.section +
                        "<br>Room: " + entry.room.id + "</html>");
                button.setBackground(new Color(173, 216, 230));
                button.setOpaque(true);
                button.setToolTipText(entry.toString());
            }
        }
    }
}
 
class AdminTimetableGrid extends TimetableGrid {
    public AdminTimetableGrid() {
        super();
        showClassroomNames();
    }
 
    private void showClassroomNames() {
        for (int hour = 0; hour < HOURS; hour++) {
            for (int day = 0; day < DAYS.length; day++) {
                buttons[hour][day].setText("");
                buttons[hour][day].setBackground(null);
                buttons[hour][day].setOpaque(false);
            }
        }
 
        for (Classroom classroom : Admin.classrooms) {
            JButton classroomButton = new JButton(classroom.id);
            classroomButton.addActionListener(e -> new ClassroomTimetableWindow(classroom));
 
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = Admin.classrooms.indexOf(classroom) + 1;
            gbc.fill = GridBagConstraints.BOTH;
            add(classroomButton, gbc);
        }
    }
}
 
 
class Course {
    String name;
    String code;
    int maxLabSections;
    int maxLectureSections;
    private Set<Student> registeredStudentsSet; // Tracks unique students
 
    public Course(String name, String code, int maxLabSections, int maxLectureSections) {
        this.name = name;
        this.code = code;
        this.maxLabSections = maxLabSections;
        this.maxLectureSections = maxLectureSections;
        this.registeredStudentsSet = new HashSet<>(); // Initialize set
    }
 
    public String toString() {
        return name + " (" + code + ")";
    }
 
    public void incrementRegisteredStudents(Student student) {
        registeredStudentsSet.add(student); // Adds only if not already present
    }
 
    public void decrementRegisteredStudents(Student student) {
        registeredStudentsSet.remove(student);
    }
 
    public int getRegisteredStudents() {
        return registeredStudentsSet.size();
    }
}
 
class TimeSlot {
    String day;
    int hour;
    public TimeSlot(String day, int hour) {
        this.day = day;
        this.hour = hour;
    }
    public String toString() {
        return day + " " + hour + ":00";
    }
 
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TimeSlot other = (TimeSlot) obj;
        return day.equals(other.day) && hour == other.hour;
    }
 
    @Override
    public int hashCode() {
        return Objects.hash(day, hour);
    }
}
 
class Classroom {
    String id;
    int capacity;
 
    public Classroom(String id, int capacity) {
        if (!id.matches("^[A-Za-z]{1}\\d{3}$")) {
            throw new IllegalArgumentException(
                "Invalid classroom ID format: must be one letter followed by three digits (e.g., A101, X123)");
        }
        this.id = id;
        this.capacity = capacity;
    }
 
    @Override
    public String toString() {
        return id + " (Capacity: " + capacity + ")";
    }
}
 
class ScheduleEntry {
    Course course;
    String section;
    Teacher teacher;
    Classroom room;
    TimeSlot slot;
    String sectionType; // Add this field
 
    public ScheduleEntry(Course course, String section, Teacher teacher, Classroom room, TimeSlot slot,
            String sectionType) {
        this.course = course;
        this.section = section;
        this.teacher = teacher;
        this.room = room;
        this.slot = slot;
        this.sectionType = sectionType;
    }
 
    @Override
    public String toString() {
        return course + " " + sectionType + " Sec:" + section + " - " + teacher.getName() + " - Room:" + room + " - "
                + slot;
    }
}
 
class BITSTimeTablePolicy {
    private static final int MIN_GAP_BETWEEN_LABS = 1; // 1 day gap
    private static final int PREFERRED_START_HOUR = 8;
    private static final int PREFERRED_END_HOUR = 17;
 
    public static boolean validateLabSchedule(ScheduleEntry newEntry, List<ScheduleEntry> existingEntries) {
        if (!"Lab".equals(newEntry.sectionType)) {
            return true; // Only validate lab sections
        }
 
        return existingEntries.stream()
                .filter(entry -> entry.course.equals(newEntry.course) &&
                        "Lab".equals(entry.sectionType))
                .noneMatch(entry -> isConsecutiveDay(entry.slot.day, newEntry.slot.day));
    }
 
    private static boolean isConsecutiveDay(String day1, String day2) {
        String[] days = { "M", "T", "W", "Th", "F" };
        int index1 = Arrays.asList(days).indexOf(day1);
        int index2 = Arrays.asList(days).indexOf(day2);
        return Math.abs(index1 - index2) <= 1;
    }
 
    public static boolean validateTimePreference(TimeSlot slot) {
        return slot.hour >= PREFERRED_START_HOUR && slot.hour <= PREFERRED_END_HOUR;
    }
 
    public static final int MORNING_SLOT = 8; // 8 AM
    public static final int MIDMORNING_SLOT = 10; // 10 AM
    public static final int AFTERNOON_SLOT = 14; // 2 PM
 
    public static final String[] MWF_PATTERN = { "M", "W", "F" };
    public static final String[] TTH_PATTERN = { "T", "Th" };
 
    public static List<SchedulePattern> getDefaultPatterns() {
        List<SchedulePattern> patterns = new ArrayList<>();
        patterns.add(new SchedulePattern(MWF_PATTERN, MORNING_SLOT, "Lecture"));
        patterns.add(new SchedulePattern(TTH_PATTERN, MIDMORNING_SLOT, "Lecture", "F", AFTERNOON_SLOT));
        return patterns;
    }
}
 
class SchedulePattern {
    String[] mainDays;
    int mainTime;
    String extraDay;
    int extraTime;
    String sectionType;
 
    public SchedulePattern(String[] mainDays, int mainTime, String sectionType) {
        this(mainDays, mainTime, sectionType, null, 0);
    }
 
    public SchedulePattern(String[] mainDays, int mainTime, String sectionType, String extraDay, int extraTime) {
        this.mainDays = mainDays;
        this.mainTime = mainTime;
        this.sectionType = sectionType;
        this.extraDay = extraDay;
        this.extraTime = extraTime;
    }
}
 
class TimetableAutoSuggestion {
    private Course course;
    private List<List<ScheduleEntry>> suggestions;
    private int currentIndex;
    private Random random;
 
    public TimetableAutoSuggestion(Course course) {
        this.course = course;
        this.suggestions = new ArrayList<>();
        this.currentIndex = -1;
        this.random = new Random();
        generateSuggestions();
    }
 
    private void generateSuggestions() {
        List<SchedulePattern> patterns = BITSTimeTablePolicy.getDefaultPatterns();
 
        for (Teacher teacher : Admin.teachers) {
            for (Classroom room : Admin.classrooms) {
                for (SchedulePattern pattern : patterns) {
                    List<ScheduleEntry> patternEntries = new ArrayList<>();
                    boolean isValid = true;
 
                    // Generate entries for main days
                    for (String day : pattern.mainDays) {
                        ScheduleEntry entry = new ScheduleEntry(
                            course,
                            "Auto" + suggestions.size(),
                            teacher,
                            room,
                            new TimeSlot(day, pattern.mainTime),
                            pattern.sectionType
                        );
 
                        if (!isValidSuggestion(entry)) {
                            isValid = false;
                            break;
                        }
                        patternEntries.add(entry);
                    }
 
                    // Add extra day if specified
                    if (isValid && pattern.extraDay != null) {
                        ScheduleEntry extraEntry = new ScheduleEntry(
                            course,
                            "Auto" + suggestions.size(),
                            teacher,
                            room,
                            new TimeSlot(pattern.extraDay, pattern.extraTime),
                            pattern.sectionType
                        );
 
                        if (!isValidSuggestion(extraEntry)) {
                            isValid = false;
                        } else {
                            patternEntries.add(extraEntry);
                        }
                    }
 
                    if (isValid) {
                        suggestions.add(patternEntries);
                    }
                }
            }
        }
    }
 
    private boolean isValidSuggestion(ScheduleEntry suggestion) {
        return !hasTimeConflict(suggestion) &&
               BITSTimeTablePolicy.validateLabSchedule(suggestion, Admin.masterSchedule);
    }
 
    private boolean hasTimeConflict(ScheduleEntry entry) {
        return Admin.masterSchedule.stream()
            .anyMatch(existing -> existing.slot.equals(entry.slot) &&
                     (existing.room.equals(entry.room) ||
                      existing.teacher.equals(entry.teacher)));
    }
 
    public List<ScheduleEntry> getNextSuggestion() {
        if (suggestions.isEmpty()) return null;
        currentIndex = (currentIndex + 1) % suggestions.size();
        return suggestions.get(currentIndex);
    }
 
    public List<ScheduleEntry> getPreviousSuggestion() {
        if (suggestions.isEmpty()) return null;
        currentIndex = (currentIndex - 1 + suggestions.size()) % suggestions.size();
        return suggestions.get(currentIndex);
    }
 
    public List<ScheduleEntry> getRandomSuggestion() {
        if (suggestions.isEmpty()) return null;
        return suggestions.get(random.nextInt(suggestions.size()));
    }
}
 
 
class LoginFrame extends JFrame {
    private static final String ADMIN_NAME = "Admin";
    private static final String ADMIN_PASSWORD = "Adminpass";
 
    public LoginFrame() {
        setTitle("Timetable Builder Login");
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
 
        // Main panel with padding
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        mainPanel.setBackground(Color.WHITE);
 
        // Logo/Header Panel
        JLabel titleLabel = new JLabel("Timetable Builder");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
 
        JLabel subtitleLabel = new JLabel("Login to your account");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        subtitleLabel.setForeground(Color.GRAY);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
 
        // Form components
        JLabel roleLabel = new JLabel("Select Role");
        roleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        roleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
 
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"Admin", "Teacher", "Student"});
        roleBox.setMaximumSize(new Dimension(300, 35));
        roleBox.setAlignmentX(Component.CENTER_ALIGNMENT);
 
        JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        usernameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
 
        JTextField nameField = new JTextField();
        nameField.setMaximumSize(new Dimension(300, 35));
        nameField.setAlignmentX(Component.CENTER_ALIGNMENT);
 
        JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        passwordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
 
        JPasswordField passwordField = new JPasswordField();
        passwordField.setMaximumSize(new Dimension(300, 35));
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
 
        // Login button with explicit styling
        JButton loginButton = new JButton("Login");
        loginButton.setMaximumSize(new Dimension(300, 40));
        loginButton.setPreferredSize(new Dimension(300, 40));
        loginButton.setMinimumSize(new Dimension(300, 40));
        loginButton.setBackground(new Color(0, 131, 255));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setOpaque(true);
        loginButton.setBorderPainted(false);
 
        // Add components with spacing
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(subtitleLabel);
        mainPanel.add(Box.createVerticalStrut(30));
        mainPanel.add(roleLabel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(roleBox);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(usernameLabel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(nameField);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(passwordLabel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(passwordField);
        mainPanel.add(Box.createVerticalStrut(25));
        mainPanel.add(loginButton);
        mainPanel.add(Box.createVerticalStrut(20));
 
        // Action listener for login
        ActionListener loginAction = e -> {
            String name = nameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            String role = (String) roleBox.getSelectedItem();
 
            if (name.isEmpty() || password.isEmpty()) {
                showError("Please enter both username and password.");
                return;
            }
 
            System.out.println("Login attempt: role=" + role + ", name=" + name);
            handleLogin(name, password, role);
        };
 
        // Add action listener to button
        loginButton.addActionListener(loginAction);
 
        // Add Enter key support
        InputMap inputMap = mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = mainPanel.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "login");
        actionMap.put("login", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loginAction.actionPerformed(e);
            }
        });
 
        // Make sure to set the content pane's background
        getContentPane().setBackground(Color.WHITE);
        add(new JScrollPane(mainPanel));
        setVisible(true);
    }
 
    private void showError(String message) {
        JOptionPane.showMessageDialog(this,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }
 
    private void handleLogin(String name, String password, String role) {
        User user;
        switch (role) {
            case "Admin":
                if (name.equals(ADMIN_NAME) && password.equals(ADMIN_PASSWORD)) {
                    user = new Admin(name, password);
                    System.out.println("Admin login successful: " + name);
                    user.showDashboard();
                } else {
                    showError("Invalid admin credentials.");
                    System.out.println("Admin login failed: invalid credentials for " + name);
                }
                break;
            case "Teacher":
                Teacher foundTeacher = Admin.teachers.stream()
                        .filter(t -> t.getName().equalsIgnoreCase(name) && t.getPassword().equals(password))
                        .findFirst()
                        .orElse(null);
                if (foundTeacher != null) {
                    user = foundTeacher;
                    System.out.println("Teacher login successful: " + name);
                    user.showDashboard();
                } else {
                    boolean userExists = Admin.teachers.stream().anyMatch(t -> t.getName().equalsIgnoreCase(name));
                    if (userExists) {
                        showError("Incorrect password for teacher.");
                        System.out.println("Teacher login failed: incorrect password for " + name);
                    } else {
                        showError("Teacher not found. Please contact the administrator.");
                        System.out.println("Teacher login failed: user not found for " + name);
                    }
                }
                break;
            case "Student":
                Student foundStudent = Admin.students.stream()
                        .filter(s -> s.getName().equalsIgnoreCase(name) && s.getPassword().equals(password))
                        .findFirst()
                        .orElse(null);
                if (foundStudent != null) {
                    user = foundStudent;
                    System.out.println("Student login successful: " + name);
                    user.showDashboard();
                } else {
                    boolean userExists = Admin.students.stream().anyMatch(s -> s.getName().equalsIgnoreCase(name));
                    if (userExists) {
                        showError("Incorrect password for student.");
                        System.out.println("Student login failed: incorrect password for " + name);
                    } else {
                        showError("Student not found. Please contact the administrator.");
                        System.out.println("Student login failed: user not found for " + name);
                    }
                }
                break;
        }
    }
}
 
class ClassroomTimetableWindow extends JFrame {
    private final Classroom classroom;
    private final TimetableGrid timetableGrid;
 
    public ClassroomTimetableWindow(Classroom classroom) {
        this.classroom = classroom;
        setTitle("Timetable for " + classroom.id);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
 
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
 
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.add(new JLabel("Classroom: " + classroom.id));
 
        this.timetableGrid = new ClassroomTimetableGrid(classroom);
 
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (timetableGrid instanceof ClassroomTimetableGrid) {
                    ((ClassroomTimetableGrid) timetableGrid).updateClassroomTimetable();
                }
            }
        });
        headerPanel.add(refreshButton);
        mainPanel.add(headerPanel, BorderLayout.NORTH);
 
        JPanel savePanel = new SaveTimetablePanel(timetableGrid);
        headerPanel.add(savePanel);
 
        mainPanel.add(new JScrollPane(timetableGrid), BorderLayout.CENTER);
 
        add(mainPanel);
        setVisible(true);
    }
 
    private class ClassroomTimetableGrid extends TimetableGrid {
        private final Classroom classroom;
 
        public ClassroomTimetableGrid(Classroom classroom) {
            super();
            this.classroom = classroom;
            updateClassroomTimetable();
        }
 
        public void updateClassroomTimetable() {
            for (int hour = 0; hour < HOURS; hour++) {
                for (int day = 0; day < DAYS.length; day++) {
                    buttons[hour][day].setText("Free");
                    buttons[hour][day].setBackground(null);
                    buttons[hour][day].setOpaque(false);
                }
            }
 
            for (ScheduleEntry entry : Admin.masterSchedule) {
                if (entry.room.equals(classroom)) {
                    int dayIndex = getDayIndex(entry.slot.day);
                    int hourIndex = entry.slot.hour - 8;
 
                    if (dayIndex >= 0 && hourIndex >= 0 && hourIndex < HOURS) {
                        JButton button = buttons[hourIndex][dayIndex];
                        button.setText("<html>" + entry.course.code + "<br>Sec: " +
                                     entry.section + "<br>" + entry.teacher.getName() + "</html>");
                        button.setBackground(new Color(173, 216, 230));
                        button.setOpaque(true);
                        button.setToolTipText(entry.toString());
                    }
                }
            }
        }
 
        protected int getDayIndex(String day) {
            for (int i = 0; i < DAYS.length; i++) {
                if (DAYS[i].equals(day)) {
                    return i;
                }
            }
            return -1;
        }
    }
}
 
class ClassroomSelectionWindow extends JFrame {
    private JComboBox<Classroom> classroomComboBox;
    private DefaultComboBoxModel<Classroom> classroomModel;
 
    public ClassroomSelectionWindow() {
        setTitle("Classroom Selection");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
 
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
 
        JPanel selectionPanel = new JPanel(new BorderLayout(5, 5));
        selectionPanel.setBorder(BorderFactory.createTitledBorder("Select Classroom"));
 
        classroomModel = new DefaultComboBoxModel<>();
        for (Classroom classroom : Admin.classrooms) {
            classroomModel.addElement(classroom);
        }
        classroomComboBox = new JComboBox<>(classroomModel);
        selectionPanel.add(classroomComboBox, BorderLayout.CENTER);
 
        JButton selectButton = new JButton("View Timetable");
        selectButton.addActionListener(e -> {
            Classroom selected = (Classroom) classroomComboBox.getSelectedItem();
            if (selected != null) {
                new ClassroomTimetableWindow(selected);
            }
        });
        selectionPanel.add(selectButton, BorderLayout.SOUTH);
 
        JPanel addPanel = new JPanel(new BorderLayout(5, 5));
        addPanel.setBorder(BorderFactory.createTitledBorder("Add New Classroom"));
 
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
 
        JPanel idPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        idPanel.add(new JLabel("Classroom ID:"));
        JTextField newClassroomField = new JTextField(15);
        idPanel.add(newClassroomField);
 
        JPanel capacityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        capacityPanel.add(new JLabel("Capacity:"));
        JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(30, 1, 200, 1));
        capacityPanel.add(capacitySpinner);
 
        inputPanel.add(idPanel);
        inputPanel.add(capacityPanel);
 
        addPanel.add(inputPanel, BorderLayout.CENTER);
 
        JButton addButton = new JButton("Add Classroom");
        addButton.addActionListener(e -> {
            String newClassroomId = newClassroomField.getText().trim();
            int capacity = (Integer) capacitySpinner.getValue();
 
            if (!newClassroomId.isEmpty()) {
                // Validate classroom ID format: one letter followed by three digits
                if (!newClassroomId.matches("^[A-Za-z]{1}\\d{3}$")) {
                    JOptionPane.showMessageDialog(this,
                            "Classroom ID must be one letter followed by three digits (e.g., A101, X999).",
                            "Invalid Classroom ID",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
 
                boolean exists = Admin.classrooms.stream()
                        .anyMatch(c -> c.id.equalsIgnoreCase(newClassroomId));
 
                if (!exists) {
                    Classroom newClassroom = new Classroom(newClassroomId, capacity);
                    Admin.classrooms.add(newClassroom);
                    classroomModel.addElement(newClassroom);
                    newClassroomField.setText("");
                    capacitySpinner.setValue(30);
                    JOptionPane.showMessageDialog(this,
                            "Classroom " + newClassroomId + " (Capacity: " + capacity + ") added successfully!");
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Classroom ID already exists!",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Please enter a valid Classroom ID.",
                        "Missing Input",
                        JOptionPane.WARNING_MESSAGE);
            }
            // In the addButton.addActionListener:
 
        });
 
        addPanel.add(addButton, BorderLayout.EAST);
 
        JPanel listPanel = new JPanel(new BorderLayout(5, 5));
        listPanel.setBorder(BorderFactory.createTitledBorder("Current Classrooms"));
 
        JList<Classroom> classroomList = new JList<>(classroomModel);
        JScrollPane scrollPane = new JScrollPane(classroomList);
        listPanel.add(scrollPane, BorderLayout.CENTER);
 
        mainPanel.add(selectionPanel, BorderLayout.NORTH);
        mainPanel.add(addPanel, BorderLayout.CENTER);
        mainPanel.add(listPanel, BorderLayout.SOUTH);
 
        add(mainPanel);
        setVisible(true);
    }
}
 
class EditCourseWindow extends JFrame {
    private Admin admin;
    private JPanel coursesPanel;
 
    public EditCourseWindow(Admin admin) {
        this.admin = admin;
        setTitle("Edit Courses");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
 
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
 
        // Courses list panel
        coursesPanel = new JPanel();
        coursesPanel.setLayout(new BoxLayout(coursesPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(coursesPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Available Courses"));
 
        // Update courses list
        updateCoursesList(coursesPanel); // Pass the coursesPanel parameter
 
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        add(mainPanel);
        setVisible(true);
    }
    private void updateCoursesList(JPanel coursesPanel) {
    for (Course course : Admin.courses) {
        JPanel courseRow = new JPanel(new BorderLayout(5, 5));
        courseRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        // Count students registered for this course
        long studentCount = Admin.students.stream()
                .filter(student -> student.selectedCourses.stream()
                        .anyMatch(entry -> entry.course.equals(course)))
                .count();

        // Updated format to show max sections and registered students
        JLabel courseInfo = new JLabel(String.format(
                "<html><b>%s</b> (%s)<br>" +
                "Max Sections: %d Lecture, %d Lab<br>" +
                "Registered Students: %d</html>",
                course.name,
                course.code,
                course.maxLectureSections,
                course.maxLabSections,
                studentCount));

        JButton editButton = new JButton("Edit");
        final Admin finalAdmin = this.admin;
        editButton.addActionListener(e -> new CourseDetailsWindow(course, finalAdmin));

        courseRow.add(courseInfo, BorderLayout.CENTER);
        courseRow.add(editButton, BorderLayout.EAST);
        coursesPanel.add(courseRow);
        coursesPanel.add(Box.createVerticalStrut(5));
    }
}
}
 
class CourseDetailsWindow extends JFrame {
    private Course course;
    private Admin admin;
    private JPanel sectionsPanel;
 
    public CourseDetailsWindow(Course course, Admin admin) {
    this.course = course;
    this.admin = admin;
    setTitle("Edit Course: " + course.code);
    setSize(900, 600);
    setLocationRelativeTo(null);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
 
    // Main layout
    JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
 
    // Course Details Panel at top
   JPanel courseDetailsPanel = new JPanel(new GridBagLayout());
        courseDetailsPanel.setBorder(BorderFactory.createTitledBorder("Course Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Course Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        courseDetailsPanel.add(new JLabel("Course Name:"), gbc);

        JTextField nameField = new JTextField(course.name, 20);
        gbc.gridx = 1;
        courseDetailsPanel.add(nameField, gbc);

        // Course Code
        gbc.gridx = 2;
        courseDetailsPanel.add(new JLabel("Course Code:"), gbc);

        JTextField codeField = new JTextField(course.code, 10);
        gbc.gridx = 3;
        courseDetailsPanel.add(codeField, gbc);

        // Save Button
        JButton saveButton = new JButton("Save Changes");
        gbc.gridx = 4;
        courseDetailsPanel.add(saveButton, gbc);

        saveButton.addActionListener(e -> {
            String newName = nameField.getText().trim();
            String newCode = codeField.getText().trim();

            if (newName.isEmpty() || newCode.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Course name and code cannot be empty",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!newCode.matches("^[A-Za-z]{3}\\d{3}$")) {
                JOptionPane.showMessageDialog(this,
                    "Course code must be in the format: 3 letters followed by 3 digits (e.g., CSC101)",
                    "Invalid Course Code",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Check if the new code already exists (if changed)
            if (!newCode.equals(course.code) && 
                Admin.courses.stream().anyMatch(c -> c.code.equals(newCode))) {
                JOptionPane.showMessageDialog(this,
                    "Course code already exists!",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Update course details
            course.name = newName;
            course.code = newCode;

            JOptionPane.showMessageDialog(this,
                "Course details updated successfully!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
        });
 
    mainPanel.add(courseDetailsPanel, BorderLayout.NORTH);
 
    // Sections Panel
    JPanel sectionsContainer = new JPanel(new BorderLayout());
    sectionsContainer.setBorder(BorderFactory.createTitledBorder("Course Sections"));
 
    sectionsPanel = new JPanel();
    sectionsPanel.setLayout(new BoxLayout(sectionsPanel, BoxLayout.Y_AXIS));
 
    JScrollPane scrollPane = new JScrollPane(sectionsPanel);
    sectionsContainer.add(scrollPane, BorderLayout.CENTER);
 
    // Add Section button at bottom right
    JButton addSectionBtn = new JButton("Add Section");
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttonPanel.add(addSectionBtn);
    sectionsContainer.add(buttonPanel, BorderLayout.SOUTH);
 
    mainPanel.add(sectionsContainer, BorderLayout.CENTER);
 
    // Add the main panel to the frame
    add(mainPanel);
 
    // Add action listener for Add Section button
    addSectionBtn.addActionListener(e -> {
        new SectionManagementWindow(course, admin) {
            @Override
            public void dispose() {
                super.dispose();
                updateSectionsList(); // Refresh the sections list after adding new section
            }
        };
    });
 
    // Initial update
    updateSectionsList();
 
    setVisible(true);
}
 
private void updateSectionsList() {
    sectionsPanel.removeAll();

    Map<Teacher, List<ScheduleEntry>> sectionsByTeacher = Admin.masterSchedule.stream()
            .filter(entry -> entry.course.equals(course))
            .collect(Collectors.groupingBy(entry -> entry.teacher));

    for (Map.Entry<Teacher, List<ScheduleEntry>> teacherEntry : sectionsByTeacher.entrySet()) {
        Teacher teacher = teacherEntry.getKey();
        List<ScheduleEntry> teacherSections = teacherEntry.getValue();

        JPanel teacherPanel = new JPanel();
        teacherPanel.setLayout(new BoxLayout(teacherPanel, BoxLayout.Y_AXIS));
        teacherPanel.setBorder(BorderFactory.createTitledBorder("Teacher: " + teacher.getName()));

        Map<String, List<ScheduleEntry>> groupedSections = teacherSections.stream()
                .collect(Collectors.groupingBy(entry -> entry.section + "-" + entry.sectionType));

        for (List<ScheduleEntry> sectionEntries : groupedSections.values()) {
            ScheduleEntry firstEntry = sectionEntries.get(0);

            // Main section panel with BorderLayout
            JPanel sectionPanel = new JPanel(new BorderLayout(10, 0));
            sectionPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));

            // Center panel with GridBagLayout for vertical centering
            JPanel centerPanel = new JPanel(new GridBagLayout());
            JPanel contentPanel = new JPanel(new GridLayout(0, 1, 0, 2));

            // Add section header
            contentPanel.add(new JLabel("Section " + firstEntry.section + " (" + firstEntry.sectionType + ")"));

            // Add room and time information
            for (ScheduleEntry entry : sectionEntries) {
                contentPanel.add(
                        new JLabel("Room: " + entry.room.id + " - " + entry.slot.day + " " + entry.slot.hour + ":00"));
            }

            // Add registered students count
            long registeredStudents = Admin.students.stream()
                    .filter(student -> student.selectedCourses.stream()
                            .anyMatch(entry -> entry.course.equals(course) &&
                                    entry.section.equals(firstEntry.section) &&
                                    entry.sectionType.equals(firstEntry.sectionType)))
                    .count();
            contentPanel.add(new JLabel("Registered Students: " + registeredStudents));

            // Add content panel to center panel for vertical centering
            centerPanel.add(contentPanel);

            // Button panel
            JPanel buttonPanel = new JPanel(new GridBagLayout());
            JPanel innerButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));

            JButton editButton = new JButton("Edit");
            JButton deleteButton = new JButton("Delete");
            deleteButton.setForeground(Color.RED);

            editButton.addActionListener(e -> editSection(firstEntry));
            deleteButton.addActionListener(e -> deleteSection(firstEntry));

            innerButtonPanel.add(editButton);
            innerButtonPanel.add(deleteButton);
            buttonPanel.add(innerButtonPanel);

            // Add panels to main section panel
            sectionPanel.add(centerPanel, BorderLayout.CENTER);
            sectionPanel.add(buttonPanel, BorderLayout.EAST);

            // Set preferred height
            sectionPanel.setPreferredSize(new Dimension(sectionPanel.getPreferredSize().width, 100)); // Adjust height as needed

            teacherPanel.add(sectionPanel);
            teacherPanel.add(Box.createVerticalStrut(5));
        }

        sectionsPanel.add(teacherPanel);
        sectionsPanel.add(Box.createVerticalStrut(10));
    }

    sectionsPanel.revalidate();
    sectionsPanel.repaint();
}

private JPanel createSectionPanel(ScheduleEntry section) {
    JPanel panel = new JPanel(new BorderLayout(5, 5));
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(200, 200, 200)),
        BorderFactory.createEmptyBorder(5, 5, 5, 5)));
 
    String sectionInfo = String.format("<html><b>Section %s (%s)</b><br>Room: %s - %s %d:00</html>",
        section.section,
        section.sectionType,
        section.room.id,
        section.slot.day,
        section.slot.hour);
 
    JLabel infoLabel = new JLabel(sectionInfo);
    panel.add(infoLabel, BorderLayout.CENTER);
 
    // Create a panel for buttons
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
 
    // Edit button
    JButton editButton = new JButton("Edit");
    editButton.addActionListener(e -> editSection(section));
 
    // Delete button
    JButton deleteButton = new JButton("Delete");
    deleteButton.setForeground(Color.RED);
    deleteButton.addActionListener(e -> {
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete Section " + section.section + "?\nThis will delete all scheduled days for this section.",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
 
        if (confirm == JOptionPane.YES_OPTION) {
            deleteSection(section);
        }
    });
 
    buttonPanel.add(editButton);
    buttonPanel.add(deleteButton);
    panel.add(buttonPanel, BorderLayout.EAST);
 
    return panel;
}
 
private void deleteSection(ScheduleEntry section) {
    try {
        if (section.sectionType.equals("Lab")) {
            // For labs, delete only this specific lab entry
            Admin.masterSchedule.remove(section);
            section.teacher.schedule.remove(section);
        } else {
            // For lectures, keep the existing behavior of deleting all entries for this section
            List<ScheduleEntry> entriesToDelete = Admin.masterSchedule.stream()
                    .filter(existingEntry -> existingEntry.course.equals(section.course) &&
                            existingEntry.section.equals(section.section) &&
                            existingEntry.teacher.equals(section.teacher) &&
                            existingEntry.sectionType.equals("Lecture"))
                    .collect(Collectors.toList());
 
            // Remove all entries from master schedule
            Admin.masterSchedule.removeAll(entriesToDelete);
 
            // Remove entries from teacher's schedule
            section.teacher.schedule.removeAll(entriesToDelete);
        }
 
        // Update the display
        updateSectionsList();
 
        JOptionPane.showMessageDialog(
                this,
                "Section " + section.section + " has been deleted successfully.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(
                this,
                "Error deleting section: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
 
private void editSection(ScheduleEntry section) {
        // Open edit dialog
        new SectionEditDialog(section, admin) {
            @Override
            public void dispose() {
                super.dispose();
                updateSectionsList(); // Refresh the sections list after editing
            }
        };
    }
}
 
class AdminDashboard extends JFrame {
    private Admin admin;
    private JTextArea logArea;
    private TimetableGrid timetableGrid;
    private Runnable refreshTeacherAndCourseData;
    private JComboBox<Teacher> teacherForCourseBox;
    private JComboBox<Course> courseForTeacherBox;
    private JPanel topPanel;
    private JTabbedPane tabbedPane;
    private JPanel managementPanel;
    private JPanel editCoursePanel;

    // Add these new fields
    private JComboBox<Teacher> teacherBox;
    private JComboBox<Course> courseBox;
    private JComboBox<Classroom> roomBox;
    private JComboBox<Teacher> deleteTeacherBox;
    private JComboBox<Course> deleteCourseBox;
    private JComboBox<Student> deleteStudentBox;
    private JPanel teacherPanel;
    private JPanel studentPanel;
    private JPanel coursePanel;

    public AdminDashboard(Admin admin) {
        this.admin = admin;
        setTitle("Admin Dashboard");
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        logArea = new JTextArea(10, 40);
        logArea.setEditable(false);
        editCoursePanel = createEditCoursePanel();
        // Initialize ComboBoxes
        teacherBox = new JComboBox<>();
        courseBox = new JComboBox<>();
        roomBox = new JComboBox<>();
        deleteTeacherBox = new JComboBox<>();
        deleteCourseBox = new JComboBox<>();
        deleteStudentBox = new JComboBox<>();
        teacherForCourseBox = new JComboBox<>();
        courseForTeacherBox = new JComboBox<>();

        timetableGrid = new AdminTimetableGrid();

        JMenuBar menuBar = new JMenuBar();
        JMenu viewMenu = new JMenu("View");
        JMenuItem classroomMenuItem = new JMenuItem("Classroom Timetables");
        classroomMenuItem.addActionListener(e -> new ClassroomSelectionWindow());
        viewMenu.add(classroomMenuItem);
        menuBar.add(viewMenu);
        setJMenuBar(menuBar);

        tabbedPane = new JTabbedPane();

        // Initialize panels
        teacherPanel = new JPanel();
        studentPanel = new JPanel();
        coursePanel = new JPanel();
        topPanel = new JPanel();

        tabbedPane = new JTabbedPane();
        managementPanel = createManagementPanel();
        tabbedPane.addTab("Management", managementPanel);
        tabbedPane.addTab("Edit Courses", editCoursePanel); // Add the Edit Courses tab

        AdminClassroomPanel classroomPanel = new AdminClassroomPanel();
        tabbedPane.addTab("Classroom Overview", classroomPanel);

        // Define refreshTeacherAndCourseData before using it
        refreshTeacherAndCourseData = () -> {
    SwingUtilities.invokeLater(() -> {
        // Clear all teacher-related ComboBoxes
        teacherBox.removeAllItems();
        deleteTeacherBox.removeAllItems();
        teacherForCourseBox.removeAllItems();

        // Repopulate teacher ComboBoxes
        for (Teacher t : Admin.teachers) {
            teacherBox.addItem(t);
            deleteTeacherBox.addItem(t);
            teacherForCourseBox.addItem(t);
        }

        // Clear all course-related ComboBoxes
        courseBox.removeAllItems();
        deleteCourseBox.removeAllItems();
        courseForTeacherBox.removeAllItems();

        // Repopulate course ComboBoxes
        for (Course c : Admin.courses) {
            courseBox.addItem(c);
            deleteCourseBox.addItem(c);
            courseForTeacherBox.addItem(c);
        }

        // Clear and repopulate student ComboBox
        deleteStudentBox.removeAllItems();
        for (Student s : Admin.students) {
            deleteStudentBox.addItem(s);
        }

        // Update UI
        teacherPanel.revalidate();
        teacherPanel.repaint();
        coursePanel.revalidate();
        coursePanel.repaint();
        studentPanel.revalidate();
        studentPanel.repaint();
    });
};

        add(tabbedPane);
        setVisible(true);

        refreshTeacherAndCourseData.run();
    }

    private boolean isValidName(String name) {
        return name.matches("^[A-Za-z ]+$");
    }

    private JPanel createManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        logArea = new JTextArea(10, 40);
        logArea.setEditable(false);

        teacherForCourseBox = new JComboBox<>();
        courseForTeacherBox = new JComboBox<>();

        for (Teacher t : Admin.teachers) {
            teacherForCourseBox.addItem(t);
            teacherBox.addItem(t);
            deleteTeacherBox.addItem(t);
        }

        for (Course c : Admin.courses) {
            courseForTeacherBox.addItem(c);
            courseBox.addItem(c);
            deleteCourseBox.addItem(c);
        }

        for (Student s : Admin.students) {
            deleteStudentBox.addItem(s);
        }

        JPanel teacherPanel = new JPanel(new GridBagLayout());
        teacherPanel.setBorder(BorderFactory.createTitledBorder("Teacher Management"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        teacherPanel.add(new JLabel("Teacher Name:"), gbc);

        JTextField teacherNameField = new JTextField(15);
        gbc.gridx = 1;
        teacherPanel.add(teacherNameField, gbc);

        gbc.gridx = 2;
        teacherPanel.add(new JLabel("Password:"), gbc);

        JPasswordField teacherPasswordField = new JPasswordField(15);
        gbc.gridx = 3;
        teacherPanel.add(teacherPasswordField, gbc);

        JButton addTeacherBtn = new JButton("Add Teacher");
        gbc.gridx = 4;
        teacherPanel.add(addTeacherBtn, gbc);
        addTeacherBtn.addActionListener(e -> {
            String name = teacherNameField.getText().trim();
            String password = new String(teacherPasswordField.getPassword()).trim();

            if (!name.isEmpty() && !password.isEmpty()) {
                if (!isValidName(name)) {
                    JOptionPane.showMessageDialog(this, "Teacher name should contain only letters and spaces.");
                    return;
                }

                admin.addTeacher(name, password);
                log("Added Teacher: " + name);
                teacherNameField.setText("");
                teacherPasswordField.setText("");
                deleteTeacherBox.addItem(new Teacher(name, password));

                refreshTeacherAndCourseData.run();
            } else {
                JOptionPane.showMessageDialog(this, "Please enter both name and password.");
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 1;
        teacherPanel.add(new JLabel("Select Teacher:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        teacherPanel.add(deleteTeacherBox, gbc);

        JButton deleteTeacherBtn = new JButton("Delete Teacher");
        gbc.gridx = 4;
        gbc.gridwidth = 1;
        teacherPanel.add(deleteTeacherBtn, gbc);
        deleteTeacherBtn.addActionListener(e -> {
    Teacher selectedTeacher = (Teacher) deleteTeacherBox.getSelectedItem();
    if (selectedTeacher != null) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete teacher " + selectedTeacher.getName() + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            admin.removeTeacher(selectedTeacher);
            log("Deleted Teacher: " + selectedTeacher.getName());
            
            // Remove from all ComboBoxes immediately
            teacherBox.removeItem(selectedTeacher);
            deleteTeacherBox.removeItem(selectedTeacher);
            teacherForCourseBox.removeItem(selectedTeacher);
            
            // Refresh all data
            refreshTeacherAndCourseData.run();
            
            // Update UI
            revalidate();
            repaint();
        }
    }
});

        JPanel studentPanel = new JPanel(new GridBagLayout());
        studentPanel.setBorder(BorderFactory.createTitledBorder("Student Management"));
        GridBagConstraints sgbc = new GridBagConstraints();
        sgbc.anchor = GridBagConstraints.WEST;
        sgbc.insets = new Insets(5, 5, 5, 5);

        sgbc.gridx = 0;
        sgbc.gridy = 0;
        studentPanel.add(new JLabel("Student Name:"), sgbc);

        JTextField studentNameField = new JTextField(15);
        sgbc.gridx = 1;
        studentPanel.add(studentNameField, sgbc);

        sgbc.gridx = 2;
        studentPanel.add(new JLabel("Password:"), sgbc);

        JPasswordField studentPasswordField = new JPasswordField(15);
        sgbc.gridx = 3;
        studentPanel.add(studentPasswordField, sgbc);

        JButton addStudentBtn = new JButton("Add Student");
        sgbc.gridx = 4;
        studentPanel.add(addStudentBtn, sgbc);
        addStudentBtn.addActionListener(e -> {
            String name = studentNameField.getText().trim();
            String password = new String(studentPasswordField.getPassword()).trim();

            if (!name.isEmpty() && !password.isEmpty()) {
                if (!isValidName(name)) {
                    JOptionPane.showMessageDialog(this, "Student name should contain only letters and spaces.");
                    return;
                }

                admin.addStudent(name, password);
                log("Added Student: " + name);
                studentNameField.setText("");
                studentPasswordField.setText("");
                refreshTeacherAndCourseData.run();
            } else {
                JOptionPane.showMessageDialog(this, "Please enter both name and password.");
            }
        });

        JPanel coursePanel = new JPanel(new GridBagLayout());
        coursePanel.setBorder(BorderFactory.createTitledBorder("Course Management"));
        GridBagConstraints cgbc = new GridBagConstraints();
        cgbc.anchor = GridBagConstraints.WEST;
        cgbc.insets = new Insets(5, 5, 5, 5);
        cgbc.fill = GridBagConstraints.HORIZONTAL;

        // Course Name
        cgbc.gridx = 0;
        cgbc.gridy = 0;
        coursePanel.add(new JLabel("Course Name:"), cgbc);

        cgbc.gridx = 1;
        cgbc.gridwidth = 1;
        JTextField courseNameField = new JTextField(15);
        coursePanel.add(courseNameField, cgbc);

        // Course Code
        cgbc.gridx = 2;
        cgbc.gridwidth = 1;
        coursePanel.add(new JLabel("Code:"), cgbc);

        cgbc.gridx = 3;
        JTextField courseCodeField = new JTextField(8);
        coursePanel.add(courseCodeField, cgbc);

        // Max Lecture Sections
        cgbc.gridx = 0;
        cgbc.gridy = 1;
        coursePanel.add(new JLabel("Max Lecture Sections:"), cgbc);

        cgbc.gridx = 1;
        JTextField maxLectureSectionsField = new JTextField(3);
        coursePanel.add(maxLectureSectionsField, cgbc);

        // Max Lab Sections
        cgbc.gridx = 2;
        coursePanel.add(new JLabel("Max Lab Sections:"), cgbc);

        cgbc.gridx = 3;
        JTextField maxLabSectionsField = new JTextField(3);
        coursePanel.add(maxLabSectionsField, cgbc);

        // Add Course Button
        cgbc.gridx = 4;
        cgbc.gridy = 1;
        cgbc.anchor = GridBagConstraints.EAST;
        JButton addCourseBtn = new JButton("Add Course");
        coursePanel.add(addCourseBtn, cgbc);

        // Select Course and Delete Course
        cgbc.gridx = 0;
        cgbc.gridy = 2;
        cgbc.anchor = GridBagConstraints.WEST;
        coursePanel.add(new JLabel("Select Course:"), cgbc);

        cgbc.gridx = 1;
        cgbc.gridwidth = 2;
        coursePanel.add(deleteCourseBox, cgbc);

        cgbc.gridx = 4;
        cgbc.gridwidth = 1;
        JButton deleteCourseBtn = new JButton("Delete Course");
        coursePanel.add(deleteCourseBtn, cgbc);

        // In AdminDashboard class, modify the addCourseBtn.addActionListener:

        // In AdminDashboard, modify the addCourseBtn.addActionListener:
        JPanel coursesPanel = new JPanel();
        coursesPanel.setLayout(new BoxLayout(coursesPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(coursesPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Available Courses"));

        updateCoursesList(coursesPanel);
        addCourseBtn.addActionListener(e -> {
            String name = courseNameField.getText().trim();
            String code = courseCodeField.getText().trim();
            String maxLab = maxLabSectionsField.getText().trim();
            String maxLecture = maxLectureSectionsField.getText().trim();

            try {
                boolean courseExists = Admin.courses.stream()
                        .anyMatch(c -> c.code.equals(code));

                if (courseExists) {
                    JOptionPane.showMessageDialog(this,
                            "Course code already exists!",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int maxLabSec = Integer.parseInt(maxLab);
                int maxLectureSec = Integer.parseInt(maxLecture);

                if (!name.isEmpty() && !code.isEmpty()) {
                    if (!code.matches("^[A-Za-z]{3}\\d{3}$")) {
                        JOptionPane.showMessageDialog(this,
                                "Course code must be in the format: 3 letters followed by 3 digits (e.g., CSC101).",
                                "Invalid Course Code",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    admin.addCourse(name, code, maxLabSec, maxLectureSec);
                    log("Added Course: " + name + " (" + code + ") " +
                            "with " + maxLectureSec + " lecture and " + maxLabSec + " lab sections");

                    courseNameField.setText("");
                    courseCodeField.setText("");
                    deleteCourseBox.addItem(new Course(name, code, maxLabSec, maxLectureSec)); // Add new course to deleteCourseBox

                    maxLabSectionsField.setText("");
                    maxLectureSectionsField.setText("");

                    refreshTeacherAndCourseData.run();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null,
                        "Please enter valid numbers for maximum sections",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        JButton editCourseBtn = new JButton("Edit Courses");
        editCourseBtn.addActionListener(e -> new EditCourseWindow(admin));
        // Add this button to your management panel

        deleteCourseBtn.addActionListener(e -> {
            Course selectedCourse = (Course) deleteCourseBox.getSelectedItem();
            if (selectedCourse != null) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "Are you sure you want to delete course " + selectedCourse.name + "?",
                        "Confirm Delete",
                        JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    admin.removeCourse(selectedCourse);
                    log("Deleted Course: " + selectedCourse.name);
                    refreshTeacherAndCourseData.run();
                }
            }
        });

        topPanel.add(teacherPanel);
        topPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        topPanel.add(studentPanel);
        topPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        topPanel.add(coursePanel);
        topPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Reset All Data");
        refreshBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to reset all data? This will clear all teachers, courses, and schedules.",
                    "Confirm Reset",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                admin.resetAllData();
                log("All data has been reset");
                refreshTeacherAndCourseData.run();
            }
        });
        panel.add(refreshBtn, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createEditCoursePanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    // Create the courses panel with BoxLayout
    JPanel coursesPanel = new JPanel();
    coursesPanel.setLayout(new BoxLayout(coursesPanel, BoxLayout.Y_AXIS));
    
    // Create scroll pane
    JScrollPane scrollPane = new JScrollPane(coursesPanel);
    scrollPane.setBorder(BorderFactory.createTitledBorder("Available Courses"));
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    // Add refresh button
    JButton refreshButton = new JButton("Refresh Course List");
    refreshButton.addActionListener(e -> {
        coursesPanel.removeAll();
        updateCoursesList(coursesPanel);
        coursesPanel.revalidate();
        coursesPanel.repaint();
    });

    // Initial population of courses
    updateCoursesList(coursesPanel);

    // Add components to main panel
    panel.add(scrollPane, BorderLayout.CENTER);
    panel.add(refreshButton, BorderLayout.SOUTH);

    return panel;
}

    // Add this method to update the edit course panel
    private void updateCoursesList(JPanel coursesPanel) {
    for (Course course : Admin.courses) {
        JPanel courseRow = new JPanel(new BorderLayout(5, 5));
        courseRow.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        // Create a fixed-width panel for course info
        JPanel courseInfoPanel = new JPanel();
        courseInfoPanel.setLayout(new BoxLayout(courseInfoPanel, BoxLayout.Y_AXIS));
        courseInfoPanel.setPreferredSize(new Dimension(400, 60)); // Adjusted width

        // Count total registered students
        long totalRegisteredStudents = Admin.students.stream()
            .filter(student -> student.selectedCourses.stream()
                .anyMatch(entry -> entry.course.equals(course)))
            .count();

        // Create labels with HTML formatting
        JLabel nameLabel = new JLabel(String.format("<html><b>%s</b> (%s)</html>", 
            course.name, course.code));
        JLabel sectionsLabel = new JLabel(String.format("<html>Max Sections: %d Lecture, %d Lab | Registered Students: %d</html>",
            course.maxLectureSections, 
            course.maxLabSections,
            totalRegisteredStudents));
        
        // Add labels to the info panel
        courseInfoPanel.add(nameLabel);
        courseInfoPanel.add(sectionsLabel);

        // Create edit button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton editButton = new JButton("Edit");
        editButton.setPreferredSize(new Dimension(80, 30));
        final Admin finalAdmin = this.admin;
        editButton.addActionListener(e -> new CourseDetailsWindow(course, finalAdmin));
        buttonPanel.add(editButton);

        // Add panels to the course row
        courseRow.add(courseInfoPanel, BorderLayout.WEST);
        courseRow.add(buttonPanel, BorderLayout.EAST);

        // Add some empty space to the right of the course info
        courseRow.add(Box.createHorizontalStrut(20), BorderLayout.CENTER);

        // Set maximum size for the course row
        courseRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        coursesPanel.add(courseRow);
        coursesPanel.add(Box.createVerticalStrut(5));
    }

    // If no courses, show a message
    if (Admin.courses.isEmpty()) {
        JLabel noCoursesLabel = new JLabel("No courses available");
        noCoursesLabel.setHorizontalAlignment(SwingConstants.CENTER);
        coursesPanel.add(noCoursesLabel);
    }

    coursesPanel.revalidate();
    coursesPanel.repaint();
}
    private void updateEditCoursePanel() {
        if (editCoursePanel != null) {
            tabbedPane.remove(editCoursePanel);
            editCoursePanel = createEditCoursePanel();
            tabbedPane.addTab("Edit Courses", editCoursePanel);
            tabbedPane.revalidate();
            tabbedPane.repaint();
        }
    }

    // Modify your refreshTeacherAndCourseData to include updating the edit course panel

    private void log(String message) {
        if (logArea != null) {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }
}


class TeacherDashboard extends JFrame {
    Teacher teacher;
    TimetableGrid timetableGrid;
 
    public TeacherDashboard(Teacher teacher) {
        this.teacher = teacher;
        setTitle("Teacher Dashboard - " + teacher.getName());
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
 
        JPanel mainPanel = new JPanel(new BorderLayout());
 
        JPanel coursePanel = new JPanel();
        coursePanel.setBorder(BorderFactory.createTitledBorder("Your Courses"));
        coursePanel.setLayout(new BoxLayout(coursePanel, BoxLayout.Y_AXIS));
 
        JTextArea coursesArea = new JTextArea(5, 30);
        coursesArea.setEditable(false);
        try {
            if (teacher.courses != null && !teacher.courses.isEmpty()) {
                for (Course course : teacher.courses) {
                    if (course != null) {
                        coursesArea.append(String.format("• %s - %d students registered\n", course, course.getRegisteredStudents()));
                    }
                }
            } else {
                coursesArea.setText("No courses assigned.");
            }
        } catch (Exception e) {
            coursesArea.setText("Error loading courses: " + e.getMessage());
        }
        coursePanel.add(new JScrollPane(coursesArea));
 
        timetableGrid = new TeacherTimetableGrid(teacher);
        JScrollPane timetableScrollPane = new JScrollPane(timetableGrid);
        timetableScrollPane.setBorder(BorderFactory.createTitledBorder("Your Timetable"));
 
        JPanel savePanel = new SaveTimetablePanel(timetableGrid);
        mainPanel.add(savePanel, BorderLayout.SOUTH);
 
        mainPanel.add(coursePanel, BorderLayout.NORTH);
        mainPanel.add(timetableScrollPane, BorderLayout.CENTER);
 
        add(mainPanel);
        setVisible(true);
    }
 
    private class TeacherTimetableGrid extends TimetableGrid {
        private Teacher teacher;
 
        public TeacherTimetableGrid(Teacher teacher) {
            super();
            this.teacher = teacher;
            updateTeacherTimetable();
        }
 
        public void updateTeacherTimetable() {
            for (int hour = 0; hour < HOURS; hour++) {
                for (int day = 0; day < DAYS.length; day++) {
                    buttons[hour][day].setText("Free");
                    buttons[hour][day].setBackground(null);
                    buttons[hour][day].setOpaque(false);
                }
            }
 
            try {
                if (teacher.schedule != null) {
                    for (ScheduleEntry entry : teacher.schedule) {
                        if (entry != null && entry.slot != null) {
                            int dayIndex = getDayIndex(entry.slot.day);
                            int hourIndex = entry.slot.hour - 8;
 
                            if (dayIndex >= 0 && hourIndex >= 0 && hourIndex < HOURS) {
                                JButton button = getButtonAt(hourIndex, dayIndex);
                                if (button != null) {
                                    button.setText("<html>" + entry.course.code + "<br>Sec: " + entry.section +
                                            "<br>Room: " + entry.room.id + "</html>");
                                    button.setBackground(new Color(173, 216, 230));
                                    button.setOpaque(true);
                                    button.setToolTipText(entry.toString());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                buttons[0][0].setText("Error: " + e.getMessage());
            }
 
            revalidate();
            repaint();
        }
 
        private JButton getButtonAt(int hour, int day) {
            if (hour >= 0 && hour < HOURS && day >= 0 && day < DAYS.length) {
                return buttons[hour][day];
            }
            return null;
        }
    }
}
 
class StudentDashboard extends JFrame {
    private Student student;
    private JPanel availableSectionsPanel;
    private JPanel selectedCoursesPanel;
    private StudentTimetableGrid timetableGrid;
    private Set<String> lastCourseCodes;
    private boolean isUpdatingSections;
 
    public StudentDashboard(Student student) {
        this.student = student;
        if (student.selectedCourses == null) {
            student.selectedCourses = new ArrayList<>();
        }
        this.lastCourseCodes = new HashSet<>();
        this.isUpdatingSections = false;
 
        setTitle("Student Dashboard - " + student.getName());
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
 
        JTabbedPane tabbedPane = new JTabbedPane();
 
        JPanel courseSelectionPanel = createCourseSelectionPanel();
        tabbedPane.addTab("Course Selection", new JScrollPane(courseSelectionPanel));
 
        JPanel timetablePanel = new JPanel(new BorderLayout());
        timetableGrid = new StudentTimetableGrid();
 
        JPanel savePanel = new SaveTimetablePanel(timetableGrid);
        timetablePanel.add(savePanel, BorderLayout.NORTH);
        timetablePanel.add(timetableGrid, BorderLayout.CENTER);
 
        tabbedPane.addTab("Timetable View", timetablePanel);
 
        add(tabbedPane);
        setVisible(true);
 
        updateAvailableSections();
    }
 
    private JPanel createCourseSelectionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
 
        availableSectionsPanel = new JPanel();
        availableSectionsPanel.setLayout(new BoxLayout(availableSectionsPanel, BoxLayout.Y_AXIS));
        availableSectionsPanel.setBorder(BorderFactory.createTitledBorder("Available Courses"));
        JScrollPane availableScrollPane = new JScrollPane(availableSectionsPanel);
        availableScrollPane.setPreferredSize(new Dimension(500, 400));
        availableScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
 
        selectedCoursesPanel = new JPanel();
        selectedCoursesPanel.setLayout(new BoxLayout(selectedCoursesPanel, BoxLayout.Y_AXIS));
        selectedCoursesPanel.setBorder(BorderFactory.createTitledBorder("Your Selected Courses"));
        JScrollPane selectedScrollPane = new JScrollPane(selectedCoursesPanel);
        selectedScrollPane.setPreferredSize(new Dimension(500, 200));
 
        panel.add(availableScrollPane, BorderLayout.CENTER);
        panel.add(selectedScrollPane, BorderLayout.SOUTH);
 
        JButton refreshButton = new JButton("Refresh Available Courses");
        refreshButton.addActionListener(e -> forceUpdateAvailableSections());
        panel.add(refreshButton, BorderLayout.NORTH);
 
        return panel;
    }
 
    private void updateAvailableSections() {
        if (isUpdatingSections) {
            return;
        }
        isUpdatingSections = true;
 
        try {
            Set<String> currentCourseCodes = Admin.masterSchedule.stream()
                    .map(entry -> entry.course.code)
                    .collect(Collectors.toSet());
            if (currentCourseCodes.equals(lastCourseCodes)) {
                return;
            }
            lastCourseCodes = new HashSet<>(currentCourseCodes);
 
            availableSectionsPanel.removeAll();
 
            Map<String, List<ScheduleEntry>> groupedByCourse = Admin.masterSchedule.stream()
                    .collect(Collectors.groupingBy(entry -> entry.course.code));
 
            for (Map.Entry<String, List<ScheduleEntry>> courseEntry : groupedByCourse.entrySet()) {
                String courseCode = courseEntry.getKey();
                List<ScheduleEntry> courseEntries = courseEntry.getValue();
                Course course = courseEntries.get(0).course;
 
                JPanel courseBlock = new JPanel(new BorderLayout());
                courseBlock.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                courseBlock.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
                courseBlock.setPreferredSize(new Dimension(480, 100));
 
                JLabel courseLabel = new JLabel(String.format("%s (%s) - %d students registered",
                        course.name, course.code, course.getRegisteredStudents()));
                courseLabel.setFont(new Font("Arial", Font.BOLD, 14));
                courseLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));
 
                JButton expandButton = new JButton("Show Sections");
 
                expandButton.addActionListener(e -> {
                    JDialog sectionDialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(availableSectionsPanel),
                            course.name + " (" + course.code + ") Sections", true);
                    sectionDialog.setLayout(new BorderLayout());
 
                    JPanel sectionContainer = new JPanel();
                    sectionContainer.setLayout(new BoxLayout(sectionContainer, BoxLayout.Y_AXIS));
                    sectionContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
 
                    Map<String, List<ScheduleEntry>> lectureSections = courseEntries.stream()
                            .filter(entry -> entry.sectionType.equals("Lecture"))
                            .collect(Collectors.groupingBy(entry -> entry.section + "-" + entry.teacher.getName()));
 
                    Map<String, List<ScheduleEntry>> labSections = courseEntries.stream()
                            .filter(entry -> entry.sectionType.equals("Lab"))
                            .collect(Collectors.groupingBy(entry -> "Lab-" + entry.section));
 
                    JLabel lectureHeader = new JLabel("Lecture Sections");
                    lectureHeader.setFont(new Font("Arial", Font.BOLD, 14));
                    sectionContainer.add(lectureHeader);
                    sectionContainer.add(Box.createVerticalStrut(10));
 
                    for (List<ScheduleEntry> sectionEntries : lectureSections.values()) {
                        addSectionPanel(sectionContainer, sectionEntries, sectionDialog);
                    }
 
                    JLabel labHeader = new JLabel("Lab Sections");
                    labHeader.setFont(new Font("Arial", Font.BOLD, 14));
                    sectionContainer.add(Box.createVerticalStrut(20));
                    sectionContainer.add(labHeader);
                    sectionContainer.add(Box.createVerticalStrut(10));
 
                    for (List<ScheduleEntry> sectionEntries : labSections.values()) {
                        addSectionPanel(sectionContainer, sectionEntries, sectionDialog);
                    }
 
                    JScrollPane scrollPane = new JScrollPane(sectionContainer);
                    scrollPane.setPreferredSize(new Dimension(500, 400));
                    sectionDialog.add(scrollPane, BorderLayout.CENTER);
                    sectionDialog.pack();
                    sectionDialog.setLocationRelativeTo(availableSectionsPanel);
                    sectionDialog.setVisible(true);
                });
 
                courseBlock.add(courseLabel, BorderLayout.WEST);
                courseBlock.add(expandButton, BorderLayout.EAST);
                availableSectionsPanel.add(courseBlock);
            }
 
            availableSectionsPanel.revalidate();
            availableSectionsPanel.repaint();
        } finally {
            isUpdatingSections = false;
        }
    }
 
    private void forceUpdateAvailableSections() {
        lastCourseCodes.clear();
        updateAvailableSections();
    }
 
    private void addSectionPanel(JPanel container, List<ScheduleEntry> sectionEntries, JDialog dialog) {
        ScheduleEntry firstEntry = sectionEntries.get(0);
        JPanel sectionPanel = new JPanel(new BorderLayout());
        sectionPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
 
        StringBuilder sectionInfo = new StringBuilder("<html>");
        if (firstEntry.sectionType.equals("Lecture")) {
            sectionInfo.append("<b>Section ").append(firstEntry.section)
                    .append("</b> - ").append(firstEntry.teacher.getName()).append("<br>");
        } else {
            sectionInfo.append("<b>Lab Section ").append(firstEntry.section)
                    .append("</b> - ").append(firstEntry.teacher.getName()).append("<br>");
        }
 
        for (ScheduleEntry entry : sectionEntries) {
            sectionInfo.append("Room: ").append(entry.room.id)
                    .append(" - ").append(entry.slot.day)
                    .append(" ").append(entry.slot.hour).append(":00<br>");
        }
        sectionInfo.append("</html>");
 
        JLabel infoLabel = new JLabel(sectionInfo.toString());
        JToggleButton toggleButton = new JToggleButton("Add");
 
        boolean isSelected = student.selectedCourses.stream()
                .anyMatch(sc -> sc.course.code.equals(firstEntry.course.code) &&
                        sc.section.equals(firstEntry.section));
 
        boolean courseAlreadySelected = student.selectedCourses.stream()
                .anyMatch(sc -> sc.course.code.equals(firstEntry.course.code));
 
        if (isSelected) {
            toggleButton.setSelected(true);
            toggleButton.setText("Remove");
        } else if (courseAlreadySelected) {
            toggleButton.setEnabled(false);
            toggleButton.setText("Course Already Selected");
        }
 
        toggleButton.addActionListener(e -> {
            if (toggleButton.isSelected()) {
                if (!student.hasCourseConflict(firstEntry)) {
                    student.selectedCourses.addAll(sectionEntries);
                    updateSelectedCourses();
                    firstEntry.course.incrementRegisteredStudents(student); // Pass student
                    timetableGrid.updateTimetable();
                    toggleButton.setText("Remove");
                    forceUpdateAvailableSections();
                } else {
                    toggleButton.setSelected(false);
                    JOptionPane.showMessageDialog(dialog,
                            "Time slot conflict detected. Cannot add this section.",
                            "Conflict Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                student.selectedCourses.removeIf(sc -> sc.course.code.equals(firstEntry.course.code) &&
                        sc.section.equals(firstEntry.section));
                firstEntry.course.decrementRegisteredStudents(student); // Pass student
                updateSelectedCourses();
                timetableGrid.updateTimetable();
                toggleButton.setText("Add");
                forceUpdateAvailableSections();
            }
        });
 
        sectionPanel.add(infoLabel, BorderLayout.CENTER);
        sectionPanel.add(toggleButton, BorderLayout.EAST);
        container.add(Box.createVerticalStrut(10));
        container.add(sectionPanel);
    }
 
    private void updateSelectedCourses() {
        selectedCoursesPanel.removeAll();
 
        if (student.selectedCourses.isEmpty()) {
            selectedCoursesPanel.add(new JLabel("No courses selected yet"));
        } else {
            Map<String, List<ScheduleEntry>> groupedCourses = student.selectedCourses.stream()
                    .collect(Collectors.groupingBy(e -> e.course.code + "-" + e.section));
 
            for (List<ScheduleEntry> entries : groupedCourses.values()) {
                ScheduleEntry firstEntry = entries.get(0);
                JPanel coursePanel = new JPanel(new BorderLayout());
 
                StringBuilder courseInfo = new StringBuilder("<html>");
                courseInfo.append(firstEntry.course.name)
                        .append(" (").append(firstEntry.course.code).append(")")
                        .append(" - Section ").append(firstEntry.section)
                        .append(" - ").append(firstEntry.teacher.getName())
                        .append("<br>");
 
                for (ScheduleEntry entry : entries) {
                    courseInfo.append(String.format("Room: %s - %s %d:00<br>",
                            entry.room.id, entry.slot.day, entry.slot.hour));
                }
                courseInfo.append("</html>");
 
                JLabel courseLabel = new JLabel(courseInfo.toString());
                JButton removeButton = new JButton("Remove");
                removeButton.addActionListener(e -> removeCourseFromStudentSchedule(firstEntry));
 
                coursePanel.add(courseLabel, BorderLayout.CENTER);
                coursePanel.add(removeButton, BorderLayout.EAST);
                selectedCoursesPanel.add(coursePanel);
            }
        }
 
        selectedCoursesPanel.revalidate();
        selectedCoursesPanel.repaint();
        timetableGrid.updateTimetable();
    }
 
    private void removeCourseFromStudentSchedule(ScheduleEntry entry) {
        student.selectedCourses.removeIf(e -> e.course.code.equals(entry.course.code) &&
                e.section.equals(entry.section));
        entry.course.decrementRegisteredStudents(student);
        updateSelectedCourses();
        if (timetableGrid != null) {
            timetableGrid.updateTimetable();
        }
        forceUpdateAvailableSections(); // Update student count immediately
    }
 
    private class StudentTimetableGrid extends TimetableGrid {
        public StudentTimetableGrid() {
            super();
            updateTimetable();
        }
 
        @Override
        public void updateTimetable() {
            for (int hour = 0; hour < HOURS; hour++) {
                for (int day = 0; day < DAYS.length; day++) {
                    buttons[hour][day].setText("Free");
                    buttons[hour][day].setBackground(null);
                    buttons[hour][day].setOpaque(false);
                }
            }
 
            for (ScheduleEntry entry : student.selectedCourses) {
                int dayIndex = getDayIndex(entry.slot.day);
                int hourIndex = entry.slot.hour - 8;
 
                if (dayIndex >= 0 && hourIndex >= 0 && hourIndex < HOURS) {
                    JButton button = buttons[hourIndex][dayIndex];
                    String htmlText = String.format("<html><center>%s<br>Sec: %s<br>Room: %s</center></html>",
                            entry.course.code,
                            entry.section,
                            entry.room.id);
 
                    button.setText(htmlText);
                    button.setBackground(new Color(144, 238, 144));
                    button.setOpaque(true);
                    button.setToolTipText(entry.toString());
                }
            }
 
            revalidate();
            repaint();
        }
    }
}
class SectionManagementWindow extends JFrame {
    private Course course;
    private Admin admin;
    private JPanel sectionsPanel;
    private TimetableAutoSuggestion autoSuggestion;
    private JButton nextSuggestionBtn;
    private JButton prevSuggestionBtn;
    private JLabel currentSuggestionLabel;
    private JPanel suggestionPanel;
    private JPanel mainPanel;
    private JButton saveSuggestionBtn;
    private List<ScheduleEntry> currentSuggestion;
 
    public SectionManagementWindow(Course course, Admin admin) {
        this.course = course;
        this.admin = admin;
        setTitle("Manage Sections - " + course.name);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
 
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
 
        JPanel topPanel = new JPanel(new BorderLayout());
 
        JPanel addSectionPanel = createAddSectionPanel();
        topPanel.add(addSectionPanel, BorderLayout.NORTH);
 
        JPanel suggestionsPanel = new JPanel(new BorderLayout());
        suggestionsPanel.setBorder(BorderFactory.createTitledBorder("Schedule Suggestions"));
 
        initializeAutoSuggestion();
        suggestionsPanel.add(suggestionPanel, BorderLayout.CENTER);
 
        mainPanel.add(topPanel, BorderLayout.CENTER);
        mainPanel.add(suggestionsPanel, BorderLayout.SOUTH);
 
        mainPanel.setBackground(Color.WHITE);
        topPanel.setBackground(Color.WHITE);
        suggestionsPanel.setBackground(Color.WHITE);
 
        add(mainPanel);
        setVisible(true);
    }
 
    private JPanel createAddSectionPanel() {
        JPanel addSectionPanel = new JPanel(new GridBagLayout());
        addSectionPanel.setBorder(BorderFactory.createTitledBorder("Add New Section"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
 
        gbc.gridx = 0;
        gbc.gridy = 0;
        addSectionPanel.add(new JLabel("Select Teacher:"), gbc);
        JComboBox<Teacher> teacherBox = new JComboBox<>();
        for (Teacher t : Admin.teachers) {
            teacherBox.addItem(t);
        }
        gbc.gridx = 1;
        addSectionPanel.add(teacherBox, gbc);
 
        gbc.gridx = 0;
        gbc.gridy = 1;
        addSectionPanel.add(new JLabel("Select Room:"), gbc);
        JComboBox<Classroom> roomBox = new JComboBox<>();
        for (Classroom r : Admin.classrooms) {
            roomBox.addItem(r);
        }
        gbc.gridx = 1;
        addSectionPanel.add(roomBox, gbc);
 
        JButton addSectionBtn = new JButton("Add Section Manually");
        addSectionBtn.addActionListener(e -> {
            Teacher selectedTeacher = (Teacher) teacherBox.getSelectedItem();
            Classroom selectedRoom = (Classroom) roomBox.getSelectedItem();
 
            if (selectedTeacher != null && selectedRoom != null) {
                openDayPatternDialog(selectedTeacher, selectedRoom, 0);
            }
        });
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        addSectionPanel.add(addSectionBtn, gbc);
 
        return addSectionPanel;
    }
 
    private void applySuggestion() {
        if (currentSuggestion != null && !currentSuggestion.isEmpty()) {
            try {
                int sectionNumber = getNextSectionNumber(course, currentSuggestion.get(0).teacher,
                        currentSuggestion.get(0).sectionType);
 
                for (ScheduleEntry suggestion : currentSuggestion) {
                    ScheduleEntry entry = new ScheduleEntry(
                            suggestion.course,
                            String.valueOf(sectionNumber),
                            suggestion.teacher,
                            suggestion.room,
                            suggestion.slot,
                            suggestion.sectionType);
 
                    admin.assignSchedule(entry);
                }
 
                // Assign course to teacher
                admin.assignCourseToTeacher(course, currentSuggestion.get(0).teacher);
 
                JOptionPane.showMessageDialog(this,
                        "Section " + sectionNumber + " created successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
 
                dispose();
 
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error creating section: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
 
    private void initializeAutoSuggestion() {
        autoSuggestion = new TimetableAutoSuggestion(course);
 
        suggestionPanel = new JPanel(new BorderLayout(5, 5));
 
        JPanel displayPanel = new JPanel();
        displayPanel.setLayout(new BoxLayout(displayPanel, BoxLayout.Y_AXIS));
        displayPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
 
        currentSuggestionLabel = new JLabel("No suggestion available");
        currentSuggestionLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        currentSuggestionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        displayPanel.add(currentSuggestionLabel);
 
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
 
        JButton prevButton = new JButton("← Previous");
        JButton nextButton = new JButton("Next →");
        JButton randomButton = new JButton("Random");
        JButton applyButton = new JButton("Apply Suggestion");
 
        for (JButton button : new JButton[]{prevButton, nextButton, randomButton, applyButton}) {
            button.setFont(new Font("Arial", Font.PLAIN, 12));
            button.setPreferredSize(new Dimension(120, 30));
        }
        applyButton.setBackground(new Color(100, 180, 100));
        applyButton.setForeground(Color.BLACK);
 
        prevButton.addActionListener(e -> showSuggestion(autoSuggestion.getPreviousSuggestion()));
        nextButton.addActionListener(e -> showSuggestion(autoSuggestion.getNextSuggestion()));
        randomButton.addActionListener(e -> showSuggestion(autoSuggestion.getRandomSuggestion()));
        applyButton.addActionListener(e -> applySuggestion());
 
        buttonPanel.add(prevButton);
        buttonPanel.add(randomButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(applyButton);
 
        suggestionPanel.add(displayPanel, BorderLayout.CENTER);
        suggestionPanel.add(buttonPanel, BorderLayout.SOUTH);
 
        showSuggestion(autoSuggestion.getRandomSuggestion());
    }
 
    private void openDayPatternDialog(Teacher teacher, Classroom room, int sectionNum) {
        JDialog dialog = new JDialog(this, "Select Days Pattern", true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);
 
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
 
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Section Type:"), gbc);
        JComboBox<String> typeBox = new JComboBox<>(new String[] { "Lecture", "Lab" });
        gbc.gridx = 1;
        panel.add(typeBox, gbc);
 
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Start Time:"), gbc);
        String[] timeOptions = new String[10];
        for (int i = 0; i < 10; i++) {
            timeOptions[i] = (i + 8) + ":00";
        }
        JComboBox<String> timeBox = new JComboBox<>(timeOptions);
        gbc.gridx = 1;
        panel.add(timeBox, gbc);
 
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Days Pattern:"), gbc);
        JComboBox<String> patternBox = new JComboBox<>(new String[] { "M W F", "T Th F", "Custom" });
        gbc.gridx = 1;
        panel.add(patternBox, gbc);
 
        JPanel customDaysPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox[] dayBoxes = new JCheckBox[5];
        String[] days = { "M", "T", "W", "Th", "F" };
        for (int i = 0; i < days.length; i++) {
            dayBoxes[i] = new JCheckBox(days[i]);
            customDaysPanel.add(dayBoxes[i]);
        }
        customDaysPanel.setVisible(false);
 
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(customDaysPanel, gbc);
 
        patternBox.addActionListener(e -> {
            customDaysPanel.setVisible(patternBox.getSelectedItem().equals("Custom"));
            dialog.pack();
        });
 
        JButton createButton = new JButton("Create Section");
        createButton.addActionListener(e -> {
            try {
                String selectedType = (String) typeBox.getSelectedItem();
                long currentSections = Admin.masterSchedule.stream()
                        .filter(entry -> entry.course.equals(course) && entry.sectionType.equals(selectedType))
                        .map(entry -> entry.section)
                        .distinct()
                        .count();
                if (selectedType.equals("Lecture") && currentSections >= course.maxLectureSections) {
                    throw new IllegalStateException("Maximum number of lecture sections reached (" + course.maxLectureSections + ")");
                } else if (selectedType.equals("Lab") && currentSections >= course.maxLabSections) {
                    throw new IllegalStateException("Maximum number of lab sections reached (" + course.maxLabSections + ")");
                }
 
                int selectedTime = Integer.parseInt(((String) timeBox.getSelectedItem()).split(":")[0]);
                List<String> selectedDays = new ArrayList<>();
                if (patternBox.getSelectedItem().equals("Custom")) {
                    for (int i = 0; i < days.length; i++) {
                        if (dayBoxes[i].isSelected()) {
                            selectedDays.add(days[i]);
                        }
                    }
                } else {
                    selectedDays = Arrays.asList(((String) patternBox.getSelectedItem()).split(" "));
                }
 
                if (selectedDays.isEmpty()) {
                    throw new IllegalStateException("Please select at least one day.");
                }
 
                int sectionNumber = getNextSectionNumber(course, teacher, selectedType);
 
                for (String day : selectedDays) {
                    TimeSlot timeSlot = new TimeSlot(day, selectedTime);
                    ScheduleEntry entry = new ScheduleEntry(
                            course,
                            String.valueOf(sectionNumber),
                            teacher,
                            room,
                            timeSlot,
                            selectedType);
 
                    // Validate lab schedule
                    if (!BITSTimeTablePolicy.validateLabSchedule(entry, Admin.masterSchedule)) {
                        throw new IllegalStateException("Lab sections must have at least one day gap between them.");
                    }
 
                    admin.assignSchedule(entry);
                }
 
                // Assign course to teacher
                admin.assignCourseToTeacher(course, teacher);
 
                JOptionPane.showMessageDialog(dialog,
                        "Section " + sectionNumber + " created successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                dispose();
 
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
 
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(createButton, gbc);
 
        dialog.add(panel);
        dialog.pack();
        dialog.setVisible(true);
    }
 
    private void showSuggestion(List<ScheduleEntry> suggestion) {
        if (suggestion != null && !suggestion.isEmpty()) {
            currentSuggestion = suggestion;
            ScheduleEntry firstEntry = suggestion.get(0);
 
            StringBuilder text = new StringBuilder("<html>");
            text.append("<div style='text-align: center;'>");
            text.append(String.format("<h2>%s</h2>", firstEntry.course.code));
            text.append(String.format("<b>Teacher:</b> %s<br>", firstEntry.teacher.getName()));
            text.append(String.format("<b>Room:</b> %s<br><br>", firstEntry.room.id));
            text.append("<b>Schedule:</b><br>");
 
            for (ScheduleEntry entry : suggestion) {
                text.append(String.format("• %s %d:00<br>", entry.slot.day, entry.slot.hour));
            }
            text.append("</div></html>");
 
            currentSuggestionLabel.setText(text.toString());
        } else {
            currentSuggestionLabel.setText("<html><center>No available suggestions</center></html>");
        }
    }
 
    private int getNextSectionNumber(Course course, Teacher teacher) {
        return getNextSectionNumber(course, teacher, "Lecture");
    }
 
    private int getNextSectionNumber(Course course, Teacher teacher, String sectionType) {
        if (sectionType.equals("Lecture")) {
            Optional<String> existingSection = Admin.masterSchedule.stream()
                    .filter(entry -> entry.course.equals(course) &&
                            entry.teacher.equals(teacher) &&
                            entry.sectionType.equals("Lecture"))
                    .map(entry -> entry.section)
                    .findFirst();
 
            if (existingSection.isPresent()) {
                return Integer.parseInt(existingSection.get());
            }
 
            return Admin.masterSchedule.stream()
                    .filter(entry -> entry.course.equals(course) &&
                            entry.sectionType.equals("Lecture"))
                    .mapToInt(entry -> Integer.parseInt(entry.section))
                    .max()
                    .orElse(0) + 1;
        } else {
            return Admin.masterSchedule.stream()
                    .filter(entry -> entry.course.equals(course) &&
                            entry.sectionType.equals("Lab"))
                    .mapToInt(entry -> Integer.parseInt(entry.section))
                    .max()
                    .orElse(0) + 1;
        }
    }
}
class SectionEditDialog extends JDialog {
    private ScheduleEntry firstEntry;
    private List<ScheduleEntry> allEntries;
    private Admin admin;
 
    public SectionEditDialog(ScheduleEntry entry, Admin admin) {
        super((Frame) null, "Edit Section", true);
        this.firstEntry = entry;
        this.admin = admin;
 
        this.allEntries = Admin.masterSchedule.stream()
            .filter(e -> e.course.equals(entry.course)
                && e.teacher.equals(entry.teacher)
                && e.section.equals(entry.section))
            .collect(Collectors.toList());
 
        setSize(500, 400);
        setLocationRelativeTo(null);
 
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
 
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
 
        // Teacher info (non-editable)
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel("Teacher:"), gbc);
        JLabel teacherLabel = new JLabel(entry.teacher.getName());
        gbc.gridx = 1;
        mainPanel.add(teacherLabel, gbc);
 
        // Section type selection
        gbc.gridx = 0;
        gbc.gridy = 1;
        mainPanel.add(new JLabel("Section Type:"), gbc);
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Lecture", "Lab"});
        typeBox.setSelectedItem(entry.sectionType); // Set the current section type
        gbc.gridx = 1;
        mainPanel.add(typeBox, gbc);
 
        // Time selection
        gbc.gridx = 0;
        gbc.gridy = 2;
        mainPanel.add(new JLabel("Start Time:"), gbc);
        String[] timeOptions = new String[10];
        for (int i = 0; i < 10; i++) {
            timeOptions[i] = (i + 8) + ":00";
        }
        JComboBox<String> timeBox = new JComboBox<>(timeOptions);
        timeBox.setSelectedItem(entry.slot.hour + ":00");
        gbc.gridx = 1;
        mainPanel.add(timeBox, gbc);
 
        // Days pattern selection
        gbc.gridx = 0;
        gbc.gridy = 3;
        mainPanel.add(new JLabel("Days Pattern:"), gbc);
        JComboBox<String> patternBox = new JComboBox<>(new String[]{"M W F", "T Th F", "Custom"});
        gbc.gridx = 1;
        mainPanel.add(patternBox, gbc);
 
        // Custom days panel
        JPanel customDaysPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox[] dayBoxes = new JCheckBox[5];
        String[] days = { "M", "T", "W", "Th", "F" };
 
        Set<String> currentDays = allEntries.stream()
        .map(scheduleEntry -> scheduleEntry.slot.day)
        .collect(Collectors.toSet());
 
        for (int i = 0; i < days.length; i++) {
            dayBoxes[i] = new JCheckBox(days[i]);
            dayBoxes[i].setSelected(currentDays.contains(days[i]));
            customDaysPanel.add(dayBoxes[i]);
        }
 
        // Determine if current pattern matches any predefined pattern
        String currentPattern = String.join(" ", currentDays);
        if (currentPattern.equals("M W F")) {
            patternBox.setSelectedItem("M W F");
            customDaysPanel.setVisible(false);
        } else if (currentPattern.equals("T Th F")) {
            patternBox.setSelectedItem("T Th F");
            customDaysPanel.setVisible(false);
        } else {
            patternBox.setSelectedItem("Custom");
            customDaysPanel.setVisible(true);
        }
 
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        mainPanel.add(customDaysPanel, gbc);
 
        patternBox.addActionListener(e -> {
            String selectedPattern = (String) patternBox.getSelectedItem();
            if (selectedPattern.equals("Custom")) {
                customDaysPanel.setVisible(true);
            } else {
                customDaysPanel.setVisible(false);
                // Update checkbox selections based on pattern
                for (int i = 0; i < days.length; i++) {
                    if (selectedPattern.equals("M W F")) {
                        dayBoxes[i].setSelected(days[i].equals("M") || days[i].equals("W") || days[i].equals("F"));
                    } else if (selectedPattern.equals("T Th F")) {
                        dayBoxes[i].setSelected(days[i].equals("T") || days[i].equals("Th") || days[i].equals("F"));
                    }
                }
            }
            pack();
        });
 
        // Room selection
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        mainPanel.add(new JLabel("Room:"), gbc);
        JComboBox<Classroom> roomBox = new JComboBox<>(Admin.classrooms.toArray(new Classroom[0]));
        roomBox.setSelectedItem(entry.room);
        gbc.gridx = 1;
        mainPanel.add(roomBox, gbc);
 
        // Save button
        JButton saveButton = new JButton("Save Changes");
        saveButton.addActionListener(actionEvent -> {
    try {
        // Remove existing entries
        Admin.masterSchedule.removeAll(allEntries);
        firstEntry.teacher.schedule.removeAll(allEntries);
 
        String selectedType = (String) typeBox.getSelectedItem();
        int selectedTime = Integer.parseInt(((String)timeBox.getSelectedItem()).split(":")[0]);
 
        // Get selected days
        List<String> selectedDays = new ArrayList<>();
        if (patternBox.getSelectedItem().equals("Custom")) {
            for (int i = 0; i < days.length; i++) {
                if (dayBoxes[i].isSelected()) {
                    selectedDays.add(days[i]);
                }
            }
        } else {
            selectedDays = Arrays.asList(((String)patternBox.getSelectedItem()).split(" "));
        }
 
        Classroom selectedRoom = (Classroom)roomBox.getSelectedItem();
 
        // Create new entries
        for (String day : selectedDays) {
            TimeSlot timeSlot = new TimeSlot(day, selectedTime);
 
            // Check for conflicts with explicit type declaration
            boolean hasConflict = Admin.masterSchedule.stream()
                .filter((ScheduleEntry existingEntry) -> !allEntries.contains(existingEntry))
                .anyMatch((ScheduleEntry existingEntry) ->
                    existingEntry.teacher.equals(firstEntry.teacher) &&
                    existingEntry.slot.equals(timeSlot));
 
            if (hasConflict) {
                throw new IllegalStateException(
                    "Time conflict detected for " + day + " at " + selectedTime + ":00");
            }
 
            ScheduleEntry newEntry = new ScheduleEntry(
                firstEntry.course,
                firstEntry.section,
                firstEntry.teacher,
                selectedRoom,
                timeSlot,
                selectedType
            );
            admin.assignSchedule(newEntry);
        }
 
        JOptionPane.showMessageDialog(this,
            "Section updated successfully!");
        dispose();
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this,
            ex.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }
});
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(saveButton, gbc);
 
        add(mainPanel);
        pack();
        setVisible(true);
    }
 
}
 
 
 
 
class TimetableExporter {
    public static void exportAsImage(JPanel timetablePanel) {
        try {
            BufferedImage image = new BufferedImage(
                timetablePanel.getWidth(),
                timetablePanel.getHeight(),
                BufferedImage.TYPE_INT_RGB
            );
 
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
            timetablePanel.paint(g2d);
            g2d.dispose();
 
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter(
                "PNG files", "png"));
 
            if (fileChooser.showSaveDialog(timetablePanel) == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                if (!filePath.toLowerCase().endsWith(".png")) {
                    filePath += ".png";
                }
 
                ImageIO.write(image, "png", new File(filePath));
 
                JOptionPane.showMessageDialog(null,
                    "Timetable saved successfully!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Error saving timetable: " + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
 
class TimetableBuildFinal {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginFrame();
        });
    }
}
