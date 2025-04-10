package com.project.emai.automation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.ss.usermodel.*;
import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/send-emails")
public class EmailController {

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;
    @Autowired
    private JavaMailSender mailSender;

    @PostMapping
    public ResponseEntity<?> sendEmails(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("senderEmail") String senderEmail,
            @RequestParam("senderPassword") String senderPassword,
            @RequestParam("file") MultipartFile file,
            @RequestParam("message") String message,
            @RequestParam(value = "attachment", required = false) MultipartFile attachment) {
        senderEmail="projectmail613@gmail.com";
        // First check authentication
        if (!isAuthenticated(authHeader)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        Map<String, Object> response = new HashMap<>();
        try {
            // Validate inputs
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is required");
            }

            List<String> recipientEmails = new ArrayList<>();
            String fileName = file.getOriginalFilename();

            if (fileName.endsWith(".xlsx")) {
                // Parse Excel file
                System.out.println("Parsing the excel to extract emails");
                try (InputStream inputStream = file.getInputStream();
                     Workbook workbook = WorkbookFactory.create(inputStream)) {
                    Sheet sheet = workbook.getSheetAt(0);
                    for (Row row : sheet) {
                        Cell cell = row.getCell(0);
                        if (cell != null) {
                            recipientEmails.add(cell.getStringCellValue().trim());
                        }
                    }
                }
            } else if (fileName.endsWith(".docx")) {
                // Parse Word document
                System.out.println("Parsing the word document to extract emails");
                try (InputStream inputStream = file.getInputStream();
                     XWPFDocument document = new XWPFDocument(inputStream);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {

                    String text = extractor.getText();
                    // Extract emails from text using regex
                    Pattern pattern = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
                    Matcher matcher = pattern.matcher(text);

                    while (matcher.find()) {
                        recipientEmails.add(matcher.group().trim());
                    }
                }
            } else {
                return ResponseEntity.badRequest().body("Unsupported file format");
            }

            // Validate we found emails
            if (recipientEmails.isEmpty()) {
                return ResponseEntity.badRequest().body("No valid email addresses found in file");
            }
            System.out.println(recipientEmails);
            // Send emails
            List<String> failedRecipients = new ArrayList<>();
            for (String recipient : recipientEmails) {
                try {
                    if (!isValidEmail(recipient)) {
                        failedRecipients.add(recipient + " (invalid format)");
                        continue;
                    }
                    System.out.println("Sending email to: "+recipient);
                    MimeMessage mimeMessage = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                    helper.setFrom(senderEmail);
                    helper.setTo(recipient);
                    helper.setSubject("Automated Email");
                    helper.setText(message);

                    if (attachment != null && !attachment.isEmpty()) {
                        helper.addAttachment(attachment.getOriginalFilename(), attachment);
                    }
                    mailSender.send(mimeMessage);
                } catch (Exception e) {
                    failedRecipients.add(recipient + " (" + e.getMessage() + ")");
                }
            }

            response.put("status", "success");
            response.put("message", "Emails sent successfully");
            response.put("count", recipientEmails.size());
            System.out.println("Emails sent successfully");
            // Explicitly return 200 OK with headers
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response);
        }
    }

    private boolean isAuthenticated(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return false;
        }

        String base64Credentials = authHeader.substring("Basic ".length());
        String credentials = new String(Base64.getDecoder().decode(base64Credentials));
        String[] values = credentials.split(":", 2);

        return adminUsername.equals(values[0]) && adminPassword.equals(values[1]);
    }
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}