CREATE DATABASE IF NOT EXISTS job_portal CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE job_portal;
CREATE TABLE IF NOT EXISTS users (
 user_id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(120) NOT NULL, email VARCHAR(190) NOT NULL UNIQUE,
 password_hash CHAR(64) NOT NULL, role ENUM('ADMIN','EMPLOYER','JOB_SEEKER') NOT NULL,
 status ENUM('ACTIVE','SUSPENDED') NOT NULL DEFAULT 'ACTIVE', created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 INDEX idx_users_role(role), INDEX idx_users_created(created_at)
);
CREATE TABLE IF NOT EXISTS job_seekers (
 seeker_id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL UNIQUE, phone VARCHAR(30), address VARCHAR(255),
 skills TEXT, education TEXT, experience TEXT, resume VARCHAR(500),
 CONSTRAINT fk_seeker_user FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS employers (
 employer_id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL UNIQUE, company_name VARCHAR(160) NOT NULL,
 company_description TEXT, company_email VARCHAR(190),
 CONSTRAINT fk_employer_user FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS jobs (
 job_id INT AUTO_INCREMENT PRIMARY KEY, employer_id INT NOT NULL, title VARCHAR(180) NOT NULL, description TEXT NOT NULL,
 requirements TEXT, skills TEXT, salary DECIMAL(12,2) NOT NULL DEFAULT 0, location VARCHAR(160) NOT NULL,
 job_type VARCHAR(50) NOT NULL, experience VARCHAR(100), deadline DATE NOT NULL,
 status ENUM('PENDING','APPROVED','REJECTED','CLOSED') NOT NULL DEFAULT 'PENDING',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_job_employer FOREIGN KEY(employer_id) REFERENCES employers(employer_id) ON DELETE CASCADE,
 INDEX idx_job_search(status,location,job_type), INDEX idx_job_title(title), INDEX idx_job_created(created_at)
);
CREATE TABLE IF NOT EXISTS applications (
 application_id INT AUTO_INCREMENT PRIMARY KEY, job_id INT NOT NULL, seeker_id INT NOT NULL,
 resume VARCHAR(500), cover_letter TEXT, status ENUM('APPLIED','UNDER_REVIEW','SHORTLISTED','INTERVIEW','SELECTED','REJECTED') NOT NULL DEFAULT 'APPLIED',
 applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE KEY uq_job_seeker(job_id,seeker_id),
 CONSTRAINT fk_application_job FOREIGN KEY(job_id) REFERENCES jobs(job_id) ON DELETE CASCADE,
 CONSTRAINT fk_application_seeker FOREIGN KEY(seeker_id) REFERENCES job_seekers(seeker_id) ON DELETE CASCADE,
 INDEX idx_application_status(status), INDEX idx_application_date(applied_at)
);
CREATE TABLE IF NOT EXISTS messages (
 message_id INT AUTO_INCREMENT PRIMARY KEY, sender_id INT NOT NULL, receiver_id INT NOT NULL,
 subject VARCHAR(200) NOT NULL, message TEXT NOT NULL, sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_message_sender FOREIGN KEY(sender_id) REFERENCES users(user_id) ON DELETE CASCADE,
 CONSTRAINT fk_message_receiver FOREIGN KEY(receiver_id) REFERENCES users(user_id) ON DELETE CASCADE,
 INDEX idx_message_conversation(sender_id,receiver_id,sent_at)
);
CREATE TABLE IF NOT EXISTS system_settings (
 setting_id INT AUTO_INCREMENT PRIMARY KEY, setting_name VARCHAR(100) NOT NULL UNIQUE,
 setting_value VARCHAR(1000) NOT NULL, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS activity_logs (
 activity_id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id INT NULL, activity_type VARCHAR(80) NOT NULL,
 description VARCHAR(1000) NOT NULL, activity_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_activity_user FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE SET NULL,
 INDEX idx_activity_time(activity_time)
);

INSERT IGNORE INTO users(user_id,name,email,password_hash,role) VALUES
(1,'Portal Administrator','admin@jobportal.com','240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9','ADMIN'),
(2,'TechCorp Recruiter','employer@techcorp.com','5e326475889cb6f01b32381e7502e9694bcad416e3d39ddc57fffe68461d8419','EMPLOYER'),
(3,'Rahul Sharma','seeker@gmail.com','551fc2b3d9634d79ed6191c13446ad02b53d69e0771836e7f86e9ea9171ccba7','JOB_SEEKER');
INSERT IGNORE INTO employers(user_id,company_name,company_description,company_email) VALUES(2,'ABC Tech Pvt Ltd','Software product and consulting company','employer@techcorp.com');
INSERT IGNORE INTO job_seekers(user_id,phone,address,skills,education,experience) VALUES(3,'9000000000','Bengaluru','Java, SQL, MySQL, OOP','B.Tech Computer Science','Fresher');
INSERT IGNORE INTO jobs(job_id,employer_id,title,description,requirements,skills,salary,location,job_type,experience,deadline,status) VALUES
(1,1,'Java Developer','Build and maintain Java applications.','Java, OOP, SQL','Java, SQL, MySQL',650000,'Bengaluru','Full-time','0-2 years','2099-12-31','APPROVED'),
(2,1,'Backend Engineer','Develop REST services and database features.','Java and relational databases','Java, MySQL, REST',800000,'Remote','Full-time','1-3 years','2099-12-31','PENDING');
INSERT IGNORE INTO applications(job_id,seeker_id,resume,cover_letter,status) VALUES(1,1,'resume.pdf','I am interested in this Java role.','UNDER_REVIEW');
INSERT IGNORE INTO system_settings(setting_name,setting_value) VALUES
('portal_name','Online Job Portal'),('contact_email','support@jobportal.com'),('maximum_resume_size_mb','5'),
('job_approval_required','true'),('application_status_options','APPLIED,UNDER_REVIEW,SHORTLISTED,INTERVIEW,SELECTED,REJECTED'),('maintenance_mode','false');
