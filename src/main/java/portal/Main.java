package portal;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.*;
import java.util.*;
/** Swing + JDBC entry point for the Online Job Portal. */
public class Main {
    private static final Color NAVY = new Color(23, 42, 77);
    private static final Color ACCENT = new Color(40, 105, 230);
    private static final Color CANVAS = new Color(246, 248, 252);
    private static final Color INK = new Color(37, 48, 66);
    static final String URL = System.getenv().getOrDefault("JOBPORTAL_DB_URL", "jdbc:mysql://localhost:3306/job_portal?serverTimezone=UTC");
    // Local classroom setup: replace the fallback with your MySQL password. Environment variables override it.
    // Read each laptop's local MySQL credentials from environment variables; never share passwords in source.
    static final String DBUSER = System.getenv().getOrDefault("JOBPORTAL_DB_USER", "root"), DBPASS = System.getenv().getOrDefault("JOBPORTAL_DB_PASSWORD", "");
    static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, DBUSER, DBPASS);
    }

    static String hash(String p) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(p.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static void log(Integer uid, String type, String detail) {
        try (Connection c = connect(); PreparedStatement p = c.prepareStatement( "INSERT INTO activity_logs(user_id,activity_type,description) VALUES(?,?,?)")) {
            if (uid == null) p.setNull(1, Types.INTEGER);
            else p.setInt(1, uid);
            p.setString(2, type);
            p.setString(3, detail);
            p.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    static String esc(String s) {
        return s == null ? "" : s;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            applyTheme();
            login();
        });
    }

    /** Apply a clean, consistent theme using Java's built-in Nimbus look and feel. */
    static void applyTheme() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // Keep the platform look and feel if Nimbus is unavailable.
        }
        UIManager.put("control", CANVAS);
        UIManager.put("Panel.background", CANVAS);
        UIManager.put("TabbedPane.background", CANVAS);
        UIManager.put("TabbedPane.foreground", INK);
        UIManager.put("Label.foreground", INK);
        UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("PasswordField.font", new Font("Segoe UI", Font.PLAIN, 14));
        UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 13));
        UIManager.put("Table.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("TableHeader.font", new Font("Segoe UI", Font.BOLD, 12));
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
    }

    static JButton primaryButton(String label) {
        JButton button = new JButton(label);
        button.setBackground(ACCENT);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT), BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        return button;
    }

    static void login() {
        JFrame f = new JFrame("Online Job Portal");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.setSize(520, 510);
        f.setLocationRelativeTo(null);
        f.setResizable(false);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(CANVAS);
        JPanel banner = new JPanel();
        banner.setBackground(NAVY);
        banner.setLayout(new BoxLayout(banner, BoxLayout.Y_AXIS));
        banner.setBorder(BorderFactory.createEmptyBorder(28, 38, 28, 38));
        JLabel brand = new JLabel("CAREER CONNECT");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 12));
        brand.setForeground(new Color(151, 190, 255));
        JLabel heading = new JLabel("Find your next opportunity");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 25));
        heading.setForeground(Color.WHITE);
        JLabel intro = new JLabel("Sign in to continue to your job portal.");
        intro.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        intro.setForeground(new Color(215, 225, 242));
        banner.add(brand);
        banner.add(Box.createVerticalStrut(12));
        banner.add(heading);
        banner.add(Box.createVerticalStrut(6));
        banner.add(intro);
        JPanel p = new JPanel(new GridLayout(0, 1, 9, 9));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(232, 236, 243)),
            BorderFactory.createEmptyBorder(28, 38, 30, 38)));
        JTextField email = new JTextField();
        JPasswordField pass = new JPasswordField();
        email.setPreferredSize(new Dimension(0, 40));
        pass.setPreferredSize(new Dimension(0, 40));
        p.add(new JLabel("Email"));
        p.add(email);
        p.add(new JLabel("Password"));
        p.add(pass);
        JButton go = primaryButton("Sign in");
        JButton reg = new JButton("Create account");
        p.add(go);
        p.add(reg);
        root.add(banner, BorderLayout.NORTH);
        root.add(p, BorderLayout.CENTER);
        f.setContentPane(root);
        go.addActionListener(e -> {
            try (Connection c = connect(); PreparedStatement q = c.prepareStatement( "SELECT user_id,name,email,role,status FROM users WHERE email=? AND password_hash=? AND status='ACTIVE'")) {
                q.setString(1, email.getText().trim()); q.setString(2, hash(new String(pass.getPassword()))); try (ResultSet r = q.executeQuery()) {
                    if (!r.next()) throw new IllegalArgumentException("Invalid credentials or inactive account."); Account a = new Account(r.getInt(1), r.getString(2), r.getString(3), r.getString(4)); log(a.id, "LOGIN", "User logged in"); f.dispose(); dashboard(a);
                }
            } catch (Exception ex) {
                error(ex);
            }
        });
        reg.addActionListener(e -> register(f));
        f.setVisible(true);
    }

    record Account(int id, String name, String email, String role) {
    }

    static void register(JFrame parent) {
        JTextField name = new JTextField(), email = new JTextField(), company = new JTextField();
        JPasswordField pw = new JPasswordField();
        JComboBox<String> role = new JComboBox<>(new String[] {
            "JOB_SEEKER", "EMPLOYER"
        });
        Object[] fields = {
            "Name", name, "Email", email, "Password (minimum 8 characters)", pw, "Role", role, "Company name (employers)", company
        };
        if (JOptionPane.showConfirmDialog(parent, fields, "Register", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            String em = email.getText().trim(), password = new String(pw.getPassword());
            if (name.getText().isBlank() || !em.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") || password.length() < 8) throw new IllegalArgumentException("Enter a name, valid email, and password of at least 8 characters.");
            try (Connection c = connect()) {
                c.setAutoCommit(false);
                try (PreparedStatement q = c.prepareStatement( "INSERT INTO users(name,email,password_hash,role) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    q.setString(1, name.getText().trim());
                    q.setString(2, em);
                    q.setString(3, hash(password));
                    q.setString(4, (String) role.getSelectedItem());
                    q.executeUpdate();
                    try (ResultSet k = q.getGeneratedKeys()) {
                        k.next();
                        int id = k.getInt(1);
                        if (role.getSelectedItem().equals("EMPLOYER")) {
                            try (PreparedStatement x = c.prepareStatement( "INSERT INTO employers(user_id,company_name,company_email) VALUES(?,?,?)")) {
                                x.setInt(1, id);
                                x.setString(2, company.getText().isBlank() ? name.getText() : company.getText());
                                x.setString(3, em);
                                x.executeUpdate();
                            }
                        } else try (PreparedStatement x = c.prepareStatement("INSERT INTO job_seekers(user_id) VALUES(?)")) {
                            x.setInt(1, id);
                            x.executeUpdate();
                        }
                    }
                }
                c.commit();
            }
            log(null, "REGISTRATION", name.getText() + " registered as " + role.getSelectedItem());
            JOptionPane.showMessageDialog(parent, "Account created. You can now sign in.");
        } catch (Exception ex) {
            error(ex);
        }
    }

    static void dashboard(Account a) {
        String roleName = a.role.replace('_', ' ');
        JFrame f = new JFrame("Online Job Portal | " + roleName + " Dashboard");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.setMinimumSize(new Dimension(980, 640));
        f.setSize(1180, 760);
        f.setLocationRelativeTo(null);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        if (a.role.equals("ADMIN")) {
            tableTab(tabs, "Overview", "SELECT 'Users' metric,COUNT(*) total FROM users UNION ALL SELECT 'Jobs',COUNT(*) FROM jobs UNION ALL SELECT 'Applications',COUNT(*) FROM applications", a);
            tableTab(tabs, "Users", "SELECT user_id ID,name,email,role,status,created_at FROM users", a);
            tableTab(tabs, "Jobs", "SELECT j.job_id ID,j.title Job,e.company_name Employer,j.location,j.salary,j.status,j.created_at FROM jobs j JOIN employers e ON e.employer_id=j.employer_id", a);
            tableTab(tabs, "Applications", "SELECT a.application_id ID,j.title Job,u.name Candidate,a.status,a.applied_at FROM applications a JOIN jobs j USING(job_id) JOIN job_seekers s USING(seeker_id) JOIN users u ON s.user_id=u.user_id", a);
            tableTab(tabs, "Activity", "SELECT activity_time,activity_type,description FROM activity_logs ORDER BY activity_time DESC LIMIT 200", a);
            settingsTab(tabs, a);
        } else if (a.role.equals("EMPLOYER")) {
            tableTab(tabs, "My Jobs", "SELECT j.job_id ID,j.title,j.location,j.salary,j.status,j.created_at,COUNT(a.application_id) applications FROM jobs j JOIN employers e USING(employer_id) LEFT JOIN applications a USING(job_id) WHERE e.user_id=" + a.id + " GROUP BY j.job_id", a);
            tableTab(tabs, "Applications", "SELECT a.application_id ID,j.title Job,u.name Candidate,u.email,a.resume,a.cover_letter,a.status,a.applied_at FROM applications a JOIN jobs j USING(job_id) JOIN employers e USING(employer_id) JOIN job_seekers s USING(seeker_id) JOIN users u ON u.user_id=s.user_id WHERE e.user_id=" + a.id, a);
            tableTab(tabs, "Messages", "SELECT sent_at,sender_id,receiver_id,subject,message FROM messages WHERE sender_id=" + a.id + " OR receiver_id=" + a.id + " ORDER BY sent_at DESC", a);
            statsTab(tabs, "SELECT j.title,COUNT(a.application_id) applications FROM jobs j JOIN employers e USING(employer_id) LEFT JOIN applications a USING(job_id) WHERE e.user_id=" + a.id + " GROUP BY j.job_id", a);
        } else {
            tableTab(tabs, "Search jobs", "SELECT j.job_id ID,j.title,em.company_name Company,j.location,j.salary,j.job_type,j.experience,j.skills,j.created_at FROM jobs j JOIN employers em USING(employer_id) WHERE j.status='APPROVED' AND j.deadline>=CURDATE()", a);
            tableTab(tabs, "My applications", "SELECT a.application_id ID,j.title Job,e.company_name Company,a.applied_at,a.status FROM applications a JOIN jobs j USING(job_id) JOIN employers e USING(employer_id) JOIN job_seekers s USING(seeker_id) WHERE s.user_id=" + a.id, a);
            tableTab(tabs, "Profile", "SELECT u.name,u.email,s.phone,s.address,s.skills,s.education,s.experience,s.resume FROM users u JOIN job_seekers s USING(user_id) WHERE u.user_id=" + a.id, a);
            tableTab(tabs, "Messages", "SELECT sent_at,sender_id,receiver_id,subject,message FROM messages WHERE sender_id=" + a.id + " OR receiver_id=" + a.id + " ORDER BY sent_at DESC", a);
            tableTab(tabs, "Recommended", "SELECT j.job_id ID,j.title,j.location,j.skills FROM jobs j JOIN job_seekers s ON s.user_id=" + a.id + " WHERE j.status='APPROVED' AND j.deadline>=CURDATE() AND s.skills IS NOT NULL AND (LOWER(j.skills) LIKE CONCAT('%',LOWER(SUBSTRING_INDEX(s.skills,',',1)),'%') OR LOWER(j.requirements) LIKE CONCAT('%',LOWER(SUBSTRING_INDEX(s.skills,',',1)),'%'))", a);
        }
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(18, 26, 18, 26));
        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(roleName + " Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 23));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Welcome back, " + a.name + "  ·  Manage your portal activity");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(new Color(210, 221, 240));
        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitle);
        header.add(heading, BorderLayout.CENTER);
        JButton logout = new JButton("Log out");
        logout.setFocusPainted(false);
        logout.addActionListener(e -> {
            f.dispose(); login();
        });
        header.add(logout, BorderLayout.EAST);
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(CANVAS);
        content.add(tabs, BorderLayout.CENTER);
        f.add(header, BorderLayout.NORTH);
        f.add(content, BorderLayout.CENTER);
        f.setVisible(true);
    }

    static void tableTab(JTabbedPane tabs, String title, String sql, Account a) {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBackground(CANVAS);
        p.setBorder(BorderFactory.createEmptyBorder(16, 18, 14, 18));
        DefaultTableModel m = new DefaultTableModel();
        JTable t = new JTable(m);
        t.setRowHeight(30);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(new Color(230, 234, 241));
        t.setSelectionBackground(new Color(221, 234, 255));
        t.setSelectionForeground(INK);
        t.setFillsViewportHeight(true);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setPreferredSize(new Dimension(0, 36));
        JTextField search = new JTextField();
        JButton refresh = new JButton("Refresh"), add = primaryButton("Add / Apply / Update"), edit = new JButton("Change status / Edit"), del = new JButton("Delete"), message = new JButton("Send message");
        JPanel top = new JPanel(new BorderLayout());
        search.setPreferredSize(new Dimension(240, 36));
        search.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(218, 225, 236)), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        top.add(new JLabel("Search   "), BorderLayout.WEST);
        top.add(search, BorderLayout.CENTER);
        top.add(refresh, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(t);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(226, 231, 239)));
        scroll.getViewport().setBackground(Color.WHITE);
        p.add(scroll, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        actions.setBackground(CANVAS);
        actions.add(add);
        actions.add(edit);
        actions.add(del);
        actions.add(message);
        p.add(actions, BorderLayout.SOUTH);
        Runnable load = () -> {
            try (Connection c = connect(); PreparedStatement q = c.prepareStatement(sql)) {
                try (ResultSet r = q.executeQuery()) {
                    m.setRowCount(0);
                    m.setColumnCount(0);
                    ResultSetMetaData md = r.getMetaData();
                    for (int i = 1; i <= md.getColumnCount(); i++) m.addColumn(md.getColumnLabel(i));
                    while (r.next()) {
                        Object[] row = new Object[md.getColumnCount()];
                        for (int i = 0; i < row.length; i++) row[i] = r.getObject(i + 1);
                        m.addRow(row);
                    }
                }
            } catch (Exception ex) {
                error(ex);
            }
        };
        refresh.addActionListener(e -> load.run());
        search.addActionListener(e -> {
            String term = search.getText().trim().toLowerCase(); if (term.isEmpty()) {
                load.run(); return;
            }
            try {
                TableRowSorter<DefaultTableModel> s = new TableRowSorter<>(m); t.setRowSorter(s); s.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(term)));
            } catch (Exception ignored) {
            }
        });
        add.addActionListener(e -> addAction(title, a, load));
        edit.addActionListener(e -> editAction(title, a, t, load));
        del.addActionListener(e -> deleteAction(title, a, t, load));
        message.addActionListener(e -> sendMessage(a));
        if (!a.role.equals("ADMIN") && !title.equals("Search jobs") && !title.equals("My applications") && !title.equals("Applications") && !title.equals("My Jobs")) {
            del.setVisible(false);
        }
        load.run();
        tabs.addTab(title, p);
    }

    static Object value(JTable t, int col) {
        int row = t.getSelectedRow();
        if (row < 0) throw new IllegalArgumentException("Select a row first.");
        return t.getValueAt(t.convertRowIndexToModel(row), col);
    }

    static void addAction(String tab, Account a, Runnable load) {
        try (Connection c = connect()) {
            if (tab.equals("My Jobs")) {
                JTextField title = new JTextField(), loc = new JTextField(), salary = new JTextField(), type = new JTextField("Full-time"), exp = new JTextField(), skills = new JTextField(), deadline = new JTextField("2099-12-31");
                JTextArea desc = new JTextArea(4, 24);
                Object[] form = {
                    "Title", title, "Location", loc, "Salary", salary, "Type", type, "Experience", exp, "Skills (comma separated)", skills, "Deadline YYYY-MM-DD", deadline, "Description", new JScrollPane(desc)
                };
                if (JOptionPane.showConfirmDialog(null, form, "Post job", JOptionPane.OK_CANCEL_OPTION) != 0) return;
                try (PreparedStatement q = c.prepareStatement( "INSERT INTO jobs(employer_id,title,description,requirements,skills,salary,location,job_type,experience,deadline,status) SELECT employer_id,?,?,?,?,?,?,?,?,?,'PENDING' FROM employers WHERE user_id=?")) {
                    q.setString(1, title.getText());
                    q.setString(2, desc.getText());
                    q.setString(3, exp.getText());
                    q.setString(4, skills.getText());
                    q.setBigDecimal(5, new java.math.BigDecimal(salary.getText()));
                    q.setString(6, loc.getText());
                    q.setString(7, type.getText());
                    q.setString(8, exp.getText());
                    q.setDate(9, java.sql.Date.valueOf(deadline.getText()));
                    q.setInt(10, a.id);
                    q.executeUpdate();
                }
                log(a.id, "JOB_POSTED", title.getText());
                JOptionPane.showMessageDialog(null, "Job posted successfully and is awaiting admin approval.");
            } else if (tab.equals("Search jobs")) {
                String id = JOptionPane.showInputDialog("Job ID to apply to");
                if (id == null) return;
                String cover = JOptionPane.showInputDialog("Cover letter");
                try (PreparedStatement q = c.prepareStatement( "INSERT INTO applications(job_id,seeker_id,resume,cover_letter) SELECT j.job_id,s.seeker_id,s.resume,? FROM jobs j JOIN job_seekers s ON s.user_id=? WHERE j.job_id=? AND j.status='APPROVED' AND j.deadline>=CURDATE()")) {
                    q.setString(1, Objects.toString(cover, ""));
                    q.setInt(2, a.id);
                    q.setInt(3, Integer.parseInt(id));
                    if (q.executeUpdate() == 0) throw new IllegalArgumentException("Job unavailable; application may already exist.");
                }
                log(a.id, "APPLICATION_SUBMITTED", "Applied for job #" + id);
                JOptionPane.showMessageDialog(null, "Application submitted successfully.");
            } else if (tab.equals("Users") && a.role.equals("ADMIN")) {
                JTextField n = new JTextField(), em = new JTextField();
                JPasswordField pw = new JPasswordField();
                JComboBox<String> role = new JComboBox<>(new String[] {
                    "ADMIN", "EMPLOYER", "JOB_SEEKER"
                });
                Object[] f = {
                    "Name", n, "Email", em, "Password", pw, "Role", role
                };
                if (JOptionPane.showConfirmDialog(null, f, "Add user", 0) != 0) return;
                try (PreparedStatement q = c.prepareStatement("INSERT INTO users(name,email,password_hash,role) VALUES(?,?,?,?)")) {
                    q.setString(1, n.getText());
                    q.setString(2, em.getText());
                    q.setString(3, hash(new String(pw.getPassword())));
                    q.setString(4, (String) role.getSelectedItem());
                    q.executeUpdate();
                }
            } else if (tab.equals("Profile")) {
                String[] fields = {
                    "phone", "address", "skills", "education", "experience", "resume"
                };
                String[] prompts = {
                    "Phone", "Address", "Skills", "Education", "Experience", "Resume path"
                };
                String[] vals = new String[fields.length];
                for (int i = 0; i < fields.length; i++) {
                    vals[i] = JOptionPane.showInputDialog(prompts[i]);
                    if (vals[i] == null) return;
                }
                try (PreparedStatement q = c.prepareStatement( "UPDATE job_seekers SET phone=?,address=?,skills=?,education=?,experience=?,resume=? WHERE user_id=?")) {
                    for (int i = 0; i < 6; i++) q.setString(i + 1, vals[i]);
                    q.setInt(7, a.id);
                    q.executeUpdate();
                }
                JOptionPane.showMessageDialog(null, "Profile updated successfully.");
                log(a.id, "PROFILE_UPDATED", "Profile updated");
            }
        } catch (Exception ex) {
            error(ex);
        }
        load.run();
    }

    static void editAction(String tab, Account a, JTable t, Runnable load) {
        try {
            if (tab.equals("Applications")) {
                int id = ((Number) value(t, 0)).intValue();
                String[] opts = {
                    "APPLIED", "UNDER_REVIEW", "SHORTLISTED", "INTERVIEW", "SELECTED", "REJECTED"
                };
                String st = (String) JOptionPane.showInputDialog(null, "New status", "Update application", -1, null, opts, opts[0]);
                if (st == null) return;
                try (Connection c = connect(); PreparedStatement q = c.prepareStatement( "UPDATE applications x JOIN jobs j USING(job_id) JOIN employers e USING(employer_id) SET x.status=? WHERE x.application_id=? AND e.user_id=?")) {
                    q.setString(1, st);
                    q.setInt(2, id);
                    q.setInt(3, a.id);
                    if (q.executeUpdate() == 0) throw new IllegalArgumentException("Application not found for your jobs.");
                }
                log(a.id, "APPLICATION_STATUS", id + " changed to " + st);
            } else if (tab.equals("Jobs") && a.role.equals("ADMIN")) {
                int id = ((Number) value(t, 0)).intValue();
                String[] opts = {
                    "APPROVED", "REJECTED", "CLOSED"
                };
                String st = (String) JOptionPane.showInputDialog(null, "Job status", "Moderate posting", -1, null, opts, opts[0]);
                if (st == null) return;
                try (Connection c = connect(); PreparedStatement q = c.prepareStatement("UPDATE jobs SET status=? WHERE job_id=?")) {
                    q.setString(1, st);
                    q.setInt(2, id);
                    q.executeUpdate();
                }
                log(a.id, "JOB_" + st, "Job #" + id);
            } else if (tab.equals("My Jobs")) {
                int id = ((Number) value(t, 0)).intValue();
                String st = (String) JOptionPane.showInputDialog("Enter updated job status: PENDING, APPROVED, REJECTED, CLOSED");
                if (st == null) return;
                try (Connection c = connect(); PreparedStatement q = c.prepareStatement( "UPDATE jobs j JOIN employers e USING(employer_id) SET j.status=? WHERE j.job_id=? AND e.user_id=? AND ? IN ('CLOSED','PENDING')")) {
                    q.setString(1, st.toUpperCase());
                    q.setInt(2, id);
                    q.setInt(3, a.id);
                    q.setString(4, st.toUpperCase());
                    q.executeUpdate();
                }
            } else if (tab.equals("Users")) {
                int id = ((Number) value(t, 0)).intValue();
                String st = JOptionPane.showInputDialog("Set account status ACTIVE or SUSPENDED");
                if (st == null) return;
                try (Connection c = connect(); PreparedStatement q = c.prepareStatement("UPDATE users SET status=? WHERE user_id=?")) {
                    q.setString(1, st.toUpperCase());
                    q.setInt(2, id);
                    q.executeUpdate();
                }
            } else if (tab.equals("Profile")) {
                addAction(tab, a, load);
                return;
            }
            load.run();
        } catch (Exception ex) {
            error(ex);
        }
    }

    static void deleteAction(String tab, Account a, JTable t, Runnable load) {
        try {
            int id = ((Number) value(t, 0)).intValue();
            if (JOptionPane.showConfirmDialog(null, "Delete selected record #" + id + "?", "Confirm", JOptionPane.YES_NO_OPTION) != 0) return;
            String sql;
            if (tab.equals("Users") && a.role.equals("ADMIN")) sql = "DELETE FROM users WHERE user_id=?";
            else if (tab.equals("Jobs")) sql = "DELETE FROM jobs WHERE job_id=?";
            else if (tab.equals("My Jobs")) sql = "DELETE j FROM jobs j JOIN employers e USING(employer_id) WHERE j.job_id=? AND e.user_id=" + a.id;
            else return;
            try (Connection c = connect(); PreparedStatement q = c.prepareStatement(sql)) {
                q.setInt(1, id);
                q.executeUpdate();
            }
            log(a.id, "DELETE", tab + " #" + id);
            load.run();
        } catch (Exception ex) {
            error(ex);
        }
    }

    static void sendMessage(Account a) {
        JTextField to = new JTextField(), subject = new JTextField();
        JTextArea body = new JTextArea(6, 30);
        Object[] fields = {
            "Recipient user ID", to, "Subject", subject, "Message", new JScrollPane(body)
        };
        if (JOptionPane.showConfirmDialog(null, fields, "Send message", JOptionPane.OK_CANCEL_OPTION) != 0) return;
        try (Connection c = connect(); PreparedStatement q = c.prepareStatement( "INSERT INTO messages(sender_id,receiver_id,subject,message) VALUES(?,?,?,?)")) {
            q.setInt(1, a.id);
            q.setInt(2, Integer.parseInt(to.getText()));
            q.setString(3, subject.getText());
            q.setString(4, body.getText());
            q.executeUpdate();
            log(a.id, "MESSAGE_SENT", "To user #" + to.getText());
            JOptionPane.showMessageDialog(null, "Message sent successfully.");
        } catch (Exception ex) {
            error(ex);
        }
    }

    static void settingsTab(JTabbedPane tabs, Account a) {
        JPanel p = new JPanel(new BorderLayout());
        DefaultTableModel m = new DefaultTableModel();
        JTable t = new JTable(m);
        JButton refresh = new JButton("Refresh"), update = new JButton("Update selected setting");
        Runnable load = () -> {
            try (Connection c = connect(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT setting_name,setting_value,updated_at FROM system_settings")) {
                m.setRowCount(0);
                m.setColumnCount(0);
                for (String n : new String[] {
                    "Setting", "Value", "Updated"
                }) m.addColumn(n);
                while (r.next()) m.addRow(new Object[] {
                    r.getString(1), r.getString(2), r.getTimestamp(3)
                });
            } catch (Exception ex) {
                error(ex);
            }
        };
        refresh.addActionListener(e -> load.run());
        update.addActionListener(e -> {
            int row = t.getSelectedRow(); if (row < 0) return; String key = Objects.toString(m.getValueAt(row, 0)), val = JOptionPane.showInputDialog("New value", m.getValueAt(row, 1)); if (val == null) return; try (Connection c = connect(); PreparedStatement q = c.prepareStatement("UPDATE system_settings SET setting_value=? WHERE setting_name=?")) {
                q.setString(1, val); q.setString(2, key); q.executeUpdate(); JOptionPane.showMessageDialog(null, "Settings updated successfully."); load.run();
            } catch (Exception ex) {
                error(ex);
            }
        });
        p.add(new JScrollPane(t));
        p.add(update, BorderLayout.SOUTH);
        p.add(refresh, BorderLayout.NORTH);
        load.run();
        tabs.addTab("Settings", p);
    }

    static void statsTab(JTabbedPane tabs, String sql, Account a) {
        JPanel p = new JPanel(new BorderLayout());
        DefaultTableModel m = new DefaultTableModel();
        JTable t = new JTable(m);
        try (Connection c = connect(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            ResultSetMetaData md = r.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) m.addColumn(md.getColumnLabel(i));
            while (r.next()) {
                Object[] row = new Object[md.getColumnCount()];
                for (int i = 0; i < row.length; i++) row[i] = r.getObject(i + 1);
                m.addRow(row);
            }
        } catch (Exception ex) {
            error(ex);
        }
        p.add(new JScrollPane(t));
        tabs.addTab("Statistics", p);
    }

    static void error(Exception e) {
        if (e instanceof SQLException sql) {
            System.err.println("Database diagnostic: SQLState=" + sql.getSQLState() + ", code=" + sql.getErrorCode() + ", message=" + sql.getMessage());
            String state = sql.getSQLState();
            String hint = state != null && state.startsWith("28") ? "MySQL rejected the username or password." : state != null && state.startsWith("42") ? "Check that job_portal and its tables were created." : "Check that MySQL is running and the connection settings are correct.";
            JOptionPane.showMessageDialog(null, "Database operation failed. " + hint + "\nOpen VS Code's Debug Console for the diagnostic.", "Online Job Portal", JOptionPane.ERROR_MESSAGE);
        } else JOptionPane.showMessageDialog(null, Objects.toString(e.getMessage(), "Operation failed."), "Online Job Portal", JOptionPane.ERROR_MESSAGE);
    }

}
