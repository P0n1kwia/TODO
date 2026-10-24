package pl.mojrytm.ui;

import javafx.scene.Node;

/** Jeden ekran aplikacji z tytułem do paska nawigacji. */
record Screen(String title, Node content) {
}
