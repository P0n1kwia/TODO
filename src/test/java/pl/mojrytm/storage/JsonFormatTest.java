package pl.mojrytm.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import pl.mojrytm.TestData;
import pl.mojrytm.model.AppData;
import pl.mojrytm.model.Area;

class JsonFormatTest {

    private final ObjectMapper mapper = Json.newMapper();

    @Test
    void writingSampleDataGivesExactlyTheGoldenFile() throws IOException {
        String written = mapper.writeValueAsString(TestData.sampleData()) + "\n";

        assertEquals(sampleFile(), written);
    }

    @Test
    void readingGoldenFileGivesSampleData() throws IOException {
        AppData read = mapper.readValue(sampleFile(), AppData.class);

        assertEquals(TestData.sampleData(), read);
    }

    @Test
    void emptyDataMatchesSkeletonFromSpec() throws IOException {
        String json = mapper.writeValueAsString(AppData.empty());

        assertTrue(json.contains("\"schemaVersion\": 2"), json);
        assertTrue(json.contains("\"dayStartHour\": 4"), json);
        assertTrue(json.contains("\"dataDir\": null"), json);
        assertTrue(json.contains("\"habits\": []"), json);
        assertTrue(json.contains("\"wheelRatings\": []"), json);
    }

    @Test
    void archivedAreaSurvivesWritingAndReading() throws IOException {
        AppData data = AppData.empty().withAreas(List.of(new Area("a", "Sen", true), new Area("b", "Ciało", false)));

        AppData read = mapper.readValue(mapper.writeValueAsString(data), AppData.class);

        assertEquals(data, read);
        assertTrue(read.areas().get(0).archived());
    }

    @Test
    void ignoresUnknownProperties() throws IOException {
        AppData read = mapper.readValue("""
                {"schemaVersion": 1, "futureField": {"a": 1},
                 "areas": [{"id": "a", "name": "Sen", "color": "red"}]}
                """, AppData.class);

        assertEquals(1, read.areas().size());
    }

    @Test
    void missingListsAndSettingsGetDefaults() throws IOException {
        AppData read = mapper.readValue("{\"schemaVersion\": 1}", AppData.class);

        assertEquals(4, read.settings().dayStartHour());
        assertTrue(read.habits().isEmpty());
    }

    private static String sampleFile() throws IOException {
        try (InputStream in = JsonFormatTest.class.getResourceAsStream("sample-v2.json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
