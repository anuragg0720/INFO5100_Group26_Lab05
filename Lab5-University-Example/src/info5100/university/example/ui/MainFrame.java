package info5100.university.example.ui;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;
import info5100.university.example.College.*;
import info5100.university.example.Department.*;
import info5100.university.example.Degree.*;
import info5100.university.example.CourseCatalog.*;
import info5100.university.example.CourseSchedule.*;
import info5100.university.example.Persona.*;

public class MainFrame extends JFrame {
    private final CardLayout card = new CardLayout();
    private final JPanel content = new JPanel(card);

    // Data
    private College college;
    private Department dept;
    private Degree isDegree;
    private CourseSchedule fall25;
    private StudentProfile alice, bob;
    private Course info5001, info5100, dbms;

    public MainFrame(){
        super("University Model — Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        seed();

        content.add(buildGpaPanel(), "gpa");
        content.add(buildDegreePanel(), "degree");
        content.add(buildFindOfferPanel(), "find");
        content.add(buildGradePanel(), "grade");
        content.add(buildEnrollCountPanel(), "count");

        card.show(content, "gpa");
        setSize(900, 560);
        setLocationRelativeTo(null);
    }

    private JPanel buildSidebar(){
        JPanel side = new JPanel();
        side.setLayout(new GridLayout(0,1,8,8));
        side.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        side.add(makeNavButton("1. GPA", "gpa"));
        side.add(makeNavButton("2. Degree Courses", "degree"));
        side.add(makeNavButton("3. Find Offer", "find"));
        side.add(makeNavButton("4. Assign Grade", "grade"));
        side.add(makeNavButton("5. Enrollment Count", "count"));
        return side;
    }
    private JButton makeNavButton(String title, String cardName){
        JButton b = new JButton(title);
        b.addActionListener(e->card.show(content, cardName));
        return b;
    }

    private void seed(){
        college = new College("Northeastern University");
        dept    = new Department("Information Systems");
        college.addDepartment(dept);

        info5001 = new Course("INFO 5001","Application Design & Development",4);
        info5100 = new Course("INFO 5100","AED - Java",4);
        dbms     = new Course("INFO 6205","Databases",4);
        dept.getCatalog().add(info5001); dept.getCatalog().add(info5100); dept.getCatalog().add(dbms);

        isDegree = new Degree("Information Systems");
        isDegree.add(info5001); isDegree.add(info5100); isDegree.add(dbms);
        dept.addDegree(isDegree);

        fall25 = new CourseSchedule("Fall 2025");
        CourseOffer o1 = fall25.addOffer(info5001, "01", 3);
        CourseOffer o2 = fall25.addOffer(info5100, "01", 2);
        fall25.addOffer(dbms, "01", 2);

        alice = new StudentProfile(new Person("Alice"));
        bob   = new StudentProfile(new Person("Bob"));
        CourseLoad aliceCL = new CourseLoad("Fall 2025");
        CourseLoad bobCL   = new CourseLoad("Fall 2025");
        alice.getTranscript().addCourseLoad(aliceCL);
        bob.getTranscript().addCourseLoad(bobCL);

        SeatAssignment a1 = alice.register(o1, aliceCL);
        SeatAssignment a2 = alice.register(o2, aliceCL);
        SeatAssignment b1 = bob.register(o1, bobCL);

        // Map courses to seat assignments for GPA credit calc
        alice.getTranscript().mapCourse(a1, info5001);
        alice.getTranscript().mapCourse(a2, info5100);
        bob.getTranscript().mapCourse(b1, info5001);
    }

    private JPanel buildGpaPanel(){
        JPanel p = panel("Compute GPA");
        JComboBox<String> sem = new JComboBox<>(new String[]{"Fall 2025"});
        JTextArea out = area();
        JButton calc = new JButton("Compute Alice GPA");
        calc.addActionListener(e->{
            double gpa = alice.getTranscript().computeSemesterGPA((String)sem.getSelectedItem());
            out.setText(String.format("Alice GPA for %s: %.2f", sem.getSelectedItem(), gpa));
        });
        p.add(row(new JLabel("Semester:"), sem, calc));
        p.add(scroll(out));
        return p;
    }

    private JPanel buildDegreePanel(){
        JPanel p = panel("List Degree Courses");
        JComboBox<String> degreeName = new JComboBox<>(new String[]{isDegree.getName()});
        JTextArea out = area();
        JButton list = new JButton("List Courses");
        list.addActionListener(e->{
            String courses = isDegree.list().stream().map(Object::toString).collect(Collectors.joining("\n"));
            out.setText("Courses for "+degreeName.getSelectedItem()+":\n"+courses);
        });
        p.add(row(new JLabel("Degree:"), degreeName, list));
        p.add(scroll(out));
        return p;
    }

    private JPanel buildFindOfferPanel(){
        JPanel p = panel("Find Course Offer");
        JTextField courseNum = new JTextField("INFO 5001", 12);
        JTextArea out = area();
        JButton find = new JButton("Find in Fall 2025");
        find.addActionListener(e->{
            String num = courseNum.getText().trim();
            String msg = fall25.findOfferByCourseNumber(num)
                .map(o->"Found Offer: "+o+" (capacity "+o.capacity()+", enrolled "+o.enrolledCount()+")")
                .orElse("Offer not found in "+fall25.getSemester());
            out.setText(msg);
        });
        p.add(row(new JLabel("Course #:"), courseNum, find));
        p.add(scroll(out));
        return p;
    }

    private JPanel buildGradePanel(){
        JPanel p = panel("Assign Grade");
        JComboBox<String> courseNum = new JComboBox<>(new String[]{"INFO 5001","INFO 5100","INFO 6205"});
        JComboBox<StudentProfile> student = new JComboBox<>(new StudentProfile[]{alice,bob});
        JTextField grade = new JTextField("A",4);
        JTextArea out = area();
        JButton assign = new JButton("Assign to Student");
        assign.addActionListener(e->{
            String num = (String)courseNum.getSelectedItem();
            StudentProfile sp = (StudentProfile)student.getSelectedItem();
            fall25.findOfferByCourseNumber(num).ifPresent(o->{
                o.getSeats().stream().map(Seat::getAssignment).filter(sa-> sa!=null && sa.getStudent()==sp).findFirst()
                    .ifPresent(sa-> sa.setLetterGrade(grade.getText().trim()));
            });
            out.setText("Assigned grade "+grade.getText()+" for "+num+" to "+sp+" (if enrolled).");
        });
        p.add(row(new JLabel("Course #:"), courseNum, new JLabel("Student:"), student, new JLabel("Grade:"), grade, assign));
        p.add(scroll(out));
        return p;
    }

    private JPanel buildEnrollCountPanel(){
        JPanel p = panel("Enrollment Count");
        JComboBox<String> courseNum = new JComboBox<>(new String[]{"INFO 5001","INFO 5100","INFO 6205"});
        JTextArea out = area();
        JButton count = new JButton("Count Enrolled (Fall 2025)");
        count.addActionListener(e->{
            String num = (String)courseNum.getSelectedItem();
            String msg = fall25.findOfferByCourseNumber(num)
                .map(o->"Students enrolled in "+o+": "+o.enrolledCount())
                .orElse("Offer not found.");
            out.setText(msg);
        });
        p.add(row(new JLabel("Course #:"), courseNum, count));
        p.add(scroll(out));
        return p;
    }

    // ---- UI helpers ----
    private JPanel panel(String title){
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createTitledBorder(title));
        return p;
    }
    private JPanel row(java.awt.Component... comps){
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for(java.awt.Component c: comps) r.add(c);
        return r;
    }
    private JTextArea area(){ JTextArea a = new JTextArea(12,50); a.setEditable(false); return a; }
    private JScrollPane scroll(JTextArea a){ return new JScrollPane(a); }

    public static void main(String[] args){
        SwingUtilities.invokeLater(()-> new MainFrame().setVisible(true));
    }
}
