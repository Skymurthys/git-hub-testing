package com.tibco.sonar.plugins.bw6.check.project;

import com.tibco.sonar.plugins.bw6.check.AbstractProjectCheck;
import com.tibco.sonar.plugins.bw6.profile.BWProcessQualityProfile;
import com.tibco.sonar.plugins.bw6.source.ProjectSource;
import com.tibco.utils.common.logger.Logger;
import com.tibco.utils.common.logger.LoggerFactory;

import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

@Rule(
        key = UATSubstvarValidationCheck.RULE_KEY,
        name = "Validate UAT.substvar Variable Values with predefined_UAT.substvar",
        description = "Compares values of matching global variables between UAT.substvar and predefined_UAT.substvar with omselect validation",
        priority = Priority.CRITICAL
)
@BelongsToProfile(title = BWProcessQualityProfile.PROFILE_NAME, priority = Priority.CRITICAL)
public class UATSubstvarValidationCheck extends AbstractProjectCheck {

    public static final String RULE_KEY = "UATSubstvarValidation";
    private static final Logger LOG = LoggerFactory.getLogger(UATSubstvarValidationCheck.class);

    @RuleProperty(
            key = "predefinedSubstvarPath",
            description = "Path to predefined_UAT.substvar file",
            defaultValue = "C:/SonarValidation/GV/predefined_UAT.substvar",
            type = "TEXT"
    )
    protected String predefinedSubstvarPath;

    @Override
    public void validate(ProjectSource source) {
        File currentDir = source.getProject().getFile();
        File parentDir = currentDir.getParentFile();

        File[] siblings = parentDir.listFiles(File::isDirectory);
        if (siblings == null) return;

        for (File sibling : siblings) {
            String name = sibling.getName().toLowerCase();
            if (name.endsWith(".module") || name.endsWith(".parent")) continue;

            File metaInfDir = new File(sibling, "META-INF");
            File UATFile = findFile(metaInfDir, "UAT.substvar");

            if (UATFile != null) {
                validateAgainstPredefined(UATFile);
                return;
            }
        }

        reportIssueOnFile("Missing UAT.substvar in application folder");
    }

    private void validateAgainstPredefined(File UATFile) {
        File predefinedFile = new File(predefinedSubstvarPath);
        if (!predefinedFile.exists() || !predefinedFile.isFile() || !predefinedFile.canRead()) {
            reportIssueOnFile("Invalid predefined_UAT.substvar file: " + predefinedSubstvarPath);
            return;
        }

        Map<String, String> UATVars = parseGlobalVariables(UATFile);
        Map<String, String> predefinedVars = parseGlobalVariables(predefinedFile);

        // Full key-by-key comparison excluding USER, SCHEMA_NAME, PASSWORD
        for (Map.Entry<String, String> entry : predefinedVars.entrySet()) {
            String varName = entry.getKey();

            if (varName.equals("//common-om-connections///Connections/JDBC/Postgres_Appl/USER") ||
                varName.equals("//common-om-connections///Connections/JDBC/Postgres_Appl/SCHEMA_NAME") ||
                varName.equals("//common-om-connections///Connections/JDBC/Postgres_Appl/PASSWORD") ||
                varName.equals("//common-om-connections///Connections/JDBC/Postgres_Appl/PASSWORD_omselect")) {
                continue; // Skip these keys from general comparison
            }

            String expectedValue = entry.getValue();

            if (UATVars.containsKey(varName)) {
                String actualValue = UATVars.get(varName);
                if (!Objects.equals(expectedValue, actualValue)) {
                    reportIssueOnFile("Variable '" + varName + "' mismatch. Expected: '" + expectedValue + "', Found: '" + actualValue + "' in UAT.substvar");
                }
            }
        }

        // Validate USER, SCHEMA_NAME, PASSWORD specifically
        validateUserSchemaPassword(UATVars, predefinedVars);
    }

    private void validateUserSchemaPassword(Map<String, String> UATVars, Map<String, String> predefinedVars) {

        String userKey = "//common-om-connections///Connections/JDBC/Postgres_Appl/USER";
        String schemaKey = "//common-om-connections///Connections/JDBC/Postgres_Appl/SCHEMA_NAME";
        String passwordKey = "//common-om-connections///Connections/JDBC/Postgres_Appl/PASSWORD";

        String user = UATVars.get(userKey);
        String schemaName = UATVars.get(schemaKey);
        String password = UATVars.get(passwordKey);

        // Report USER-SCHEMA mismatch in all cases if values are present
		if (user != null && schemaName != null) {
			String expectedSchema;

			if ("omselect".equalsIgnoreCase(user)) {
				expectedSchema = "omselect";
			} else {
				expectedSchema = predefinedVars.get(schemaKey);
			}

			if (expectedSchema != null && !expectedSchema.equals(schemaName)) {
				reportIssueOnFile("SCHEMA NAME mismatch for USER '" + user + "'. Expected: '" + expectedSchema + "', Found: '" + schemaName + "' in UAT.substvar");
			}
		}

        // If any of the three are missing in UAT → skip password validation
        if (user == null || schemaName == null || password == null) {
            return;
        }

        String predefinedPasswordKey;
        if ("omselect".equalsIgnoreCase(user)) {
            predefinedPasswordKey = "//common-om-connections///Connections/JDBC/Postgres_Appl/PASSWORD_omselect";
        } else {
            predefinedPasswordKey = "//common-om-connections///Connections/JDBC/Postgres_Appl/PASSWORD";
        }

        String expectedPassword = predefinedVars.get(predefinedPasswordKey);

        // If predefined password key is missing → skip password validation
        if (expectedPassword == null) {
            return;
        }

        if (!expectedPassword.equals(password)) {
            reportIssueOnFile("Password mismatch for USER '" + user + "'. Expected: '" + expectedPassword + "', Found: '" + password + "' in UAT.substvar");
        }
    }

    private Map<String, String> parseGlobalVariables(File file) {
        Map<String, String> vars = new HashMap<>();
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setNamespaceAware(false);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new FileInputStream(file));
            doc.getDocumentElement().normalize();

            NodeList variableNodes = doc.getElementsByTagName("globalVariable");

            for (int i = 0; i < variableNodes.getLength(); i++) {
                Node node = variableNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element elem = (Element) node;
                    String name = getTagValue("name", elem);
                    String value = getTagValue("value", elem);
                    if (name != null) {
                        vars.put(name.trim(), value != null ? value.trim() : "");
                    }
                }
            }
        } catch (Exception e) {
            reportIssueOnFile("Error parsing: " + file.getName() + " - " + e.getMessage());
        }
        return vars;
    }

    private String getTagValue(String tag, Element element) {
        NodeList list = element.getElementsByTagName(tag);
        if (list != null && list.getLength() > 0) {
            return list.item(0).getTextContent().trim();
        }
        return null;
    }

    private File findFile(File dir, String fileName) {
        if (dir == null || !dir.exists()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File file : files) {
            if (file.getName().equals(fileName)) {
                return file;
            }
        }
        return null;
    }

    @Override
    public String getRuleKeyName() {
        return RULE_KEY;
    }

    @Override
    public Logger getLogger() {
        return LOG;
    }
}
