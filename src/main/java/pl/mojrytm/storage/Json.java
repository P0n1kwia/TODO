package pl.mojrytm.storage;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** Konfiguracja Jacksona dla pliku danych: czytelny, stabilny i porównywalny w diffie. */
final class Json {

    private Json() {
    }

    static ObjectMapper newMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // daty jako 2026-10-24, nie jako liczby
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setDefaultPrettyPrinter(prettyPrinter());
        // starsze i nowsze pliki mogą mieć dodatkowe pola
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // śmieci po końcu dokumentu oznaczają uszkodzony plik
        mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        return mapper;
    }

    /** Dwie spacje wcięcia, "klucz": wartość, puste tablice jako [], zawsze końce linii \n. */
    private static DefaultPrettyPrinter prettyPrinter() {
        DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter(Separators.createDefaultInstance()
                .withObjectFieldValueSpacing(Separators.Spacing.AFTER)
                .withObjectEmptySeparator("")
                .withArrayEmptySeparator(""));
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
        return printer;
    }
}
