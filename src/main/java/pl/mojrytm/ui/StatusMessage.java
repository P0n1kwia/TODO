package pl.mojrytm.ui;

/** Krótki komunikat w pasku stanu (informacja albo błąd). */
record StatusMessage(String text, boolean error) {
}
