package com.pedro.linkedin.connections.api;

import com.pedro.linkedin.connections.repository.ConnectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:target/test-linkedin.db",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class ConnectionControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void cleanDatabase() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        connectionRepository.deleteAll();
    }

    @Test
    void shouldImportConnectionsAndListThem() throws Exception {
        String csv = """
                First Name,Last Name,Email Address,Company,Position,Connected On
                Ana,Silva,ana@example.com,Acme,Software Engineer,2024-01-20
                Bruno,Souza,bruno@example.com,Globex,Recruiter,2024-02-15
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "connections.csv",
                MediaType.TEXT_PLAIN_VALUE,
                csv.getBytes()
        );

        mockMvc.perform(multipart("/api/connections/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.warnings").isArray());

        mockMvc.perform(get("/api/connections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldReturnBadRequestWhenCsvHeaderIsInvalid() throws Exception {
        String csv = """
                First Name,Last Name,Email Address,Company,Connected On
                Ana,Silva,ana@example.com,Acme,2024-01-20
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalid.csv",
                MediaType.TEXT_PLAIN_VALUE,
                csv.getBytes()
        );

        mockMvc.perform(multipart("/api/connections/import").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid CSV"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldReturnStatsAfterImport() throws Exception {
        String csv = """
                First Name,Last Name,Email Address,Company,Position,Connected On
                Ana,Silva,ana@example.com,Acme,Software Engineer,2024-01-20
                Bruno,Souza,bruno@example.com,Acme,Recruiter,2024-02-15
                Carla,Lima,carla@example.com,Globex,Recruiter,2024-03-10
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "connections.csv",
                MediaType.TEXT_PLAIN_VALUE,
                csv.getBytes()
        );

        mockMvc.perform(multipart("/api/connections/import").file(file))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/connections/stats").param("top", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalConnections").value(3))
                .andExpect(jsonPath("$.topCompanies.length()").value(2))
                .andExpect(jsonPath("$.topCompanies[0].value").value("Acme"))
                .andExpect(jsonPath("$.topCompanies[0].total").value(2))
                .andExpect(jsonPath("$.topPositions[0].value").value("Recruiter"))
                .andExpect(jsonPath("$.topPositions[0].total").value(2));
    }

    @Test
    void shouldImportAllCsvFilesFromZip() throws Exception {
        byte[] zipBytes = buildZip(
                "nested/Connections.csv", """
                        First Name,Last Name,Email Address,Company,Position,Connected On
                        Ana,Silva,ana@example.com,Acme,Software Engineer,2024-01-20
                        """,
                "nested/ignored.csv", """
                        First Name,Last Name,Email Address,Company,Position,Connected On
                        Bruno,Souza,bruno@example.com,Globex,Recruiter,2024-02-15
                        """
        );

        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "connections.zip",
                "application/zip",
                zipBytes
        );

        mockMvc.perform(multipart("/api/connections/import").file(zipFile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(0));

        mockMvc.perform(get("/api/connections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldRejectZipWithoutConnectionsCsvByName() throws Exception {
        byte[] zipBytes = buildZip(
                "companies.csv", """
                        First Name,Last Name,Email Address,Company,Position,Connected On
                        Ana,Silva,ana@example.com,Acme,Software Engineer,2024-01-20
                        """,
                "profiles.csv", """
                        First Name,Last Name,Email Address,Company,Position,Connected On
                        Bruno,Souza,bruno@example.com,Globex,Recruiter,2024-02-15
                        """
        );

        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "connections.zip",
                "application/zip",
                zipBytes
        );

        mockMvc.perform(multipart("/api/connections/import").file(zipFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid CSV"))
                .andExpect(jsonPath("$.message").value("ZIP file 'connections.zip' does not contain any CSV files."));
    }

    @Test
    void shouldImportMultipleFilesInBatchEndpoint() throws Exception {
        String csv1 = """
                First Name,Last Name,Email Address,Company,Position,Connected On
                Ana,Silva,ana@example.com,Acme,Software Engineer,2024-01-20
                """;
        String csv2 = """
                First Name,Last Name,Email Address,Company,Position,Connected On
                Bruno,Souza,bruno@example.com,Globex,Recruiter,2024-02-15
                """;

        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "connections-1.csv",
                MediaType.TEXT_PLAIN_VALUE,
                csv1.getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "connections-2.csv",
                MediaType.TEXT_PLAIN_VALUE,
                csv2.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/connections/import/batch").file(file1).file(file2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2))
                .andExpect(jsonPath("$.skipped").value(0));

        mockMvc.perform(get("/api/connections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldImportLinkedInConnectionsCsvWithPreambleNotes() throws Exception {
        String csv = """
                Notes:
                "When exporting your connection data, some email addresses may be missing."
                                
                First Name,Last Name,URL,Email Address,Company,Position,Connected On
                Ana,Silva,https://www.linkedin.com/in/ana,,Acme,Software Engineer,11 Mar 2026
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Connections.csv",
                MediaType.TEXT_PLAIN_VALUE,
                csv.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/connections/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(1))
                .andExpect(jsonPath("$.skipped").value(0));
    }

    @Test
    void shouldImportRealConnectionsCsvFromLinkedinDataFolder() throws Exception {
        String csv = Files.readString(Path.of("linkedin-data", "Connections.csv"), StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "Connections.csv",
                MediaType.TEXT_PLAIN_VALUE,
                csv.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/connections/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(greaterThan(0)));
    }

    private byte[] buildZip(String firstName, String firstContent, String secondName, String secondContent) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry(firstName));
            zip.write(firstContent.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry(secondName));
            zip.write(secondContent.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return output.toByteArray();
    }
}
