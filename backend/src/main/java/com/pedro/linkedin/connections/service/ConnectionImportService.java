package com.pedro.linkedin.connections.service;

import com.pedro.linkedin.connections.api.dto.ImportResultResponse;
import com.pedro.linkedin.connections.domain.Comment;
import com.pedro.linkedin.connections.domain.Connection;
import com.pedro.linkedin.connections.domain.Project;
import com.pedro.linkedin.connections.domain.Skill;
import com.pedro.linkedin.connections.repository.CommentRepository;
import com.pedro.linkedin.connections.repository.ConnectionRepository;
import com.pedro.linkedin.connections.repository.ProjectRepository;
import com.pedro.linkedin.connections.repository.SkillRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ConnectionImportService {

    private static final Set<String> IMPORTABLE_CSV_FILE_NAMES = Set.of(
            "connections.csv",
            "comments.csv",
            "projects.csv",
            "skills.csv"
    );

    private static final Map<String, Set<String>> REQUIRED_HEADER_ALIASES = Map.of(
            "firstName", Set.of("firstname", "givenname", "nome", "primeironome"),
            "lastName", Set.of("lastname", "surname", "familyname", "sobrenome", "ultimonome"),
            "emailAddress", Set.of("emailaddress", "email", "enderecodeemail", "enderecoemail"),
            "company", Set.of("company", "currentcompany", "empresa", "companhia"),
            "position", Set.of("position", "jobtitle", "title", "cargo", "funcao"),
            "connectedOn", Set.of("connectedon", "connectiondate", "dateconnected", "conectadoem", "datadeconexao")
    );

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yy"),
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d-MMM-yy", Locale.ENGLISH)
    );

    private final ConnectionRepository connectionRepository;
    private final CommentRepository commentRepository;
    private final ProjectRepository projectRepository;
    private final SkillRepository skillRepository;

    public ConnectionImportService(
            ConnectionRepository connectionRepository,
            CommentRepository commentRepository,
            ProjectRepository projectRepository,
            SkillRepository skillRepository
    ) {
        this.connectionRepository = connectionRepository;
        this.commentRepository = commentRepository;
        this.projectRepository = projectRepository;
        this.skillRepository = skillRepository;
    }

    @Transactional
    public ImportResultResponse importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidCsvException("CSV file is required and cannot be empty.");
        }

        return importFiles(List.of(file));
    }

    @Transactional
    public ImportResultResponse importFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new InvalidCsvException("At least one file is required.");
        }

        ImportBatch batch = new ImportBatch();
        List<String> warnings = new ArrayList<>();
        int skipped = 0;

        try {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                if (isZip(file)) {
                skipped += importZip(file, batch, warnings);
                    continue;
                }
                String content = sanitizeContent(file.getBytes());
                skipped += importCsvContent(content, file.getOriginalFilename(), batch, warnings);
            }
        } catch (IOException exception) {
            throw new InvalidCsvException("Unable to read CSV file: " + exception.getMessage());
        }

        clearImportedData();
        connectionRepository.saveAll(batch.connections());
        commentRepository.saveAll(batch.comments());
        projectRepository.saveAll(batch.projects());
        skillRepository.saveAll(batch.skills());
        return new ImportResultResponse(batch.importedCount(), skipped, warnings);
    }

    private int importZip(MultipartFile zipFile, ImportBatch batch, List<String> warnings) throws IOException {
        int skipped = 0;
        int csvEntries = 0;
        String zipName = zipFile.getOriginalFilename() == null ? "uploaded.zip" : zipFile.getOriginalFilename();

        try (InputStream inputStream = zipFile.getInputStream();
             ZipInputStream zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory() || !isImportableCsvEntry(entry.getName())) {
                    zipInputStream.closeEntry();
                    continue;
                }
                csvEntries++;
                try {
                    String content = sanitizeContent(zipInputStream.readAllBytes());
                    skipped += importCsvContent(content, entry.getName(), batch, warnings);
                } catch (InvalidCsvException exception) {
                    addWarning(warnings, "File " + entry.getName() + ": " + exception.getMessage());
                } finally {
                    zipInputStream.closeEntry();
                }
            }
        }

        if (csvEntries == 0) {
            throw new InvalidCsvException("ZIP file '" + zipName + "' does not contain any CSV files.");
        }
        return skipped;
    }

    private int importCsvContent(String content, String sourceName, ImportBatch batch, List<String> warnings) throws IOException {
        int skipped = 0;
        String source = sourceName == null || sourceName.isBlank() ? "uploaded.csv" : sourceName;
        String baseName = normalizeFileName(source);

        switch (baseName) {
            case "comments.csv" -> skipped += importComments(content, source, batch, warnings);
            case "projects.csv" -> skipped += importProjects(content, source, batch, warnings);
            case "skills.csv" -> skipped += importSkills(content, source, batch, warnings);
            default -> skipped += importConnections(content, source, batch, warnings);
        }
        return skipped;
    }

    private int importConnections(String content, String source, ImportBatch batch, List<String> warnings) throws IOException {
        ParsedCsv parsed = parseWithFlexibleDelimiter(content);
        int skipped = 0;

        for (CSVRecord record : parsed.records()) {
            try {
                batch.connections().add(toConnection(mapRecord(record, parsed.headerMapping())));
            } catch (InvalidCsvException exception) {
                skipped++;
                addWarning(warnings, "File " + source + ", line " + record.getRecordNumber() + ": " + exception.getMessage());
            }
        }
        return skipped;
    }

    private int importProjects(String content, String source, ImportBatch batch, List<String> warnings) throws IOException {
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();

        try (CSVParser parser = CSVParser.parse(content, csvFormat)) {
            int skipped = 0;
            for (CSVRecord record : parser.getRecords()) {
                try {
                    batch.projects().add(toProject(mapProjectRecord(record)));
                } catch (InvalidCsvException exception) {
                    skipped++;
                    addWarning(warnings, "File " + source + ", line " + record.getRecordNumber() + ": " + exception.getMessage());
                }
            }
            return skipped;
        }
    }

    private int importComments(String content, String source, ImportBatch batch, List<String> warnings) throws IOException {
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();

        try (CSVParser parser = CSVParser.parse(content, csvFormat)) {
            int skipped = 0;
            for (CSVRecord record : parser.getRecords()) {
                try {
                    batch.comments().add(toComment(mapCommentRecord(record)));
                } catch (InvalidCsvException exception) {
                    skipped++;
                    addWarning(warnings, "File " + source + ", line " + record.getRecordNumber() + ": " + exception.getMessage());
                }
            }
            return skipped;
        }
    }

    private int importSkills(String content, String source, ImportBatch batch, List<String> warnings) throws IOException {
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .build();

        try (CSVParser parser = CSVParser.parse(content, csvFormat)) {
            int skipped = 0;
            for (CSVRecord record : parser.getRecords()) {
                try {
                    batch.skills().add(toSkill(mapSkillRecord(record)));
                } catch (InvalidCsvException exception) {
                    skipped++;
                    addWarning(warnings, "File " + source + ", line " + record.getRecordNumber() + ": " + exception.getMessage());
                }
            }
            return skipped;
        }
    }

    private void addWarning(List<String> warnings, String message) {
        if (warnings.size() < 20) {
            warnings.add(message);
        }
    }

    private void clearImportedData() {
        commentRepository.deleteAllInBatch();
        projectRepository.deleteAllInBatch();
        skillRepository.deleteAllInBatch();
        connectionRepository.deleteAllInBatch();
    }

    private boolean isZip(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            return true;
        }
        String contentType = file.getContentType();
        return "application/zip".equalsIgnoreCase(contentType) || "application/x-zip-compressed".equalsIgnoreCase(contentType);
    }

    private boolean isCsvName(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    private boolean isImportableCsvEntry(String entryName) {
        if (!isCsvName(entryName)) {
            return false;
        }
        return IMPORTABLE_CSV_FILE_NAMES.contains(normalizeFileName(entryName));
    }

    private String normalizeFileName(String entryName) {
        String normalizedName = entryName.replace('\\', '/');
        int lastSlashIndex = normalizedName.lastIndexOf('/');
        String baseName = lastSlashIndex >= 0 ? normalizedName.substring(lastSlashIndex + 1) : normalizedName;
        return baseName.toLowerCase(Locale.ROOT);
    }

    private ParsedCsv parseWithFlexibleDelimiter(String content) throws IOException {
        String contentWithoutPreamble = stripPreamble(content);
        List<Character> delimiters = List.of(',', ';', '\t');
        InvalidCsvException lastHeaderError = null;

        for (char delimiter : delimiters) {
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setDelimiter(delimiter)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build();

            try (CSVParser parser = CSVParser.parse(contentWithoutPreamble, csvFormat)) {
                Map<String, Integer> headerMap = parser.getHeaderMap();
                if (headerMap == null || headerMap.isEmpty()) {
                    continue;
                }
                try {
                    HeaderMapping mapping = resolveHeaders(headerMap);
                    return new ParsedCsv(mapping, parser.getRecords());
                } catch (InvalidCsvException exception) {
                    lastHeaderError = exception;
                }
            }
        }

        if (lastHeaderError != null) {
            throw lastHeaderError;
        }
        throw new InvalidCsvException("Unable to parse CSV header.");
    }

    private String stripPreamble(String content) {
        String[] lines = content.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            String normalizedLine = normalizeHeader(lines[index]);
            boolean hasFirstName = containsHeaderAliasInLine(normalizedLine, "firstName");
            boolean hasLastName = containsHeaderAliasInLine(normalizedLine, "lastName");
            boolean hasConnectedOn = containsHeaderAliasInLine(normalizedLine, "connectedOn");

            if (hasFirstName && hasLastName && hasConnectedOn) {
                return String.join("\n", java.util.Arrays.copyOfRange(lines, index, lines.length));
            }
        }
        return content;
    }

    private boolean containsHeaderAliasInLine(String normalizedLine, String canonicalName) {
        return REQUIRED_HEADER_ALIASES.get(canonicalName).stream().anyMatch(normalizedLine::contains);
    }

    private HeaderMapping resolveHeaders(Map<String, Integer> headerMap) {
        Map<String, String> normalizedToOriginal = new LinkedHashMap<>();
        for (String originalHeader : headerMap.keySet()) {
            normalizedToOriginal.put(normalizeHeader(originalHeader), originalHeader);
        }

        List<String> missing = REQUIRED_HEADER_ALIASES.entrySet().stream()
                .filter(entry -> entry.getValue().stream().noneMatch(normalizedToOriginal::containsKey))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();

        if (!missing.isEmpty()) {
            throw new InvalidCsvException("CSV is missing required columns: " + String.join(", ", missing));
        }

        return new HeaderMapping(
                findOriginal(normalizedToOriginal, "firstName"),
                findOriginal(normalizedToOriginal, "lastName"),
                findOriginal(normalizedToOriginal, "emailAddress"),
                findOriginal(normalizedToOriginal, "company"),
                findOriginal(normalizedToOriginal, "position"),
                findOriginal(normalizedToOriginal, "connectedOn")
        );
    }

    private ConnectionImportRow mapRecord(CSVRecord record, HeaderMapping headerMapping) {
        String firstName = requireValue(record, headerMapping.firstNameHeader(), "firstName");
        String lastName = requireValue(record, headerMapping.lastNameHeader(), "lastName");
        String emailAddress = nullableValue(record, headerMapping.emailAddressHeader(), "emailAddress");
        String company = nullableValue(record, headerMapping.companyHeader(), "company");
        String position = nullableValue(record, headerMapping.positionHeader(), "position");
        String connectedOnRaw = nullableValue(record, headerMapping.connectedOnHeader(), "connectedOn");

        return new ConnectionImportRow(
                firstName,
                lastName,
                emailAddress,
                company,
                position,
                parseDate(connectedOnRaw)
        );
    }

    private Connection toConnection(ConnectionImportRow row) {
        Connection connection = new Connection();
        connection.setFirstName(row.firstName());
        connection.setLastName(row.lastName());
        connection.setEmailAddress(row.emailAddress());
        connection.setCompany(row.company());
        connection.setPosition(row.position());
        connection.setConnectedOn(row.connectedOn());
        return connection;
    }

    private ProjectImportRow mapProjectRecord(CSVRecord record) {
        return new ProjectImportRow(
                requireHeaderValue(record, "Title"),
                optionalHeaderValue(record, "Description"),
                optionalHeaderValue(record, "Url"),
                optionalHeaderValue(record, "Started On"),
                optionalHeaderValue(record, "Finished On")
        );
    }

    private Project toProject(ProjectImportRow row) {
        Project project = new Project();
        project.setTitle(row.title());
        project.setDescription(row.description());
        project.setUrl(row.url());
        project.setStartedOn(row.startedOn());
        project.setFinishedOn(row.finishedOn());
        return project;
    }

    private CommentImportRow mapCommentRecord(CSVRecord record) {
        return new CommentImportRow(
                requireHeaderValue(record, "Date"),
                optionalHeaderValue(record, "Link"),
                optionalHeaderValue(record, "Message")
        );
    }

    private Comment toComment(CommentImportRow row) {
        Comment comment = new Comment();
        comment.setDate(row.date());
        comment.setLink(row.link());
        comment.setMessage(row.message());
        return comment;
    }

    private SkillImportRow mapSkillRecord(CSVRecord record) {
        return new SkillImportRow(requireHeaderValue(record, "Name"));
    }

    private Skill toSkill(SkillImportRow row) {
        Skill skill = new Skill();
        skill.setName(row.name());
        return skill;
    }

    private String requireValue(CSVRecord record, String header, String canonicalName) {
        String value = nullableValue(record, header, canonicalName);
        if (value == null) {
            throw new InvalidCsvException("Field '" + canonicalName + "' is required.");
        }
        return value;
    }

    private String nullableValue(CSVRecord record, String header, String canonicalName) {
        if (!record.isMapped(header)) {
            throw new InvalidCsvException("Field '" + canonicalName + "' is not present in CSV header.");
        }
        String value = record.get(header);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String requireHeaderValue(CSVRecord record, String header) {
        String value = optionalHeaderValue(record, header);
        if (value == null) {
            throw new InvalidCsvException("Field '" + header + "' is required.");
        }
        return value;
    }

    private String optionalHeaderValue(CSVRecord record, String header) {
        if (!record.isMapped(header)) {
            throw new InvalidCsvException("Field '" + header + "' is not present in CSV header.");
        }
        String value = record.get(header);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private LocalDate parseDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(rawDate, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next formatter.
            }
        }
        throw new InvalidCsvException("Invalid date value for 'Connected On': " + rawDate);
    }

    private String sanitizeContent(byte[] bytes) {
        Charset charset = detectCharset(bytes);
        int offset = bomLength(bytes);
        String content = new String(bytes, offset, bytes.length - offset, charset);
        if (content.startsWith("\uFEFF")) {
            return content.substring(1);
        }
        return content;
    }

    private Charset detectCharset(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            return StandardCharsets.UTF_8;
        }
        if (bytes.length >= 2
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xFE) {
            return StandardCharsets.UTF_16LE;
        }
        if (bytes.length >= 2
                && (bytes[0] & 0xFF) == 0xFE
                && (bytes[1] & 0xFF) == 0xFF) {
            return StandardCharsets.UTF_16BE;
        }

        int evenNulls = 0;
        int oddNulls = 0;
        for (int index = 0; index < bytes.length; index++) {
            if (bytes[index] == 0) {
                if (index % 2 == 0) {
                    evenNulls++;
                } else {
                    oddNulls++;
                }
            }
        }

        if (oddNulls > evenNulls * 2) {
            return StandardCharsets.UTF_16LE;
        }
        if (evenNulls > oddNulls * 2) {
            return StandardCharsets.UTF_16BE;
        }
        return StandardCharsets.UTF_8;
    }

    private int bomLength(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            return 3;
        }
        if (bytes.length >= 2
                && (((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE)
                || ((bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF))) {
            return 2;
        }
        return 0;
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        String withoutAccents = Normalizer
                .normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents
                .replace("\uFEFF", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private String findOriginal(Map<String, String> normalizedToOriginal, String canonicalName) {
        return REQUIRED_HEADER_ALIASES.get(canonicalName).stream()
                .filter(normalizedToOriginal::containsKey)
                .findFirst()
                .map(normalizedToOriginal::get)
                .orElseThrow(() -> new InvalidCsvException("CSV is missing required column: " + canonicalName));
    }

    private record HeaderMapping(
            String firstNameHeader,
            String lastNameHeader,
            String emailAddressHeader,
            String companyHeader,
            String positionHeader,
            String connectedOnHeader
    ) {
    }

    private record ParsedCsv(
            HeaderMapping headerMapping,
            List<CSVRecord> records
    ) {
    }

    private record ImportBatch(
            List<Connection> connections,
            List<Comment> comments,
            List<Project> projects,
            List<Skill> skills
    ) {
        private ImportBatch() {
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        private int importedCount() {
            return connections.size() + comments.size() + projects.size() + skills.size();
        }
    }
}
