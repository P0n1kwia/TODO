package pl.mojrytm.storage;

import com.fasterxml.jackson.databind.node.ObjectNode;

/** Jeden krok migracji: przenosi dane z wersji {@link #fromVersion()} na kolejną. */
public interface Migration {

    int fromVersion();

    /** Zmienia drzewo JSON w miejscu. Numer wersji podbija {@link Migrator}, nie migracja. */
    void migrate(ObjectNode root);
}
