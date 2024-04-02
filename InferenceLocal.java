import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class InferenceLocal extends JFrame {
    private JButton selectImageButton;
    private JTextArea resultTextArea;
    private JLabel imagePreviewLabel;

    public InferenceLocal() {
        setTitle("Image Inference");
        setSize(800, 400); // Increased width to accommodate image preview
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        selectImageButton = new JButton("Select Image");
        selectImageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select Image File");
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    processImage(selectedFile.toPath());
                }
            }
        });

        resultTextArea = new JTextArea();
        resultTextArea.setEditable(false);
        resultTextArea.setFont(new Font("Arial", Font.PLAIN, 25)); // Setting font size and style

        JScrollPane scrollPane = new JScrollPane(resultTextArea);

        imagePreviewLabel = new JLabel();
        imagePreviewLabel.setPreferredSize(new Dimension(300, 300)); // Set preferred size for the image preview

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(selectImageButton, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(imagePreviewLabel, BorderLayout.EAST); // Add image preview to the right

        add(panel);
    }

    private void processImage(Path imagePath) {
        try {
            byte[] imageArray = Files.readAllBytes(imagePath);
            String encoded = Base64.getEncoder().encodeToString(imageArray);
            byte[] data = encoded.getBytes(StandardCharsets.US_ASCII);
            String api_key = "KynvI0EJWnGX03nYyGhz"; // Your API Key
            String model_endpoint = "detected-violence-image-dataset/2"; // Set model endpoint

            // Construct the URL
            String uploadURL = "https://detect.roboflow.com/" + model_endpoint + "?api_key=" + api_key + "&name=original.jpg";

            // Configure Request
            URL url = new URL(uploadURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", String.valueOf(data.length));
            connection.setDoOutput(true);

            // Write Data
            try (OutputStream os = connection.getOutputStream()) {
                os.write(data);
            }

            // Get Response
            StringBuilder responseContent;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                responseContent = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    responseContent.append(line);
                }
            }

            // Format JSON output with spacing
            String formattedOutput = formatJson(responseContent.toString());

            resultTextArea.setText(formattedOutput);

            // Set image preview
            BufferedImage image = ImageIO.read(imagePath.toFile());
            Image scaledImage = image.getScaledInstance(300, 300, Image.SCALE_SMOOTH);
            imagePreviewLabel.setIcon(new ImageIcon(scaledImage));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private String formatJson(String json) {
        // Replace "time" with "time taken to connect to api"
        json = json.replace("\"time\"", "Time Taken to Connect to API ");
    
        // Check if "predictions" array is empty
        if (json.contains("\"predictions\": []")) {
            return "Not violent image";
        }
    
        // Replace "predictions" with "Findings"
        json = json.replace("\"predictions\"", "\"Findings\"");
    
        // Remove "detection_id" from each finding
        json = json.replaceAll("\"detection_id\":\"[^\"]*\",?", "");
    
        // Rename "class" to "Weapon"
        json = json.replace("\"class\"", "\"Weapon\"");
        json = json.replace("\"class_id\"", "Class");
    
        // Rename "confidence" to "Violent percentage" and format the value to percentage
        json = json.replaceAll("\"confidence\":(\\d+\\.?\\d*),", "\"Violent percentage\":$1%,");
    
        // Remove "image" lines
        json = json.replaceAll("\"image\":", "\"IMAGE\":\n");
    
        // Add one space after "height"
        json = json.replaceAll("\"height\":(\\d+)", "\"height\":$1, ");
    
        // Add an empty line after "Findings" as a sub-heading
        json = json.replaceAll("\"Findings\":", "\"FINDINGS\":");
    
        // Remove brackets from the JSON output
        json = json.replaceAll("[\\{\\}\\[\\]]", "");
    
        // Add line breaks after commas
        json = json.replaceAll(",", ",\n");
    
        // Remove "x", "y", "width", and "height" from "Findings"
        json = json.replaceAll("\"x\":\\d+\\.?\\d*,\n\"y\":\\d+\\.?\\d*,\n\"width\":\\d+\\.?\\d*,\n\"height\":\\d+\\.?\\d*,\n", "");
    
        // Remove '.0' from the "Findings" section
        json = json.replaceAll("\\.0(?=,\n\"Violent percentage\":)", "");
    
        // Check if "Findings" is empty
        if (json.contains("FINDINGS:") && json.contains(":")) {
            int index = json.indexOf("FINDINGS:");
            int endIndex = json.indexOf(",", index);
            if (endIndex == -1) {
                endIndex = json.indexOf("\n", index);
            }
            String findings = json.substring(index, endIndex);
            if (findings.trim().equals("FINDINGS:")) {
                json += "\nNot violent image";
            }
        }
    
        // Add indentation
        StringBuilder formattedJson = new StringBuilder();
        String[] lines = json.split("\n");
        int indentLevel = 0;
        for (String line : lines) {
            if (line.endsWith("{")) {
                formattedJson.append(getIndentString(indentLevel)).append(line).append("\n");
                indentLevel++;
            } else if (line.endsWith("}")) {
                indentLevel--;
                formattedJson.append(getIndentString(indentLevel)).append(line).append("\n");
            } else {
                formattedJson.append(getIndentString(indentLevel)).append(line).append("\n");
            }
        }
        return formattedJson.toString();
    }
    

    // Method to generate spaces for indentation
    private String getIndentString(int indentLevel) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            indent.append("    "); // 4 spaces per indent level
        }
        return indent.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new InferenceLocal().setVisible(true);
            }
        });
    }
}
